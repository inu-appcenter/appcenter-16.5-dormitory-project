package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.fcm.entity.FcmOutbox;
import com.example.appcenter_project.domain.fcm.entity.FcmToken;
import com.example.appcenter_project.domain.fcm.enums.FcmRoutingType;
import com.example.appcenter_project.domain.fcm.repository.FcmOutboxRepository;
import com.example.appcenter_project.domain.openChat.dto.UnreadNotificationInfo;
import com.example.appcenter_project.domain.openChat.entity.OpenChatParticipant;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.ChatNotificationMode;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomType;
import com.example.appcenter_project.domain.openChat.repository.OpenChatParticipantRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomRepository;
import com.example.appcenter_project.domain.user.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenChatNotificationService {

    private final OpenChatParticipantRepository participantRepository;
    private final OpenChatRoomRepository openChatRoomRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final FcmOutboxRepository fcmOutboxRepository;

    @Transactional
    public void sendHourlyUnreadNotifications() {
        List<UnreadNotificationInfo> unreadInfos = participantRepository.findUnreadCountsForNotification();

        log.info("[BUNDLE-DIAG] 알림 대상 participant 수: {}", unreadInfos.size());
        unreadInfos.forEach(info ->
                log.info("[BUNDLE-DIAG] roomId={}, userId={}, unreadCount={}", info.roomId(), info.userId(), info.unreadCount()));

        if (unreadInfos.isEmpty()) {
            return;
        }

        Set<Long> roomIds = unreadInfos.stream()
                .map(UnreadNotificationInfo::roomId)
                .collect(Collectors.toSet());
        Set<Long> userIds = unreadInfos.stream()
                .map(UnreadNotificationInfo::userId)
                .collect(Collectors.toSet());

        List<OpenChatRoom> rooms = openChatRoomRepository.findAllById(roomIds);
        Map<Long, String> roomNameMap = rooms.stream()
                .collect(Collectors.toMap(OpenChatRoom::getId, OpenChatRoom::getName));
        Map<Long, OpenChatRoomType> roomTypeMap = rooms.stream()
                .collect(Collectors.toMap(OpenChatRoom::getId, OpenChatRoom::getRoomType));

        Map<Long, String> userTokenMap = fcmTokenRepository.findAllByUserIdIn(new ArrayList<>(userIds)).stream()
                .collect(Collectors.toMap(
                        token -> token.getUser().getId(),
                        FcmToken::getToken,
                        (existing, replacement) -> existing
                ));

        List<FcmOutbox> outboxes = unreadInfos.stream()
                .filter(info -> userTokenMap.containsKey(info.userId()))
                .map(info -> {
                    String token = userTokenMap.get(info.userId());
                    String roomName = roomNameMap.getOrDefault(info.roomId(), "오픈채팅");
                    String body = "새 메시지 " + info.unreadCount() + "개";
                    OpenChatRoomType roomType = roomTypeMap.getOrDefault(info.roomId(), OpenChatRoomType.OPEN);
                    FcmRoutingType routingType = (roomType == OpenChatRoomType.PERSONAL)
                            ? FcmRoutingType.CHAT_PERSONAL : FcmRoutingType.CHAT_OPEN;
                    return FcmOutbox.create(token, roomName, body, routingType, info.roomId());
                })
                .toList();

        if (!outboxes.isEmpty()) {
            fcmOutboxRepository.saveAll(outboxes);
            log.info("오픈채팅 시간별 알림 배치 완료: {}건 발송 예약", outboxes.size());
        }

        LocalDateTime notifiedAt = LocalDateTime.now();
        Map<String, UnreadNotificationInfo> infoMap = unreadInfos.stream()
                .collect(Collectors.toMap(
                        info -> info.roomId() + ":" + info.userId(),
                        info -> info
                ));

        List<OpenChatParticipant> targets = participantRepository.findAllByRoomIdIn(new ArrayList<>(roomIds)).stream()
                .filter(p -> infoMap.containsKey(p.getRoomId() + ":" + p.getUserId()))
                .toList();

        log.info("[BUNDLE-DIAG] 갱신 대상 participant 수: {}", targets.size());
        targets.forEach(p ->
                log.info("[BUNDLE-DIAG] 갱신 전 roomId={}, userId={}, lastBundledNotifiedAt={}", p.getRoomId(), p.getUserId(), p.getLastBundledNotifiedAt()));

        targets.forEach(p -> p.updateLastBundledNotifiedAt(notifiedAt));
        log.info("[BUNDLE-DIAG] lastBundledNotifiedAt → {} 로 갱신 완료", notifiedAt);
    }

    @Transactional
    public void cancelPendingChatNotifications(Long userId, Long roomId) {
        List<String> tokens = fcmTokenRepository.findAllByUserIdIn(List.of(userId)).stream()
                .map(FcmToken::getToken)
                .filter(t -> t != null)
                .toList();
        if (!tokens.isEmpty()) {
            fcmOutboxRepository.cancelPendingChatOutboxesByTokensAndRoom(tokens, roomId);
        }
    }

    @Transactional
    public void sendImmediateNotifications(Long roomId, OpenChatRoomType roomType, Set<Long> onlineUserIds, String title, String body) {
        List<OpenChatParticipant> everyParticipants =
                participantRepository.findAllByRoomIdAndNotificationMode(roomId, ChatNotificationMode.EVERY);

        List<Long> targetUserIds = everyParticipants.stream()
                .map(OpenChatParticipant::getUserId)
                .filter(userId -> !onlineUserIds.contains(userId))
                .toList();

        if (targetUserIds.isEmpty()) {
            return;
        }

        Map<Long, String> tokenMap = fcmTokenRepository.findAllByUserIdIn(targetUserIds).stream()
                .collect(Collectors.toMap(
                        token -> token.getUser().getId(),
                        FcmToken::getToken,
                        (existing, replacement) -> existing
                ));

        FcmRoutingType routingType = (roomType == OpenChatRoomType.PERSONAL)
                ? FcmRoutingType.CHAT_PERSONAL : FcmRoutingType.CHAT_OPEN;

        List<FcmOutbox> outboxes = targetUserIds.stream()
                .filter(tokenMap::containsKey)
                .map(userId -> FcmOutbox.create(tokenMap.get(userId), title, body, routingType, roomId))
                .toList();

        if (!outboxes.isEmpty()) {
            fcmOutboxRepository.saveAll(outboxes);
        }
    }
}
