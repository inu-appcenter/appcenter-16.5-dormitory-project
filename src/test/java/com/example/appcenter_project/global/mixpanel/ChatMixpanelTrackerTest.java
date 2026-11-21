package com.example.appcenter_project.global.mixpanel;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatMixpanelTrackerTest {

    @Mock MixpanelService mixpanelService;
    @InjectMocks ChatMixpanelTracker tracker;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void commit() {
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }

    @Test
    @DisplayName("[BR-TBD] 커밋 전에는 발화하지 않고, 커밋 후에 발화한다")
    void firesOnlyAfterCommit() {
        tracker.trackMessageSent(1L, 10L, "OPEN", 100L, "TEXT", false);

        verify(mixpanelService, never()).trackEvent(any(), any(), any());

        commit();

        verify(mixpanelService).trackEvent(eq("1"), eq("chat_message_sent"), any(JSONObject.class));
    }

    @Test
    @DisplayName("[BR-TBD] 롤백(커밋 없음)이면 발화하지 않는다")
    void doesNotFireOnRollback() {
        tracker.trackMessageSent(1L, 10L, "OPEN", 100L, "TEXT", true);

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(mixpanelService, never()).trackEvent(any(), any(), any());
    }

    @Test
    @DisplayName("[BR-TBD] 메시지 이벤트 속성: room_id, room_type, message_id, message_type")
    void messageSentProperties() {
        tracker.trackMessageSent(1L, 10L, "DERIVED", 100L, "IMAGE", false);
        commit();

        ArgumentCaptor<JSONObject> props = ArgumentCaptor.forClass(JSONObject.class);
        verify(mixpanelService).trackEvent(eq("1"), eq("chat_message_sent"), props.capture());
        assertThat(props.getValue().getLong("room_id")).isEqualTo(10L);
        assertThat(props.getValue().getString("room_type")).isEqualTo("DERIVED");
        assertThat(props.getValue().getLong("message_id")).isEqualTo(100L);
        assertThat(props.getValue().getString("message_type")).isEqualTo("IMAGE");
    }

    @Test
    @DisplayName("[BR-TBD] 첫 메시지면 chat_first_message_sent 도 함께 발화한다")
    void firstMessageFiresBothEvents() {
        tracker.trackMessageSent(1L, 10L, "ROOMMATE", 100L, "TEXT", true);
        commit();

        verify(mixpanelService).trackEvent(eq("1"), eq("chat_message_sent"), any(JSONObject.class));
        verify(mixpanelService).trackEvent(eq("1"), eq("chat_first_message_sent"), any(JSONObject.class));
    }

    @Test
    @DisplayName("[BR-TBD] 첫 메시지가 아니면 chat_first_message_sent 는 발화하지 않는다")
    void notFirstMessage() {
        tracker.trackMessageSent(1L, 10L, "ROOMMATE", 100L, "TEXT", false);
        commit();

        verify(mixpanelService, never()).trackEvent(any(), eq("chat_first_message_sent"), any());
    }

    @Test
    @DisplayName("[BR-TBD] 트랜잭션 밖에서는 즉시 발화한다")
    void firesImmediatelyWithoutTransaction() {
        TransactionSynchronizationManager.clearSynchronization();

        tracker.trackRoomViewed(1L, 10L, "ROOMMATE");

        verify(mixpanelService).trackEvent(eq("1"), eq("chat_room_viewed"), any(JSONObject.class));
    }

    @Test
    @DisplayName("[BR-TBD] 방 참여 이벤트")
    void roomJoined() {
        tracker.trackRoomJoined(1L, 10L, "OPEN");
        commit();

        verify(mixpanelService).trackEvent(eq("1"), eq("chat_room_joined"), any(JSONObject.class));
    }

    @Test
    @DisplayName("[BR-TBD] 모집 마감 이벤트 속성: origin_room_id, participant_count")
    void recruitmentClosed() {
        tracker.trackRecruitmentClosed(2L, 10L, 5L, 7L);
        commit();

        ArgumentCaptor<JSONObject> props = ArgumentCaptor.forClass(JSONObject.class);
        verify(mixpanelService).trackEvent(eq("2"), eq("recruitment_closed"), props.capture());
        assertThat(props.getValue().getString("room_type")).isEqualTo("DERIVED");
        assertThat(props.getValue().getLong("origin_room_id")).isEqualTo(5L);
        assertThat(props.getValue().getLong("participant_count")).isEqualTo(7L);
    }

    @Test
    @DisplayName("[BR-TBD] MixpanelService 예외는 호출자에게 전파되지 않는다")
    void swallowsMixpanelFailure() {
        org.mockito.Mockito.doThrow(new RuntimeException("boom"))
                .when(mixpanelService).trackEvent(any(), any(), any());

        tracker.trackRoomJoined(1L, 10L, "OPEN");
        commit();
    }
}
