package com.example.appcenter_project.domain.roommate.service;

import com.example.appcenter_project.common.image.entity.Image;
import com.example.appcenter_project.common.image.enums.ImageType;
import com.example.appcenter_project.common.image.service.ImageService;
import com.example.appcenter_project.domain.fcm.entity.FcmOutbox;
import com.example.appcenter_project.domain.fcm.entity.FcmToken;
import com.example.appcenter_project.domain.fcm.enums.FcmRoutingType;
import com.example.appcenter_project.domain.fcm.repository.FcmOutboxRepository;
import com.example.appcenter_project.domain.notification.dto.request.RequestNotificationDto;
import com.example.appcenter_project.domain.notification.entity.Notification;
import com.example.appcenter_project.domain.notification.service.NotificationService;
import com.example.appcenter_project.domain.openChat.enums.ChatNotificationMode;
import com.example.appcenter_project.domain.roommate.dto.request.RequestRoommateChatDto;
import com.example.appcenter_project.domain.roommate.dto.response.ResponseRoommateChatDto;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingChat;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.repository.FcmTokenRepository;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingChatRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingRoomRepository;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.config.RoommateWebSocketEventListener;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.example.appcenter_project.global.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoommateChattingChatService {

    private final RoommateChattingChatRepository chatRepository;
    private final RoommateChattingRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final FcmOutboxRepository fcmOutboxRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final ImageService imageService;

    public ResponseRoommateChatDto sendChat(Long userId, RequestRoommateChatDto requestRoommateChatDto) {
        log.info("💬 [채팅 전송 시작] userId: {}, roomId: {}, content: {}",
                userId, requestRoommateChatDto.getRoommateChattingRoomId(), requestRoommateChatDto.getContent());

        // 1. 보낸 사람 조회 (예외 처리 포함)
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        // 2. 채팅방 조회
        RoommateChattingRoom room = chatRoomRepository.findById(requestRoommateChatDto.getRoommateChattingRoomId())
                .orElseThrow(() -> new CustomException(ROOMMATE_CHAT_ROOM_NOT_FOUND));

        // 3. 보낸 사람 → 수신자 확인 및 보안 검증
        User receiver;
        if (room.getGuest().getId().equals(userId)) {
            receiver = room.getHost(); // 내가 요청자면 상대는 게시글 작성자
        } else if (room.getHost().getId().equals(userId)) {
            receiver = room.getGuest(); // 내가 작성자면 상대는 요청자
        } else {
            throw new CustomException(ROOMMATE_CHAT_ROOM_FORBIDDEN); // 해당 채팅방 소속이 아님
        }

        // 수신자가 방을 나간 상태면 메시지 수신 시 자동 재진입 (채팅 목록에 다시 표시)
        if (room.getHost().getId().equals(receiver.getId()) && room.isHostLeft()) {
            room.rejoinAsHost();
        } else if (room.getGuest().getId().equals(receiver.getId()) && room.isGuestLeft()) {
            room.rejoinAsGuest();
        }

        log.info("👥 [채팅방 참여자] 발신자: {} ({}), 수신자: {} ({})",
                sender.getId(), sender.getStudentNumber(),
                receiver.getId(), receiver.getStudentNumber());

        // 4. 수신자가 현재 WebSocket 방에 접속해 있는지 확인
        boolean isReceiverOnline = isUserOnlineInRoom(requestRoommateChatDto.getRoommateChattingRoomId(), receiver.getId());
        log.info("🔍 [수신자 온라인 상태] receiverId: {}, isOnline: {}", receiver.getId(), isReceiverOnline);

        // 5. 채팅 메시지 엔티티 생성 (수신자가 온라인이면 자동으로 읽음 처리)
        RoommateChattingChat chat = RoommateChattingChat.builder()
                .roommateChattingRoom(room)
                .member(sender)
                .content(requestRoommateChatDto.getContent())
                .readByReceiver(isReceiverOnline) // 수신자가 온라인이면 읽음 처리
                .build();

        // 6. DB에 저장
        RoommateChattingChat savedChat = chatRepository.save(chat);
        log.info("💾 [채팅 DB 저장 완료] chatId: {}, read: {}", savedChat.getId(), savedChat.isReadByReceiver());


        ResponseRoommateChatDto responseDto = ResponseRoommateChatDto.entityToDto(savedChat, null);
        String destination = "/sub/roommate/chat/" + room.getId();

        log.info("📡 [WebSocket 전송] destination: {}, chatId: {}", destination, savedChat.getId());
        messagingTemplate.convertAndSend(destination, responseDto);

        // 8. 수신자가 온라인이고 자동으로 읽음 처리된 경우, 읽음 알림 전송
        if (isReceiverOnline) {
            String readDestination = "/sub/roommate/chat/read/" + room.getId() + "/user/" + sender.getId();
            List<Long> readIds = List.of(savedChat.getId());
            log.info("📖 [자동 읽음 처리 알림] destination: {}, readIds: {}", readDestination, readIds);
            messagingTemplate.convertAndSend(readDestination, readIds);
        }

        if (!isReceiverOnline) {
            boolean isReceiverHost = room.getHost().getId().equals(receiver.getId());
            ChatNotificationMode mode = isReceiverHost
                    ? room.getHostNotificationMode()
                    : room.getGuestNotificationMode();
            if (mode == null) {
                mode = ChatNotificationMode.EVERY;
            }
            sendChatNotification(sender, receiver, room.getId(), chat.getContent(), mode);
        }

        return responseDto;
    }

    private void sendChatNotification(User sender, User receiver, Long chatRoomId, String content, ChatNotificationMode mode) {
        if (mode != ChatNotificationMode.EVERY) {
            return;
        }
        Notification chatNotification = notificationService.createChatNotification(sender.getName(), chatRoomId, content);
        List<FcmToken> tokens = fcmTokenRepository.findAllByUser(receiver);
        List<FcmOutbox> outboxes = tokens.stream()
                .map(token -> FcmOutbox.create(token.getToken(), chatNotification.getTitle(), chatNotification.getBody(),
                        FcmRoutingType.CHAT_ROOMMATE, chatRoomId))
                .toList();
        if (!outboxes.isEmpty()) {
            fcmOutboxRepository.saveAll(outboxes);
        }
    }

    public void markAsRead(Long roomId, Long userId) {
        RoommateChattingRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ROOMMATE_CHAT_ROOM_NOT_FOUND));

        User me = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        // 내가 보낸 메시지를 제외하고, 읽지 않은 메시지들을 모두 읽음 처리
        List<RoommateChattingChat> unreadMessages = chatRepository.findByRoommateChattingRoomAndMemberNotAndReadByReceiverFalse(room, me);

        List<Long> readIds = new ArrayList<>();
        unreadMessages.forEach(chat -> {
            chat.markAsRead();
            readIds.add(chat.getId());
        });

        // 읽음 처리된 메시지 ID들을 발신자(상대방)에게 실시간 전송
        if (!readIds.isEmpty()) {
            Long otherUserId = room.getHost().getId().equals(userId) ? room.getGuest().getId() : room.getHost().getId();
            String destination = "/sub/roommate/chat/read/" + roomId + "/user/" + otherUserId;
            log.info("📖 [실시간 읽음 처리] destination: {}, readIds: {}", destination, readIds);
            messagingTemplate.convertAndSend(destination, readIds);
        }
    }

    public Integer getUnReadCountByUserIdAdRoomId(Long userId, Long roomId) {
        RoommateChattingRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ROOMMATE_CHAT_ROOM_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        return chatRepository.findByRoommateChattingRoomAndMemberNotAndReadByReceiverFalse(room, user).size();
    }

    public void sendSystemMessage(RoommateChattingRoom room, String content) {
        RoommateChattingChat systemChat = RoommateChattingChat.createSystemMessage(room, content);
        chatRepository.save(systemChat);
        ResponseRoommateChatDto dto = ResponseRoommateChatDto.systemDto(room.getId(), content);
        messagingTemplate.convertAndSend("/sub/roommate/chat/" + room.getId(), dto);
    }

    @Transactional
    public void sendSystemMessageById(Long roomId, String content) {
        RoommateChattingRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ROOMMATE_CHAT_ROOM_NOT_FOUND));
        sendSystemMessage(room, content);
    }

    @Transactional
    public void sendStudentIdRequestMessage(Long roomId, Long requesterId, Long requestId) {
        RoommateChattingRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ROOMMATE_CHAT_ROOM_NOT_FOUND));
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
        String content = "{\"requestId\":" + requestId + ",\"requesterId\":" + requesterId
                + ",\"requesterNickname\":\"" + requester.getName() + "\"}";
        RoommateChattingChat chat = RoommateChattingChat.createStudentIdRequestMessage(
                room, requester, content, requestId);
        chatRepository.save(chat);
        ResponseRoommateChatDto dto = ResponseRoommateChatDto.entityToDto(chat, null);
        messagingTemplate.convertAndSend("/sub/roommate/chat/" + roomId, dto);
    }

    public Integer getUnReadCountByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        Integer result = 0;
        List<RoommateChattingRoom> chattingRooms = chatRoomRepository.findAllByHostOrGuest(user, user);
        for (RoommateChattingRoom chattingRoom : chattingRooms) {
            Integer unReadCountByUserIdAdRoomId = getUnReadCountByUserIdAdRoomId(userId, chattingRoom.getId());
            result +=  unReadCountByUserIdAdRoomId;
        }

        return result;
    }

    // 사용자가 특정 채팅방에 온라인 상태인지 확인하는 메서드
    private boolean isUserOnlineInRoom(Long roomId, Long userId) {
        // RoommateWebSocketEventListener의 static 맵을 참조
        List<String> onlineUsers = RoommateWebSocketEventListener.roommateChatRoomInUserMap.get(roomId.toString());

        if (onlineUsers == null) {
            return false;
        }

        boolean isOnline = onlineUsers.contains(userId.toString());
        log.debug("🔍 [사용자 온라인 상태 확인] roomId: {}, userId: {}, onlineUsers: {}, isOnline: {}",
                roomId, userId, onlineUsers, isOnline);

        return isOnline;
    }

    @Transactional(readOnly = true)
    public List<ResponseRoommateChatDto> getChatList(Long userId, Long roomId, HttpServletRequest request) {
        RoommateChattingRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ROOMMATE_CHAT_ROOM_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        // 접근 권한 확인
        if (!room.getGuest().getId().equals(userId) && !room.getHost().getId().equals(userId)) {
            throw new CustomException(ROOMMATE_CHAT_ROOM_FORBIDDEN); // 이 채팅방에 속하지 않은 사용자입니다.
        }

        // 채팅 내역 조회
        List<RoommateChattingChat> chatList = chatRepository.findByRoommateChattingRoom(room);

        // 안 읽은 메시지 읽음 처리 (내가 보낸 거 제외)
        List<Long> readIds = new ArrayList<>();
        chatList.stream()
                .filter(chat -> chat.getMember() != null
                        && !chat.getMember().getId().equals(userId)
                        && !chat.isReadByReceiver())
                .forEach(chat -> {
                    chat.markAsRead();
                    readIds.add(chat.getId());
                });

        // 읽음 처리된 메시지가 있으면 발신자(상대방)에게 알림 전송
        if (!readIds.isEmpty()) {
            Long otherUserId = room.getHost().getId().equals(userId) ? room.getGuest().getId() : room.getHost().getId();
            String destination = "/sub/roommate/chat/read/" + roomId + "/user/" + otherUserId;
            log.info("📖 [채팅 조회 시 읽음 처리] destination: {}, readIds: {}", destination, readIds);
            messagingTemplate.convertAndSend(destination, readIds);
        }

        return chatList.stream()
                .map(chat -> {
                    String imageUrl = chat.getMember() != null
                            ? imageService.findImage(ImageType.USER, chat.getMember().getId(), request).getImageUrl()
                            : null;
                    return ResponseRoommateChatDto.entityToDto(chat, imageUrl);
                })
                .toList();
    }
}
