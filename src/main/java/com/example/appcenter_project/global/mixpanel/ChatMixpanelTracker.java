package com.example.appcenter_project.global.mixpanel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMixpanelTracker {

    private final MixpanelService mixpanelService;

    public void trackMessageSent(Long userId, Long roomId, String roomType, Long messageId, String messageType, boolean isFirstMessage) {
        JSONObject props = roomProps(roomId, roomType);
        props.put("message_id", messageId);
        props.put("message_type", messageType);
        send(userId, "chat_message_sent", props);
        if (isFirstMessage) {
            send(userId, "chat_first_message_sent", props);
        }
    }

    public void trackRoomViewed(Long userId, Long roomId, String roomType) {
        send(userId, "chat_room_viewed", roomProps(roomId, roomType));
    }

    public void trackRoomJoined(Long userId, Long roomId, String roomType) {
        send(userId, "chat_room_joined", roomProps(roomId, roomType));
    }

    public void trackRecruitmentClosed(Long actorId, Long roomId, Long originRoomId, long participantCount) {
        JSONObject props = roomProps(roomId, "DERIVED");
        props.put("origin_room_id", originRoomId);
        props.put("participant_count", participantCount);
        send(actorId, "recruitment_closed", props);
    }

    private JSONObject roomProps(Long roomId, String roomType) {
        JSONObject props = new JSONObject();
        props.put("room_id", roomId);
        props.put("room_type", roomType);
        return props;
    }

    private void send(Long userId, String eventName, JSONObject props) {
        Runnable task = () -> {
            try {
                mixpanelService.trackEvent(userId.toString(), eventName, props);
            } catch (Exception e) {
                log.warn("Mixpanel 채팅 이벤트 추적 실패 - userId: {}, event: {}", userId, eventName);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}
