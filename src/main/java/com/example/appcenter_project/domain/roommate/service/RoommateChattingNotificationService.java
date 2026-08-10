package com.example.appcenter_project.domain.roommate.service;

import com.example.appcenter_project.domain.fcm.entity.FcmOutbox;
import com.example.appcenter_project.domain.fcm.entity.FcmToken;
import com.example.appcenter_project.domain.fcm.enums.FcmRoutingType;
import com.example.appcenter_project.domain.fcm.repository.FcmOutboxRepository;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseNotificationModeDto;
import com.example.appcenter_project.domain.openChat.enums.ChatNotificationMode;
import com.example.appcenter_project.domain.roommate.dto.RoommateUnreadNotificationInfo;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingChatRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingRoomRepository;
import com.example.appcenter_project.domain.user.repository.FcmTokenRepository;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoommateChattingNotificationService {

    private final RoommateChattingRoomRepository roommateChattingRoomRepository;
    private final RoommateChattingChatRepository roommateChattingChatQuerydslRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final FcmOutboxRepository fcmOutboxRepository;

    @Transactional
    public ResponseNotificationModeDto updateNotificationMode(Long userId, Long roomId, ChatNotificationMode mode) {
        RoommateChattingRoom room = roommateChattingRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOMMATE_CHAT_ROOM_NOT_FOUND));

        boolean isHost = room.getHost().getId().equals(userId);
        boolean isGuest = room.getGuest().getId().equals(userId);

        if (!isHost && !isGuest) {
            throw new CustomException(ErrorCode.ROOMMATE_CHAT_ROOM_FORBIDDEN);
        }

        if (isHost) {
            room.updateHostNotificationMode(mode);
        } else {
            room.updateGuestNotificationMode(mode);
        }

        return ResponseNotificationModeDto.of(mode);
    }

    @Transactional(readOnly = true)
    public ResponseNotificationModeDto getNotificationMode(Long userId, Long roomId) {
        RoommateChattingRoom room = roommateChattingRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOMMATE_CHAT_ROOM_NOT_FOUND));

        boolean isHost = room.getHost().getId().equals(userId);
        boolean isGuest = room.getGuest().getId().equals(userId);

        if (!isHost && !isGuest) {
            throw new CustomException(ErrorCode.ROOMMATE_CHAT_ROOM_FORBIDDEN);
        }

        ChatNotificationMode mode = isHost ? room.getHostNotificationMode() : room.getGuestNotificationMode();
        return ResponseNotificationModeDto.of(mode);
    }

    @Transactional
    public void sendHourlyUnreadNotifications() {
        List<RoommateUnreadNotificationInfo> unreadInfos =
                roommateChattingChatQuerydslRepository.findUnreadCountsForBundled();

        if (unreadInfos.isEmpty()) {
            return;
        }

        List<Long> userIds = unreadInfos.stream()
                .map(RoommateUnreadNotificationInfo::userId)
                .collect(Collectors.toList());

        Map<Long, FcmToken> tokenMap = fcmTokenRepository.findAllByUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(
                        t -> t.getUser().getId(),
                        t -> t,
                        (a, b) -> a
                ));

        List<FcmOutbox> outboxes = unreadInfos.stream()
                .filter(info -> tokenMap.containsKey(info.userId()))
                .map(info -> {
                    FcmToken token = tokenMap.get(info.userId());
                    String body = "새 메시지 " + info.unreadCount() + "개";
                    return FcmOutbox.create(token.getToken(), "룸메이트 채팅", body,
                            FcmRoutingType.CHAT_ROOMMATE, info.roomId());
                })
                .collect(Collectors.toList());

        if (!outboxes.isEmpty()) {
            fcmOutboxRepository.saveAll(outboxes);
        }
    }
}
