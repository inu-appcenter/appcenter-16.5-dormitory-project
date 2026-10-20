package com.example.appcenter_project.domain.fcm.service;

import com.example.appcenter_project.domain.fcm.entity.FcmOutbox;
import com.example.appcenter_project.domain.fcm.fixture.FcmOutboxFixture;
import com.example.appcenter_project.domain.fcm.repository.FcmOutboxRepository;
import com.example.appcenter_project.domain.user.repository.FcmTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

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
}
