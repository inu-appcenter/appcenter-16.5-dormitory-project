package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.common.image.entity.Image;
import com.example.appcenter_project.common.image.enums.ImageType;
import com.example.appcenter_project.common.image.repository.ImageRepository;
import com.example.appcenter_project.common.image.service.ImageService;
import com.example.appcenter_project.domain.openChat.dto.request.RequestOpenChatMessageDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseAdminChatRoomDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatMessageDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatMessageListDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatReadEventDto;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomType;
import com.example.appcenter_project.domain.openChat.entity.OpenChatMessage;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatMessageType;
import com.example.appcenter_project.domain.openChat.repository.OpenChatMessageRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatParticipantRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomRepository;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.config.OpenChatSessionRegistry;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.exception.ErrorCode;
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
        openChatMessageRepository.save(message);

        room.updateLastMessage(message.getContent(), message.getCreatedDate());

        Set<Long> usersToRead = new HashSet<>(sessionRegistry.getSubscriberUserIds(request.getRoomId()));
        usersToRead.add(userId);
        openChatParticipantRepository.updateLastReadMessageIdByRoomIdAndUserIdIn(request.getRoomId(), usersToRead, message.getId());

        int unreadCount = calculateUnreadCount(request.getRoomId(), message.getId());

        ResponseOpenChatMessageDto response = ResponseOpenChatMessageDto.from(message, sender.getName(), unreadCount);
        messagingTemplate.convertAndSend("/sub/openchat/" + request.getRoomId(), response);
        messagingTemplate.convertAndSend("/sub/openchat/" + request.getRoomId() + "/read",
                ResponseOpenChatReadEventDto.of(message.getId(), unreadCount));

        if (openChatNotificationService != null) {
            openChatNotificationService.sendImmediateNotifications(
                    request.getRoomId(), room.getRoomType(), usersToRead, room.getName(), request.getContent());
        }
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

        List<OpenChatMessage> messages = openChatMessageRepository.findByRoomIdWithCursor(roomId, lastMessageId, size + 1);

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

        List<ResponseOpenChatMessageDto> dtos = messages.stream()
                .map(msg -> {
                    String nickname = msg.getType() == OpenChatMessageType.SYSTEM
                            ? null
                            : nicknameByUserId.get(msg.getSenderId());
                    int unreadCount = calculateUnreadCount(roomId, msg.getId());
                    if (msg.getType() == OpenChatMessageType.ROOM_LINK) {
                        return toRoomLinkDto(msg, nickname, unreadCount);
                    }
                    if (msg.getType() == OpenChatMessageType.STUDENT_ID_REQUEST) {
                        return toStudentIdRequestDto(msg, nickname, unreadCount);
                    }
                    List<String> imageUrls = msg.getType() == OpenChatMessageType.IMAGE
                            ? imageUrlsMap.getOrDefault(msg.getId(), List.of())
                            : List.of();
                    return ResponseOpenChatMessageDto.from(msg, nickname, unreadCount, imageUrls);
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

        openChatMessageRepository.findLatestMessageIdByRoomId(roomId).ifPresent(latestId ->
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
        try {
            Map<?, ?> parsed = objectMapper.readValue(msg.getContent(), Map.class);
            Long derivedRoomId = ((Number) parsed.get("derivedRoomId")).longValue();
            String roomName = (String) parsed.get("roomName");
            String description = (String) parsed.get("description");
            Integer maxParticipants = parsed.get("maxParticipants") != null
                    ? ((Number) parsed.get("maxParticipants")).intValue() : null;
            return ResponseOpenChatMessageDto.fromRoomLink(msg, nickname, unreadCount,
                    derivedRoomId, roomName, description, maxParticipants);
        } catch (Exception e) {
            return ResponseOpenChatMessageDto.from(msg, nickname, unreadCount, List.of());
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
