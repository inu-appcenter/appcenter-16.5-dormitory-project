package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.common.image.entity.Image;
import com.example.appcenter_project.common.image.enums.ImageType;
import com.example.appcenter_project.common.image.repository.ImageRepository;
import com.example.appcenter_project.common.image.service.ImageService;
import com.example.appcenter_project.domain.openChat.dto.request.RequestEditOpenChatMessageDto;
import com.example.appcenter_project.domain.openChat.dto.request.RequestOpenChatMessageDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseAdminChatRoomDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatMessageDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatMessageEditEventDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatMessageListDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatReadEventDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseRecruitmentStatusEventDto;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomRecruitmentStatus;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomType;
import com.example.appcenter_project.domain.openChat.entity.OpenChatMessage;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatMessageType;
import com.example.appcenter_project.domain.openChat.repository.OpenChatMessageQuerydslRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatMessageRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatParticipantRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomRepository;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.config.OpenChatSessionRegistry;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.exception.ErrorCode;
import com.example.appcenter_project.shared.dto.ReplySourceDto;
import com.example.appcenter_project.shared.enums.ChatRoomType;
import com.example.appcenter_project.shared.enums.ReplySourceStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OpenChatMessageService {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final int MAX_IMAGE_COUNT = 5;
    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024;

    private final OpenChatRoomRepository openChatRoomRepository;
    private final OpenChatParticipantRepository openChatParticipantRepository;
    private final OpenChatMessageRepository openChatMessageRepository;
    private final OpenChatMessageQuerydslRepository openChatMessageQuerydslRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ImageService imageService;
    private final ImageRepository imageRepository;
    private final OpenChatSessionRegistry sessionRegistry;
    private final OpenChatNotificationService openChatNotificationService;
    private final ObjectMapper objectMapper;

    public void sendMessage(Long userId, RequestOpenChatMessageDto request) {
        OpenChatRoom room = openChatRoomRepository.findById(request.getRoomId()).orElse(null);
        if (room == null) return;

        if (openChatParticipantRepository.findByRoomIdAndUserId(request.getRoomId(), userId).isEmpty()) return;

        User sender = userRepository.findById(userId).orElse(null);
        if (sender == null) return;

        OpenChatMessage message = OpenChatMessage.create(request.getRoomId(), userId, request.getContent(), OpenChatMessageType.TEXT);

        Long replyToId = request.getReplyToMessageId();
        ReplySourceDto replySource = null;
        if (replyToId != null && replyToId > 0) {
            replySource = prepareReply(message, request.getRoomId(), replyToId);
        }

        openChatMessageRepository.save(message);

        room.updateLastMessage(message.getContent(), message.getCreatedDate());

        Set<Long> usersToRead = new HashSet<>(sessionRegistry.getSubscriberUserIds(request.getRoomId()));
        usersToRead.add(userId);
        openChatParticipantRepository.updateLastReadMessageIdByRoomIdAndUserIdIn(request.getRoomId(), usersToRead, message.getId());

        int unreadCount = calculateUnreadCount(request.getRoomId(), message.getId());

        ResponseOpenChatMessageDto response = ResponseOpenChatMessageDto.from(message, sender.getName(), unreadCount, List.of(), replySource);
        messagingTemplate.convertAndSend("/sub/openchat/" + request.getRoomId(), response);
        messagingTemplate.convertAndSend("/sub/openchat/" + request.getRoomId() + "/read",
                ResponseOpenChatReadEventDto.of(message.getId(), unreadCount));

        if (openChatNotificationService != null) {
            openChatNotificationService.sendImmediateNotifications(
                    request.getRoomId(), room.getRoomType(), usersToRead, room.getName(), request.getContent());
        }
    }

    private ReplySourceDto prepareReply(OpenChatMessage message, Long roomId, Long replyToMessageId) {
        OpenChatMessage original = openChatMessageRepository.findById(replyToMessageId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_MESSAGE_NOT_FOUND));

        if (original.isDeleted()) throw new CustomException(ErrorCode.OPEN_CHAT_MESSAGE_ALREADY_DELETED);
        if (!original.getRoomId().equals(roomId)) throw new CustomException(ErrorCode.OPEN_CHAT_REPLY_TARGET_NOT_IN_SAME_ROOM);
        if (original.getReplyToMessageId() != null) throw new CustomException(ErrorCode.OPEN_CHAT_NESTED_REPLY_NOT_ALLOWED);

        OpenChatMessageType originalType = original.getType();
        if (originalType == OpenChatMessageType.SYSTEM || originalType == OpenChatMessageType.BOT
                || originalType == OpenChatMessageType.ROOM_LINK
                || originalType == OpenChatMessageType.STUDENT_ID_REQUEST) {
            throw new CustomException(ErrorCode.OPEN_CHAT_REPLY_NOT_ALLOWED_FOR_TYPE);
        }

        ChatRoomType roomType = openChatRoomRepository.findById(roomId)
                .map(r -> r.getRoomType() == OpenChatRoomType.DERIVED ? ChatRoomType.DERIVED : ChatRoomType.OPEN)
                .orElse(ChatRoomType.OPEN);

        Long derivedRoomId = null;
        if (originalType == OpenChatMessageType.REOPEN_CARD) {
            derivedRoomId = parseDerivedRoomId(original.getContent());
            if (derivedRoomId == null) throw new CustomException(ErrorCode.OPEN_CHAT_DERIVED_ROOM_ID_PARSE_FAILED);
        }

        message.attachReply(replyToMessageId, originalType, original.getSenderId(), roomId, roomType, derivedRoomId);

        String senderNickname = userRepository.findById(original.getSenderId())
                .map(User::getName).orElse(null);

        if (originalType == OpenChatMessageType.REOPEN_CARD) {
            boolean closed = openChatRoomRepository.findById(derivedRoomId)
                    .map(OpenChatRoom::isRecruitmentClosed).orElse(false);
            return ReplySourceDto.builder()
                    .replyToMessageId(replyToMessageId)
                    .status(closed ? ReplySourceStatus.RECRUITMENT_CLOSED : ReplySourceStatus.RECRUITING)
                    .replyToSenderId(original.getSenderId())
                    .replyToSenderNickname(senderNickname)
                    .replyToRoomType(roomType)
                    .replyToRoomId(roomId)
                    .replyToDerivedRoomId(derivedRoomId)
                    .build();
        }

        String preview = original.getContent();
        if (preview != null && preview.length() > 100) preview = preview.substring(0, 100);

        return ReplySourceDto.builder()
                .replyToMessageId(replyToMessageId)
                .status(ReplySourceStatus.NORMAL)
                .replyToSenderId(original.getSenderId())
                .replyToSenderNickname(senderNickname)
                .contentPreview(preview)
                .replyToRoomType(roomType)
                .replyToRoomId(roomId)
                .build();
    }

    public List<ResponseOpenChatMessageDto> sendImageMessage(Long userId, Long roomId, List<MultipartFile> images, HttpServletRequest httpServletRequest) {
        OpenChatRoom room = openChatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND));

        openChatParticipantRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_NOT_PARTICIPANT));

        validateImageFiles(images);

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<ResponseOpenChatMessageDto> results = new java.util.ArrayList<>();

        for (MultipartFile image : images) {
            OpenChatMessage message = OpenChatMessage.create(roomId, userId, "", OpenChatMessageType.IMAGE);
            openChatMessageRepository.save(message);

            imageService.saveImages(ImageType.OPEN_CHAT_MESSAGE, message.getId(), List.of(image));

            List<String> imageUrls = imageService.findStaticImageUrls(ImageType.OPEN_CHAT_MESSAGE, message.getId(), httpServletRequest);

            Set<Long> usersToRead = new HashSet<>(sessionRegistry.getSubscriberUserIds(roomId));
            usersToRead.add(userId);
            openChatParticipantRepository.updateLastReadMessageIdByRoomIdAndUserIdIn(roomId, usersToRead, message.getId());

            int unreadCount = calculateUnreadCount(roomId, message.getId());

            ResponseOpenChatMessageDto response = ResponseOpenChatMessageDto.from(message, sender.getName(), unreadCount, imageUrls);
            messagingTemplate.convertAndSend("/sub/openchat/" + roomId, response);
            messagingTemplate.convertAndSend("/sub/openchat/" + roomId + "/read",
                    ResponseOpenChatReadEventDto.of(message.getId(), unreadCount));

            if (openChatNotificationService != null) {
                openChatNotificationService.sendImmediateNotifications(roomId, room.getRoomType(), usersToRead, room.getName(), "[이미지]");
            }

            results.add(response);
        }

        if (!results.isEmpty()) {
            room.updateLastMessage("[이미지]", results.get(results.size() - 1).getCreatedAt());
        }

        return results;
    }

    public void sendSystemMessage(Long roomId, String content) {
        OpenChatRoom room = openChatRoomRepository.findById(roomId).orElse(null);
        if (room == null) return;

        OpenChatMessage message = OpenChatMessage.create(roomId, 0L, content, OpenChatMessageType.SYSTEM);
        openChatMessageRepository.save(message);

        room.updateLastMessage(message.getContent(), message.getCreatedDate());

        Set<Long> subscribers = sessionRegistry.getSubscriberUserIds(roomId);
        if (!subscribers.isEmpty()) {
            openChatParticipantRepository.updateLastReadMessageIdByRoomIdAndUserIdIn(roomId, subscribers, message.getId());
        }

        int unreadCount = calculateUnreadCount(roomId, message.getId());

        ResponseOpenChatMessageDto response = ResponseOpenChatMessageDto.from(message, null, unreadCount);
        messagingTemplate.convertAndSend("/sub/openchat/" + roomId, response);
        messagingTemplate.convertAndSend("/sub/openchat/" + roomId + "/read",
                ResponseOpenChatReadEventDto.of(message.getId(), unreadCount));
    }

    public void sendRoomLinkMessage(long originRoomId, long senderId, long derivedRoomId, String name, String description, int maxParticipants) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        OpenChatRoom originRoom = openChatRoomRepository.findById(originRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND));

        Map<String, Object> payload = new HashMap<>();
        payload.put("derivedRoomId", derivedRoomId);
        payload.put("roomName", name);
        payload.put("description", description);
        payload.put("maxParticipants", maxParticipants);

        String content;
        try {
            content = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.UNHANDLED_EXCEPTION);
        }

        OpenChatMessage message = OpenChatMessage.create(originRoomId, senderId, content, OpenChatMessageType.ROOM_LINK);
        openChatMessageRepository.save(message);

        originRoom.updateLastMessage(content, message.getCreatedDate());

        Set<Long> usersToRead = new HashSet<>(sessionRegistry.getSubscriberUserIds(originRoomId));
        usersToRead.add(senderId);
        openChatParticipantRepository.updateLastReadMessageIdByRoomIdAndUserIdIn(originRoomId, usersToRead, message.getId());

        int unreadCount = calculateUnreadCount(originRoomId, message.getId());

        ResponseOpenChatMessageDto response = ResponseOpenChatMessageDto.fromRoomLink(
                message, sender.getName(), unreadCount,
                derivedRoomId, name, description, maxParticipants);
        messagingTemplate.convertAndSend("/sub/openchat/" + originRoomId, response);
        messagingTemplate.convertAndSend("/sub/openchat/" + originRoomId + "/read",
                ResponseOpenChatReadEventDto.of(message.getId(), unreadCount));
    }

    public void sendStudentIdRequestMessage(long roomId, long requesterId, long requestId) {
        User sender = userRepository.findById(requesterId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Map<String, Object> payload = new HashMap<>();
        payload.put("requestId", requestId);
        payload.put("requesterId", requesterId);
        payload.put("requesterNickname", sender.getName());

        String content;
        try {
            content = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.UNHANDLED_EXCEPTION);
        }

        OpenChatMessage message = OpenChatMessage.create(roomId, requesterId, content, OpenChatMessageType.STUDENT_ID_REQUEST);
        openChatMessageRepository.save(message);

        openChatRoomRepository.findById(roomId).ifPresent(room ->
                room.updateLastMessage(content, message.getCreatedDate()));

        Set<Long> usersToRead = new HashSet<>(sessionRegistry.getSubscriberUserIds(roomId));
        usersToRead.add(requesterId);
        openChatParticipantRepository.updateLastReadMessageIdByRoomIdAndUserIdIn(roomId, usersToRead, message.getId());

        int unreadCount = calculateUnreadCount(roomId, message.getId());

        ResponseOpenChatMessageDto response = ResponseOpenChatMessageDto.fromStudentIdRequest(
                message, sender.getName(), unreadCount, requestId);
        messagingTemplate.convertAndSend("/sub/openchat/" + roomId, response);
        messagingTemplate.convertAndSend("/sub/openchat/" + roomId + "/read",
                ResponseOpenChatReadEventDto.of(message.getId(), unreadCount));
    }

    public ResponseOpenChatMessageListDto getMessages(Long userId, Long roomId, Long lastMessageId, int size, HttpServletRequest request) {
        openChatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND));

        openChatParticipantRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_NOT_PARTICIPANT));

        List<OpenChatMessage> messages = openChatMessageQuerydslRepository.findByRoomIdWithCursor(roomId, lastMessageId, size + 1);

        boolean hasNext = messages.size() > size;
        if (hasNext) {
            messages = new ArrayList<>(messages.subList(1, messages.size()));
        }

        Long latestId = messages.isEmpty() ? null : messages.get(messages.size() - 1).getId();
        if (latestId != null) {
            openChatParticipantRepository.updateLastReadMessageId(roomId, userId, latestId);
            int unreadCount = calculateUnreadCount(roomId, latestId);
            messagingTemplate.convertAndSend("/sub/openchat/" + roomId + "/read",
                    ResponseOpenChatReadEventDto.of(latestId, unreadCount));
        }

        List<Long> imageMessageIds = messages.stream()
                .filter(msg -> msg.getType() == OpenChatMessageType.IMAGE)
                .map(OpenChatMessage::getId)
                .toList();

        final Map<Long, List<String>> imageUrlsMap = imageMessageIds.isEmpty()
                ? Map.of()
                : imageRepository.findByImageTypeAndEntityIdIn(ImageType.OPEN_CHAT_MESSAGE, imageMessageIds).stream()
                        .collect(Collectors.groupingBy(
                                Image::getEntityId,
                                Collectors.mapping(img -> imageService.getImageUrl(ImageType.OPEN_CHAT_MESSAGE, img, request), Collectors.toList())
                        ));

        List<Long> senderIds = messages.stream()
                .filter(msg -> msg.getType() != OpenChatMessageType.SYSTEM)
                .map(OpenChatMessage::getSenderId)
                .distinct()
                .toList();

        final Map<Long, String> nicknameByUserId = userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getName() != null ? u.getName() : ""));

        List<Long> linkedRoomIds = messages.stream()
                .filter(msg -> msg.getType() == OpenChatMessageType.ROOM_LINK
                        || msg.getType() == OpenChatMessageType.REOPEN_CARD)
                .map(this::extractLinkedRoomId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        final Map<Long, OpenChatRoom> linkedRoomMap = linkedRoomIds.isEmpty()
                ? Map.of()
                : openChatRoomRepository.findAllById(linkedRoomIds).stream()
                        .collect(Collectors.toMap(OpenChatRoom::getId, r -> r));

        Map<Long, ReplySourceDto> replySources = buildReplySources(messages);

        List<ResponseOpenChatMessageDto> dtos = messages.stream()
                .map(msg -> {
                    String nickname = msg.getType() == OpenChatMessageType.SYSTEM
                            ? null
                            : nicknameByUserId.get(msg.getSenderId());
                    int unreadCount = calculateUnreadCount(roomId, msg.getId());
                    if (msg.getType() == OpenChatMessageType.ROOM_LINK
                            || msg.getType() == OpenChatMessageType.REOPEN_CARD) {
                        return toRoomLinkDto(msg, nickname, unreadCount, linkedRoomMap);
                    }
                    if (msg.getType() == OpenChatMessageType.STUDENT_ID_REQUEST) {
                        return toStudentIdRequestDto(msg, nickname, unreadCount);
                    }
                    List<String> imageUrls = msg.getType() == OpenChatMessageType.IMAGE
                            ? imageUrlsMap.getOrDefault(msg.getId(), List.of())
                            : List.of();
                    return ResponseOpenChatMessageDto.from(msg, nickname, unreadCount, imageUrls, replySources.get(msg.getId()));
                })
                .toList();

        Long nextCursor = hasNext && !messages.isEmpty() ? messages.get(0).getId() : null;

        return ResponseOpenChatMessageListDto.builder()
                .messages(dtos)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();
    }

    public void markChatRoomAsRead(Long roomId, Long userId) {
        openChatParticipantRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_NOT_PARTICIPANT));

        openChatMessageQuerydslRepository.findLatestMessageIdByRoomId(roomId).ifPresent(latestId ->
                openChatParticipantRepository.updateLastReadMessageId(roomId, userId, latestId));
    }

    @Transactional(readOnly = true)
    public List<ResponseAdminChatRoomDto> getAdminChatRooms() {
        return openChatRoomRepository.findByRoomTypeNot(OpenChatRoomType.PERSONAL).stream()
                .map(ResponseAdminChatRoomDto::from)
                .toList();
    }

    public void sendBotMessage(Long adminId, Long roomId, String content) {
        OpenChatRoom room = openChatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND));

        if (room.getRoomType() == OpenChatRoomType.PERSONAL) {
            throw new CustomException(ErrorCode.OPEN_CHAT_BOT_TARGET_PERSONAL);
        }

        User sender = userRepository.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        OpenChatMessage message = OpenChatMessage.create(roomId, adminId, content, OpenChatMessageType.BOT);
        openChatMessageRepository.save(message);

        room.updateLastMessage(content, message.getCreatedDate());

        Set<Long> subscribers = sessionRegistry.getSubscriberUserIds(roomId);
        if (!subscribers.isEmpty()) {
            openChatParticipantRepository.updateLastReadMessageIdByRoomIdAndUserIdIn(roomId, subscribers, message.getId());
        }

        int unreadCount = calculateUnreadCount(roomId, message.getId());

        ResponseOpenChatMessageDto response = ResponseOpenChatMessageDto.from(message, sender.getName(), unreadCount);
        messagingTemplate.convertAndSend("/sub/openchat/" + roomId, response);
        messagingTemplate.convertAndSend("/sub/openchat/" + roomId + "/read",
                ResponseOpenChatReadEventDto.of(message.getId(), unreadCount));

        if (openChatNotificationService != null) {
            openChatNotificationService.sendImmediateNotifications(
                    roomId, room.getRoomType(), subscribers, room.getName(), content);
        }
    }

    public int calculateUnreadCount(Long roomId, Long messageId) {
        long total = openChatParticipantRepository.countByRoomId(roomId);
        long readCount = openChatParticipantRepository.countReadByRoomIdAndMessageId(roomId, messageId);
        return (int) (total - readCount);
    }

    private ResponseOpenChatMessageDto toRoomLinkDto(OpenChatMessage msg, String nickname, int unreadCount) {
        return toRoomLinkDto(msg, nickname, unreadCount, Map.of());
    }

    private ResponseOpenChatMessageDto toRoomLinkDto(OpenChatMessage msg, String nickname, int unreadCount,
                                                     Map<Long, OpenChatRoom> linkedRoomMap) {
        try {
            Map<?, ?> parsed = objectMapper.readValue(msg.getContent(), Map.class);
            Long derivedRoomId = ((Number) parsed.get("derivedRoomId")).longValue();
            String roomName = (String) parsed.get("roomName");
            String description = (String) parsed.get("description");
            Integer maxParticipants = parsed.get("maxParticipants") != null
                    ? ((Number) parsed.get("maxParticipants")).intValue() : null;
            OpenChatRoom linkedRoom = linkedRoomMap.get(derivedRoomId);
            boolean recruitmentClosed = linkedRoom != null && linkedRoom.isRecruitmentClosed();
            OpenChatRoomRecruitmentStatus recruitmentStatus = recruitmentClosed
                    ? OpenChatRoomRecruitmentStatus.CLOSED
                    : OpenChatRoomRecruitmentStatus.OPEN;
            return ResponseOpenChatMessageDto.fromRoomLink(msg, nickname, unreadCount,
                    derivedRoomId, roomName, description, maxParticipants, recruitmentClosed, recruitmentStatus);
        } catch (Exception e) {
            return ResponseOpenChatMessageDto.from(msg, nickname, unreadCount, List.of());
        }
    }

    private Long extractLinkedRoomId(OpenChatMessage msg) {
        try {
            Map<?, ?> parsed = objectMapper.readValue(msg.getContent(), Map.class);
            Object raw = parsed.get("derivedRoomId");
            return raw != null ? ((Number) raw).longValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private ResponseOpenChatMessageDto toStudentIdRequestDto(OpenChatMessage msg, String nickname, int unreadCount) {
        try {
            Map<?, ?> parsed = objectMapper.readValue(msg.getContent(), Map.class);
            Long requestId = ((Number) parsed.get("requestId")).longValue();
            return ResponseOpenChatMessageDto.fromStudentIdRequest(msg, nickname, unreadCount, requestId);
        } catch (Exception e) {
            return ResponseOpenChatMessageDto.from(msg, nickname, unreadCount, List.of());
        }
    }

    public void sendRecruitmentStatusEvent(Long parentRoomId, Long derivedRoomId, OpenChatRoomRecruitmentStatus status) {
        messagingTemplate.convertAndSend(
                "/sub/openchat/" + parentRoomId + "/recruitment-status",
                ResponseRecruitmentStatusEventDto.of(derivedRoomId, status));
    }

    public void sendReopenCardMessage(Long parentRoomId, Long actorId, Long derivedRoomId,
                                      String roomName, String description, int maxParticipants, int transitionCount) {
        String duplKey = derivedRoomId + "_reopen_" + transitionCount;
        if (openChatMessageRepository.existsByDuplKey(duplKey)) {
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("derivedRoomId", derivedRoomId);
        payload.put("roomName", roomName);
        payload.put("description", description);
        payload.put("maxParticipants", maxParticipants);

        String content;
        try {
            content = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.UNHANDLED_EXCEPTION);
        }

        OpenChatMessage message = OpenChatMessage.createReopenCard(parentRoomId, actorId, content, duplKey);
        openChatMessageRepository.save(message);

        openChatRoomRepository.findById(parentRoomId).ifPresent(room ->
                room.updateLastMessage(content, message.getCreatedDate()));

        int unreadCount = calculateUnreadCount(parentRoomId, message.getId());

        User sender = userRepository.findById(actorId).orElse(null);
        String senderNickname = sender != null ? sender.getName() : null;

        ResponseOpenChatMessageDto response = ResponseOpenChatMessageDto.fromRoomLink(
                message, senderNickname, unreadCount,
                derivedRoomId, roomName, description, maxParticipants, false,
                OpenChatRoomRecruitmentStatus.OPEN);
        messagingTemplate.convertAndSend("/sub/openchat/" + parentRoomId, response);
    }

    @Transactional
    public void sendMessageWithReply(Long roomId, Long senderId, String content, Long replyToMessageId) {
        OpenChatMessage originalMessage = openChatMessageRepository.findById(replyToMessageId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_MESSAGE_NOT_FOUND));

        if (originalMessage.isDeleted()) {
            throw new CustomException(ErrorCode.OPEN_CHAT_MESSAGE_ALREADY_DELETED);
        }

        if (!originalMessage.getRoomId().equals(roomId)) {
            throw new CustomException(ErrorCode.OPEN_CHAT_REPLY_TARGET_NOT_IN_SAME_ROOM);
        }

        if (originalMessage.getReplyToMessageId() != null) {
            throw new CustomException(ErrorCode.OPEN_CHAT_NESTED_REPLY_NOT_ALLOWED);
        }

        OpenChatMessageType originalType = originalMessage.getType();
        if (originalType == OpenChatMessageType.SYSTEM
                || originalType == OpenChatMessageType.BOT
                || originalType == OpenChatMessageType.ROOM_LINK
                || originalType == OpenChatMessageType.STUDENT_ID_REQUEST) {
            throw new CustomException(ErrorCode.OPEN_CHAT_REPLY_NOT_ALLOWED_FOR_TYPE);
        }

        Long derivedRoomId = null;
        if (originalType == OpenChatMessageType.REOPEN_CARD) {
            derivedRoomId = parseDerivedRoomId(originalMessage.getContent());
            if (derivedRoomId == null) {
                throw new CustomException(ErrorCode.OPEN_CHAT_DERIVED_ROOM_ID_PARSE_FAILED);
            }
        }

        ChatRoomType roomType = ChatRoomType.OPEN;
        if (openChatRoomRepository != null) {
            roomType = openChatRoomRepository.findById(roomId)
                    .map(room -> room.getRoomType() == OpenChatRoomType.DERIVED ? ChatRoomType.DERIVED : ChatRoomType.OPEN)
                    .orElse(ChatRoomType.OPEN);
        }

        OpenChatMessage reply = OpenChatMessage.create(roomId, senderId, content, OpenChatMessageType.TEXT);
        reply.attachReply(replyToMessageId, originalType, originalMessage.getSenderId(), roomId, roomType, derivedRoomId);
        openChatMessageRepository.save(reply);
    }

    @Transactional
    public void deleteMessage(Long roomId, Long messageId, Long requesterId) {
        OpenChatMessage message = openChatMessageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_MESSAGE_NOT_FOUND));

        if (!message.getSenderId().equals(requesterId)) {
            throw new CustomException(ErrorCode.OPEN_CHAT_MESSAGE_NOT_OWNED_BY_USER);
        }

        if (message.isDeleted()) {
            throw new CustomException(ErrorCode.OPEN_CHAT_MESSAGE_ALREADY_DELETED);
        }

        message.softDelete();
        openChatMessageRepository.save(message);
    }

    @Transactional
    public ResponseOpenChatMessageDto editMessage(Long requesterId, Long roomId, Long messageId, RequestEditOpenChatMessageDto dto) {
        OpenChatMessage message = openChatMessageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_MESSAGE_NOT_FOUND));

        if (!message.getSenderId().equals(requesterId)) {
            throw new CustomException(ErrorCode.OPEN_CHAT_MESSAGE_NOT_OWNED_BY_USER);
        }

        if (message.getType() != OpenChatMessageType.TEXT) {
            throw new CustomException(ErrorCode.OPEN_CHAT_MESSAGE_EDIT_FORBIDDEN_TYPE);
        }

        if (message.isDeleted()) {
            throw new CustomException(ErrorCode.OPEN_CHAT_MESSAGE_ALREADY_DELETED);
        }

        if (dto.getContent().equals(message.getContent())) {
            throw new CustomException(ErrorCode.OPEN_CHAT_MESSAGE_CONTENT_UNCHANGED);
        }

        message.updateContent(dto.getContent());
        openChatMessageRepository.save(message);

        Optional<Long> latestIdOpt = openChatMessageQuerydslRepository.findLatestMessageIdByRoomId(roomId);
        if (latestIdOpt.isPresent() && latestIdOpt.get().equals(messageId)) {
            openChatRoomRepository.findById(roomId).ifPresent(r ->
                    r.updateLastMessage(message.getContent(), message.getEditedAt()));
        }

        messagingTemplate.convertAndSend("/sub/openchat/" + roomId + "/edit",
                ResponseOpenChatMessageEditEventDto.from(message));

        return ResponseOpenChatMessageDto.from(message, null, 0, java.util.List.of(), null);
    }

    @Transactional(readOnly = true)
    public Map<Long, ReplySourceDto> buildReplySources(List<OpenChatMessage> messages) {
        Map<Long, ReplySourceDto> result = new HashMap<>();

        List<OpenChatMessage> replyMessages = messages.stream()
                .filter(msg -> msg.getReplyToMessageId() != null)
                .toList();

        if (replyMessages.isEmpty()) {
            return result;
        }

        List<Long> originalIds = replyMessages.stream()
                .map(OpenChatMessage::getReplyToMessageId)
                .distinct()
                .toList();

        Map<Long, OpenChatMessage> originalMap = openChatMessageRepository.findAllById(originalIds).stream()
                .collect(Collectors.toMap(OpenChatMessage::getId, m -> m));

        Map<Long, Long> derivedRoomIdByOriginalId = new HashMap<>();
        for (OpenChatMessage m : originalMap.values()) {
            if (m.getType() == OpenChatMessageType.REOPEN_CARD) {
                Long drid = m.getReplyToDerivedRoomId() != null
                        ? m.getReplyToDerivedRoomId()
                        : parseDerivedRoomId(m.getContent());
                if (drid != null) {
                    derivedRoomIdByOriginalId.put(m.getId(), drid);
                }
            }
        }

        List<Long> derivedRoomIds = new ArrayList<>(derivedRoomIdByOriginalId.values().stream()
                .distinct()
                .toList());

        Map<Long, OpenChatRoom> derivedRoomMap = derivedRoomIds.isEmpty()
                ? Map.of()
                : openChatRoomRepository.findAllById(derivedRoomIds).stream()
                        .collect(Collectors.toMap(OpenChatRoom::getId, r -> r));

        List<Long> senderIds = originalMap.values().stream()
                .filter(m -> !m.isDeleted())
                .map(OpenChatMessage::getSenderId)
                .distinct()
                .toList();

        Map<Long, String> nicknameMap = userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getName() != null ? u.getName() : ""));

        for (OpenChatMessage replyMsg : replyMessages) {
            Long origId = replyMsg.getReplyToMessageId();
            OpenChatMessage orig = originalMap.get(origId);

            if (orig == null) {
                result.put(replyMsg.getId(), ReplySourceDto.builder()
                        .replyToMessageId(origId)
                        .status(ReplySourceStatus.NOT_FOUND)
                        .replyToRoomType(replyMsg.getReplyToRoomType())
                        .replyToRoomId(replyMsg.getReplyToRoomId())
                        .build());
                continue;
            }

            if (orig.isDeleted()) {
                result.put(replyMsg.getId(), ReplySourceDto.builder()
                        .replyToMessageId(origId)
                        .status(ReplySourceStatus.DELETED)
                        .replyToRoomType(replyMsg.getReplyToRoomType())
                        .replyToRoomId(replyMsg.getReplyToRoomId())
                        .build());
                continue;
            }

            if (orig.getType() == OpenChatMessageType.REOPEN_CARD) {
                Long derivedRoomId = derivedRoomIdByOriginalId.get(orig.getId());
                OpenChatRoom derivedRoom = derivedRoomId != null ? derivedRoomMap.get(derivedRoomId) : null;
                ReplySourceStatus status;
                if (derivedRoom == null) {
                    status = ReplySourceStatus.NOT_FOUND;
                } else {
                    status = derivedRoom.isRecruitmentClosed()
                            ? ReplySourceStatus.RECRUITMENT_CLOSED
                            : ReplySourceStatus.RECRUITING;
                }
                result.put(replyMsg.getId(), ReplySourceDto.builder()
                        .replyToMessageId(origId)
                        .status(status)
                        .replyToSenderId(orig.getSenderId())
                        .replyToSenderNickname(nicknameMap.get(orig.getSenderId()))
                        .replyToRoomType(replyMsg.getReplyToRoomType())
                        .replyToRoomId(replyMsg.getReplyToRoomId())
                        .replyToDerivedRoomId(derivedRoomId)
                        .build());
                continue;
            }

            String preview = orig.getContent();
            if (preview != null && preview.length() > 100) {
                preview = preview.substring(0, 100);
            }
            result.put(replyMsg.getId(), ReplySourceDto.builder()
                    .replyToMessageId(origId)
                    .status(ReplySourceStatus.NORMAL)
                    .replyToSenderId(orig.getSenderId())
                    .replyToSenderNickname(nicknameMap.get(orig.getSenderId()))
                    .contentPreview(preview)
                    .replyToRoomType(replyMsg.getReplyToRoomType())
                    .replyToRoomId(replyMsg.getReplyToRoomId())
                    .build());
        }

        return result;
    }

    private Long parseDerivedRoomId(String content) {
        if (content == null) return null;
        if (objectMapper != null) {
            try {
                Map<?, ?> parsed = objectMapper.readValue(content, Map.class);
                Object raw = parsed.get("derivedRoomId");
                return raw != null ? ((Number) raw).longValue() : null;
            } catch (Exception e) {
                return null;
            }
        }
        try {
            int idx = content.indexOf("\"derivedRoomId\":");
            if (idx < 0) return null;
            String after = content.substring(idx + 16).trim();
            int end = 0;
            while (end < after.length() && Character.isDigit(after.charAt(end))) end++;
            if (end == 0) return null;
            return Long.parseLong(after.substring(0, end));
        } catch (Exception e) {
            return null;
        }
    }

    private void validateImageFiles(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            throw new CustomException(ErrorCode.OPEN_CHAT_IMAGE_EMPTY);
        }
        if (images.size() > MAX_IMAGE_COUNT) {
            throw new CustomException(ErrorCode.OPEN_CHAT_IMAGE_COUNT_EXCEEDED);
        }
        for (MultipartFile image : images) {
            if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
                throw new CustomException(ErrorCode.IMAGE_INVALID_FORMAT);
            }
            String originalFilename = image.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                throw new CustomException(ErrorCode.IMAGE_INVALID_FORMAT);
            }
            String safeName = Paths.get(originalFilename).getFileName().toString();
            int dotIndex = safeName.lastIndexOf('.');
            if (dotIndex < 0) {
                throw new CustomException(ErrorCode.IMAGE_INVALID_FORMAT);
            }
            String ext = safeName.substring(dotIndex).toLowerCase();
            if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
                throw new CustomException(ErrorCode.IMAGE_INVALID_FORMAT);
            }
            String contentType = image.getContentType();
            if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
                throw new CustomException(ErrorCode.IMAGE_INVALID_FORMAT);
            }
        }
    }
}
