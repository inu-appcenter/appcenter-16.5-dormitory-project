package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.fcm.entity.FcmOutbox;
import com.example.appcenter_project.domain.fcm.entity.FcmToken;
import com.example.appcenter_project.domain.fcm.enums.FcmRoutingType;
import com.example.appcenter_project.domain.fcm.enums.OutboxStatus;
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
@Transactional
public class OpenChatNotificationService {

    private final OpenChatParticipantRepository participantRepository;
    private final OpenChatRoomRepository openChatRoomRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final FcmOutboxRepository fcmOutboxRepository;

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
                    return FcmOutbox.create(token, roomName, body, routingType, info.roomId(), null);
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

    public void cancelPendingChatNotifications(Long userId, Long roomId) {
        List<String> tokens = fcmTokenRepository.findAllByUserIdIn(List.of(userId)).stream()
                .map(FcmToken::getToken)
                .filter(t -> t != null)
                .toList();
        if (!tokens.isEmpty()) {
            fcmOutboxRepository.cancelPendingChatOutboxesByTokensAndRoom(tokens, roomId);
        }
    }

    public void sendImmediateNotifications(Long roomId, OpenChatRoomType roomType, Set<Long> onlineUserIds, String title, String body, Long messageId) {
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
                .map(userId -> FcmOutbox.create(tokenMap.get(userId), title, body, routingType, roomId, messageId))
                .toList();

        if (!outboxes.isEmpty()) {
            fcmOutboxRepository.saveAll(outboxes);
        }
    }

    /**
     * 메시지에 연결된 모든 수신자의 발송 대기/재시도 알림을 취소한다.
     * 발송 완료 알림은 원래 수신 토큰에 데이터 취소 이벤트를 예약한다.
     * 발송 중 알림과 messageId가 없는 묶음 알림은 별도로 처리해야 한다.
     */
    public void cancelNotificationsForMessage(Long roomId, Long messageId, OpenChatRoomType roomType) {
        FcmRoutingType routingType = roomType == OpenChatRoomType.PERSONAL ? FcmRoutingType.CHAT_PERSONAL : FcmRoutingType.CHAT_OPEN;

        // 이미 발송 중이거나 발송된 알림은 별도의 클라이언트 취소 처리가 필요
        fcmOutboxRepository.bulkMarkDelete(
                roomId, messageId, routingType, List.of(OutboxStatus.PENDING, OutboxStatus.FAILED), LocalDateTime.now());

        // 발송 완료된 알림
        List<FcmOutbox> sent = fcmOutboxRepository.findSentMessageNotificationsForUpdate(roomId, messageId, routingType);
        if (sent.isEmpty()) {
            return;
        }

        // 취소 알림 생성
        List<FcmOutbox> cancellations = sent.stream()
                .collect(Collectors.toMap(FcmOutbox::getToken, FcmOutbox::createCancellation, (first, duplicate) -> first))
                .values().stream().toList();
        fcmOutboxRepository.saveAll(cancellations);

        // 이미 발송된 알림 삭제 처리
        fcmOutboxRepository.markCancellationQueued(sent.stream().map(FcmOutbox::getId).toList(), LocalDateTime.now());
    }

    public void updateNotificationsForMessage(Long roomId, Long messageId, OpenChatRoomType roomType, String body) {
        FcmRoutingType routing = roomType == OpenChatRoomType.PERSONAL ? FcmRoutingType.CHAT_PERSONAL : FcmRoutingType.CHAT_OPEN;
        List<OutboxStatus> unsent = List.of(OutboxStatus.PENDING, OutboxStatus.FAILED);
        LocalDateTime now = LocalDateTime.now();

        //발송된 알림
        List<FcmOutbox> sent = fcmOutboxRepository.findSentMessageNotificationsForUpdate(roomId, messageId, routing);

        // 대기·실패 상태인 원본 알림의 본문만 최신 내용으로 변경 (action=CREATE 유지)
        fcmOutboxRepository.updateUnsentMessageContent(roomId, messageId, routing, unsent, body, now);

        // 최신 내용으로 UPDATE를 새로 생성하기 전에, 대기·실패 상태인 이전 UPDATE의 발송을 취소
        fcmOutboxRepository.supersedeUnsentUpdateMessage(roomId, messageId, routing, unsent, now);

        //Update용 알림 생성
        List<FcmOutbox> updates = sent.stream()
                .collect(Collectors.toMap(FcmOutbox::getToken,
                        original -> original.createUpdate(original.getTitle(), body), (first, duplicate) -> first))
                .values().stream().toList();

        if (!updates.isEmpty()) {
            fcmOutboxRepository.saveAll(updates);
        }
    }
}
