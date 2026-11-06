package com.example.appcenter_project.domain.fcm.service;

import com.example.appcenter_project.domain.fcm.entity.FcmOutbox;
import com.example.appcenter_project.domain.fcm.fixture.FcmOutboxFixture;
import com.example.appcenter_project.domain.fcm.repository.FcmOutboxRepository;
import com.example.appcenter_project.domain.user.repository.FcmTokenRepository;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class FcmAsyncSenderTest {

    @Mock
    FcmTokenRepository fcmTokenRepository;

    @Mock
    FcmOutboxRepository fcmOutboxRepository;

    @Mock
    RedisTemplate<String, Object> redisTemplate;

    @Mock
    FirebaseMessaging firebaseMessaging;

    @InjectMocks
    FcmAsyncSender fcmAsyncSender;

    @BeforeEach
    void stubRedis() {
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("data.path = /chat/open/1234 포함 — AC-1: CHAT_OPEN FCM data.path 필드")
    void should_include_data_path_chat_open_when_chat_open_routing() throws Exception {
        // given
        List<FcmOutbox> batch = FcmOutboxFixture.createChatOpenOutboxBatch(1234L, 1);
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        given(firebaseMessaging.sendEachForMulticast(captor.capture())).willReturn(null);

        // when
        fcmAsyncSender.sendOutboxBatch(batch, "채팅 제목", "채팅 내용").join();

        // then
        MulticastMessage message = captor.getValue();
        assertThat(message.getData()).containsEntry("path", "/chat/open/1234");
    }

    @Test
    @DisplayName("data.path = /chat/open/99 포함 — AC-2: CHAT_PERSONAL FCM data.path 필드")
    void should_include_data_path_chat_personal_when_chat_personal_routing() throws Exception {
        // given
        List<FcmOutbox> batch = FcmOutboxFixture.createChatPersonalOutboxBatch(99L, 1);
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        given(firebaseMessaging.sendEachForMulticast(captor.capture())).willReturn(null);

        // when
        fcmAsyncSender.sendOutboxBatch(batch, "채팅 제목", "채팅 내용").join();

        // then
        MulticastMessage message = captor.getValue();
        assertThat(message.getData()).containsEntry("path", "/chat/open/99");
    }

    @Test
    @DisplayName("data.path = /announcements/7 포함 — AC-3: ANNOUNCEMENT FCM data.path 필드")
    void should_include_data_path_announcements_when_announcement_routing() throws Exception {
        // given
        List<FcmOutbox> batch = FcmOutboxFixture.createAnnouncementOutboxBatch(7L, 1);
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        given(firebaseMessaging.sendEachForMulticast(captor.capture())).willReturn(null);

        // when
        fcmAsyncSender.sendOutboxBatch(batch, "공지 제목", "공지 내용").join();

        // then
        MulticastMessage message = captor.getValue();
        assertThat(message.getData()).containsEntry("path", "/announcements/7");
    }

    @Test
    @DisplayName("APNS thread-id = notice — AC-3: ANNOUNCEMENT APNS thread-id 고정값")
    void should_include_apns_threadId_notice_when_announcement_routing() throws Exception {
        // given
        List<FcmOutbox> batch = FcmOutboxFixture.createAnnouncementOutboxBatch(7L, 1);
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        given(firebaseMessaging.sendEachForMulticast(captor.capture())).willReturn(null);

        // when
        fcmAsyncSender.sendOutboxBatch(batch, "공지 제목", "공지 내용").join();

        // then
        MulticastMessage message = captor.getValue();
        assertThat(message.getApnsConfig().getAps().getThreadId()).isEqualTo("notice");
    }

    @Test
    @DisplayName("data.path = /complain/5 포함 — AC-4: COMPLAINT FCM data.path 필드")
    void should_include_data_path_complain_when_complaint_routing() throws Exception {
        // given
        List<FcmOutbox> batch = FcmOutboxFixture.createComplaintOutboxBatch(5L, 1);
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        given(firebaseMessaging.sendEachForMulticast(captor.capture())).willReturn(null);

        // when
        fcmAsyncSender.sendOutboxBatch(batch, "민원 제목", "민원 내용").join();

        // then
        MulticastMessage message = captor.getValue();
        assertThat(message.getData()).containsEntry("path", "/complain/5");
    }

    @Test
    @DisplayName("data.path = /roommate/list/3 포함 — AC-5: ROOMMATE_POST FCM data.path 필드")
    void should_include_data_path_roommate_list_when_roommate_post_routing() throws Exception {
        // given
        List<FcmOutbox> batch = FcmOutboxFixture.createRoommatePostOutboxBatch(3L, 1);
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        given(firebaseMessaging.sendEachForMulticast(captor.capture())).willReturn(null);

        // when
        fcmAsyncSender.sendOutboxBatch(batch, "룸메이트 제목", "룸메이트 내용").join();

        // then
        MulticastMessage message = captor.getValue();
        assertThat(message.getData()).containsEntry("path", "/roommate/list/3");
    }

    @Test
    @DisplayName("data.path 키 없음 — AC-6: routingType null인 경우 data.path 생략")
    void should_not_include_data_path_when_routing_is_null() throws Exception {
        // given
        List<FcmOutbox> batch = FcmOutboxFixture.createGenericOutboxBatch(1);
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        given(firebaseMessaging.sendEachForMulticast(captor.capture())).willReturn(null);

        // when
        fcmAsyncSender.sendOutboxBatch(batch, "제목", "내용").join();

        // then
        MulticastMessage message = captor.getValue();
        assertThat(message.getData()).doesNotContainKey("path");
    }

    @Test
    @DisplayName("APNS config 미포함 — AC-6: routingType null인 경우 APNS 필드 생략")
    void should_not_include_apns_config_when_routing_is_null() throws Exception {
        // given
        List<FcmOutbox> batch = FcmOutboxFixture.createGenericOutboxBatch(1);
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        given(firebaseMessaging.sendEachForMulticast(captor.capture())).willReturn(null);

        // when
        fcmAsyncSender.sendOutboxBatch(batch, "제목", "내용").join();

        // then
        MulticastMessage message = captor.getValue();
        assertThat(message.getApnsConfig()).isNull();
    }

    @Test
    @DisplayName("Android config 미포함 — AC-6: routingType null인 경우 Android 필드 생략")
    void should_not_include_android_config_when_routing_is_null() throws Exception {
        // given
        List<FcmOutbox> batch = FcmOutboxFixture.createGenericOutboxBatch(1);
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        given(firebaseMessaging.sendEachForMulticast(captor.capture())).willReturn(null);

        // when
        fcmAsyncSender.sendOutboxBatch(batch, "제목", "내용").join();

        // then
        MulticastMessage message = captor.getValue();
        assertThat(message.getAndroidConfig()).isNull();
    }

    @Test
    @DisplayName("APNS thread-id = chat_room_1234 포함 — CHAT_OPEN FCM APNS 필드")
    void should_include_apns_threadId_chat_room_1234_when_chat_open_routing() throws Exception {
        // given
        List<FcmOutbox> batch = FcmOutboxFixture.createChatOpenOutboxBatch(1234L, 1);
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        given(firebaseMessaging.sendEachForMulticast(captor.capture())).willReturn(null);

        // when
        fcmAsyncSender.sendOutboxBatch(batch, "채팅 제목", "채팅 내용").join();

        // then
        MulticastMessage message = captor.getValue();
        assertThat(message.getApnsConfig().getAps().getThreadId()).isEqualTo("chat_room_1234");
    }

    @Test
    @DisplayName("data.type = CHAT_OPEN 포함 — CHAT_OPEN FCM data type 필드")
    void should_include_data_type_CHAT_OPEN_when_chat_open_routing() throws Exception {
        // given
        List<FcmOutbox> batch = FcmOutboxFixture.createChatOpenOutboxBatch(1234L, 1);
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        given(firebaseMessaging.sendEachForMulticast(captor.capture())).willReturn(null);

        // when
        fcmAsyncSender.sendOutboxBatch(batch, "채팅 제목", "채팅 내용").join();

        // then
        MulticastMessage message = captor.getValue();
        assertThat(message.getData()).containsEntry("type", "CHAT_OPEN");
    }

    @Test
    @DisplayName("data.chatRoomId = 1234 포함 — CHAT_OPEN FCM data chatRoomId 필드")
    void should_include_data_chatRoomId_1234_when_chat_open_routing() throws Exception {
        // given
        List<FcmOutbox> batch = FcmOutboxFixture.createChatOpenOutboxBatch(1234L, 1);
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        given(firebaseMessaging.sendEachForMulticast(captor.capture())).willReturn(null);

        // when
        fcmAsyncSender.sendOutboxBatch(batch, "채팅 제목", "채팅 내용").join();

        // then
        MulticastMessage message = captor.getValue();
        assertThat(message.getData()).containsEntry("chatRoomId", "1234");
    }

    @Test
    @DisplayName("data.type = ANNOUNCEMENT 포함 — ANNOUNCEMENT FCM data type 필드")
    void should_include_data_type_ANNOUNCEMENT_when_announcement_routing() throws Exception {
        // given
        List<FcmOutbox> batch = FcmOutboxFixture.createAnnouncementOutboxBatch(7L, 1);
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        given(firebaseMessaging.sendEachForMulticast(captor.capture())).willReturn(null);

        // when
        fcmAsyncSender.sendOutboxBatch(batch, "공지 제목", "공지 내용").join();

        // then
        MulticastMessage message = captor.getValue();
        assertThat(message.getData()).containsEntry("type", "ANNOUNCEMENT");
    }

    @Test
    @DisplayName("data.noticeId = 7 포함 — ANNOUNCEMENT FCM data noticeId 필드")
    void should_include_data_noticeId_7_when_announcement_routing() throws Exception {
        // given
        List<FcmOutbox> batch = FcmOutboxFixture.createAnnouncementOutboxBatch(7L, 1);
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        given(firebaseMessaging.sendEachForMulticast(captor.capture())).willReturn(null);

        // when
        fcmAsyncSender.sendOutboxBatch(batch, "공지 제목", "공지 내용").join();

        // then
        MulticastMessage message = captor.getValue();
        assertThat(message.getData()).containsEntry("noticeId", "7");
    }

    @Test
    void updateIncludesNotificationAndActionSurvivesRetry() throws Exception {
        FcmOutbox update = FcmOutbox.create("device", "old", "old",
                        com.example.appcenter_project.domain.fcm.enums.FcmRoutingType.CHAT_PERSONAL, 10L, 123L)
                .createUpdate("room", "edited");
        update.markProcessing();
        update.markFailed("UNAVAILABLE", java.time.LocalDateTime.now());
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        given(firebaseMessaging.sendEachForMulticast(captor.capture())).willReturn(null);
        fcmAsyncSender.sendOutboxBatch(List.of(update), update.getTitle(), update.getBody()).join();
        assertThat(captor.getValue().getNotification().getTitle()).isEqualTo("room");
        assertThat(captor.getValue().getNotification().getBody()).isEqualTo("edited");
        assertThat(captor.getValue().getData())
                .containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
                        "action", "UPDATE",
                        "messageId", "123",
                        "type", "CHAT_PERSONAL",
                        "path", "/chat/open/10",
                        "chatRoomId", "10"));
    }

    @Test
    void sendsCancellationDataOnlyToOriginalDevice() throws Exception {
        FcmOutbox original = FcmOutbox.create("original-device", "room", "message",
                com.example.appcenter_project.domain.fcm.enums.FcmRoutingType.CHAT_OPEN, 10L, 123L);
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        given(firebaseMessaging.sendEachForMulticast(captor.capture())).willReturn(null);

        FcmOutbox cancel = original.createCancellation();
        fcmAsyncSender.sendOutboxBatch(List.of(cancel), cancel.getTitle(), cancel.getBody()).join();

        MulticastMessage message = captor.getValue();
        assertThat(message.getTokens()).containsExactly("original-device");
        assertThat(message.getNotification()).isNull();
        assertThat(message.getData())
                .containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
                        "type", "CHAT_OPEN",
                        "action", "DELETE",
                        "path", "/chat/open/10",
                        "chatRoomId", "10",
                        "messageId", "123"));
        assertThat(message.getApnsConfig().getAps().isContentAvailable()).isTrue();
        assertThat(message.getApnsConfig().getHeaders()).containsEntry("apns-push-type", "background");
    }

    @Test
    void normalNotificationContainsMessageIdAndRoutingData() throws Exception {
        FcmOutbox original = FcmOutbox.create("device", "room", "message",
                com.example.appcenter_project.domain.fcm.enums.FcmRoutingType.CHAT_OPEN, 10L, 123L);
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        given(firebaseMessaging.sendEachForMulticast(captor.capture())).willReturn(null);
        fcmAsyncSender.sendOutboxBatch(List.of(original), "room", "message").join();
        assertThat(captor.getValue().getNotification()).isNotNull();
        assertThat(captor.getValue().getData())
                .containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
                        "action", "CREATE",
                        "messageId", "123",
                        "type", "CHAT_OPEN",
                        "path", "/chat/open/10",
                        "chatRoomId", "10"));
    }

    // BR-792 AC2 — 즉시 알림 analytics_label 포맷 (buildAnalyticsLabel 단독 단위 테스트)

    @Test
    @DisplayName("analytics_label = openchat_create_msg_12345_u_67890 — BR-792 AC2: 즉시 알림 라벨 포맷")
    void should_set_analyticsLabel_immediate_format_when_userId_and_messageId_present() {
        // given
        FcmOutbox outbox = FcmOutboxFixture.createWithUserIdAndChatOpenImmediate(67890L, 42L, 12345L);

        // when
        String label = FcmAsyncSender.buildAnalyticsLabel(outbox);

        // then
        assertThat(label).isEqualTo("openchat_create_msg_12345_u_67890");
    }

    // BR-792 AC3 — 묶음 알림 analytics_label 포맷

    @Test
    @DisplayName("analytics_label = openchat_create_bundle_42_u_67890 — BR-792 AC3: 묶음 알림 라벨 포맷")
    void should_set_analyticsLabel_bundle_format_when_messageId_is_null() {
        // given
        FcmOutbox outbox = FcmOutboxFixture.createWithUserIdAndChatOpenBundle(67890L, 42L);

        // when
        String label = FcmAsyncSender.buildAnalyticsLabel(outbox);

        // then
        assertThat(label).isEqualTo("openchat_create_bundle_42_u_67890");
    }

    // BR-792 AC4 — userId null 처리

    @Test
    @DisplayName("analytics_label에 _u_unknown 포함 — BR-792 AC4: userId null이면 unknown으로 대체")
    void should_use_unknown_in_analyticsLabel_when_userId_is_null() {
        // given
        FcmOutbox outbox = FcmOutboxFixture.createWithNullUserIdAndChatOpenImmediate(42L, 12345L);

        // when
        String label = FcmAsyncSender.buildAnalyticsLabel(outbox);

        // then
        assertThat(label).isEqualTo("openchat_create_msg_12345_u_unknown");
    }

    // BR-792 AC6 — Android delivery receipt 활성화

    @Test
    @DisplayName("delivery_metrics_exported data=true 포함 — BR-792 AC6: Android delivery receipt 활성화")
    @SuppressWarnings("unchecked")
    void should_include_delivery_metrics_exported_data_when_outbox_has_routing() throws Exception {
        // given
        List<FcmOutbox> batch = FcmOutboxFixture.createChatOpenOutboxBatchWithUserId(42L, 1);
        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        BatchResponse batchResponse = mockBatchResponse(1, true, null);
        given(firebaseMessaging.sendEach(captor.capture())).willReturn(batchResponse);

        // when
        fcmAsyncSender.sendOutboxBatch(batch, "채팅 제목", "채팅 내용").join();

        // then
        Message message = captor.getValue().get(0);
        java.lang.reflect.Field dataField = Message.class.getDeclaredField("data");
        dataField.setAccessible(true);
        java.util.Map<String, String> data = (java.util.Map<String, String>) dataField.get(message);
        assertThat(data)
                .containsEntry("com.google.firebase.messaging.delivery_metrics_exported", "true");
    }

    // BR-792 AC7 — sendEach 전환

    @Test
    @DisplayName("sendEach(List<Message>) 호출 — BR-792 AC7: 토큰별 개별 Message로 sendEach 사용")
    void should_call_sendEach_with_individual_messages_when_multiple_outboxes() throws Exception {
        // given
        List<FcmOutbox> batch = FcmOutboxFixture.createChatOpenOutboxBatchWithUserId(42L, 2);
        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        BatchResponse batchResponse = mockBatchResponse(2, true, null);
        given(firebaseMessaging.sendEach(captor.capture())).willReturn(batchResponse);

        // when
        fcmAsyncSender.sendOutboxBatch(batch, "채팅 제목", "채팅 내용").join();

        // then
        assertThat(captor.getValue()).hasSize(2);
    }

    // BR-792 AC5 — Firebase messageId 저장

    @Test
    @DisplayName("FcmOutbox.firebaseMessageId 저장 — BR-792 AC5: 성공 응답의 messageId가 outbox에 기록됨")
    void should_record_firebaseMessageId_in_outbox_when_send_succeeds() throws Exception {
        // given
        FcmOutbox outbox = FcmOutboxFixture.createWithUserIdAndChatOpenImmediate(100L, 42L, 12345L);
        String returnedMessageId = "projects/uni-dorm/messages/0:1234567890";
        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        BatchResponse batchResponse = mockBatchResponse(1, true, returnedMessageId);
        given(firebaseMessaging.sendEach(captor.capture())).willReturn(batchResponse);

        // when
        fcmAsyncSender.sendOutboxBatch(List.of(outbox), "채팅 제목", "채팅 내용").join();

        // then
        assertThat(outbox.getFirebaseMessageId()).isEqualTo(returnedMessageId);
        then(fcmOutboxRepository).should().updateFirebaseMessageId(outbox.getId(), returnedMessageId);
    }

    // BR-792 AC9 — 기존 레코드 호환 (userId null outbox 처리 시 예외 없음)

    @Test
    @DisplayName("예외 없이 _u_unknown 라벨로 처리 — BR-792 AC9: userId null인 기존 레코드 호환")
    void should_process_legacy_outbox_without_exception_when_userId_is_null() {
        // given
        FcmOutbox outbox = FcmOutboxFixture.createWithNullUserIdAndChatOpenImmediate(42L, 12345L);

        // when
        String label = FcmAsyncSender.buildAnalyticsLabel(outbox);

        // then
        assertThat(label).contains("_u_unknown");
    }

    private BatchResponse mockBatchResponse(int count, boolean success, String messageId) {
        BatchResponse batchResponse = mock(BatchResponse.class);
        List<SendResponse> responses = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            SendResponse sendResponse = mock(SendResponse.class);
            given(sendResponse.isSuccessful()).willReturn(success);
            given(sendResponse.getMessageId()).willReturn(messageId);
            responses.add(sendResponse);
        }
        given(batchResponse.getResponses()).willReturn(responses);
        given(batchResponse.getSuccessCount()).willReturn(success ? count : 0);
        given(batchResponse.getFailureCount()).willReturn(success ? 0 : count);
        return batchResponse;
    }
}
