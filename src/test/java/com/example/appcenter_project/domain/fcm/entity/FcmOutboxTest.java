package com.example.appcenter_project.domain.fcm.entity;

import com.example.appcenter_project.domain.fcm.enums.FcmRoutingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FcmOutboxTest {

    @Test
    void createPreservesMessageIdForCancellation() {
        FcmOutbox outbox = FcmOutbox.create("device", "room", "text", FcmRoutingType.CHAT_OPEN, 10L, 123L);
        assertThat(outbox.getMessageId()).isEqualTo(123L);
        assertThat(outbox.createCancellation().getMessageId()).isEqualTo(123L);
    }

    @Test
    void nonMessageNotificationsMayOmitMessageId() {
        assertThat(FcmOutbox.create("device", "title", "body").getMessageId()).isNull();
        assertThat(FcmOutbox.create("device", "title", "body", FcmRoutingType.ANNOUNCEMENT, 1L)
                .getMessageId()).isNull();
        assertThat(FcmOutbox.create("device", "room", "새 메시지 3개", FcmRoutingType.CHAT_OPEN, 10L, null)
                .getMessageId()).isNull();
    }

    @Test
    @DisplayName("routingType = ANNOUNCEMENT 저장 — AC-3: 공지사항 routing 저장")
    void should_set_routingType_ANNOUNCEMENT_when_announcement_routing_provided() {
        // given
        Long announcementId = 7L;

        // when
        FcmOutbox outbox = FcmOutbox.create("token", "제목", "내용", FcmRoutingType.ANNOUNCEMENT, announcementId);

        // then
        assertThat(outbox.getRoutingType()).isEqualTo(FcmRoutingType.ANNOUNCEMENT);
    }

    @Test
    @DisplayName("routingId = 7 저장 — AC-3: 공지사항 announcementId 저장")
    void should_set_routingId_when_announcement_routing_provided() {
        // given
        Long announcementId = 7L;

        // when
        FcmOutbox outbox = FcmOutbox.create("token", "제목", "내용", FcmRoutingType.ANNOUNCEMENT, announcementId);

        // then
        assertThat(outbox.getRoutingId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("routingType = CHAT_OPEN 저장 — AC-1: 오픈채팅 즉시 알림 routing 저장")
    void should_set_routingType_CHAT_OPEN_when_chat_open_routing_provided() {
        // given
        Long roomId = 1234L;

        // when
        FcmOutbox outbox = FcmOutbox.create("token", "채팅 제목", "채팅 내용", FcmRoutingType.CHAT_OPEN, roomId);

        // then
        assertThat(outbox.getRoutingType()).isEqualTo(FcmRoutingType.CHAT_OPEN);
    }

    @Test
    @DisplayName("routingType = CHAT_PERSONAL 저장 — AC-2: 개인채팅 즉시 알림 routing 저장")
    void should_set_routingType_CHAT_PERSONAL_when_chat_personal_routing_provided() {
        // given
        Long roomId = 99L;

        // when
        FcmOutbox outbox = FcmOutbox.create("token", "채팅 제목", "채팅 내용", FcmRoutingType.CHAT_PERSONAL, roomId);

        // then
        assertThat(outbox.getRoutingType()).isEqualTo(FcmRoutingType.CHAT_PERSONAL);
    }

    @Test
    @DisplayName("routingType = COMPLAINT 저장 — AC-4: 민원 routing 저장")
    void should_set_routingType_COMPLAINT_when_complaint_routing_provided() {
        // given
        Long complaintId = 5L;

        // when
        FcmOutbox outbox = FcmOutbox.create("token", "민원 제목", "민원 내용", FcmRoutingType.COMPLAINT, complaintId);

        // then
        assertThat(outbox.getRoutingType()).isEqualTo(FcmRoutingType.COMPLAINT);
    }

    @Test
    @DisplayName("routingType = ROOMMATE_POST 저장 — AC-5: 룸메이트 게시글 routing 저장")
    void should_set_routingType_ROOMMATE_POST_when_roommate_post_routing_provided() {
        // given
        Long postId = 3L;

        // when
        FcmOutbox outbox = FcmOutbox.create("token", "룸메이트 제목", "룸메이트 내용", FcmRoutingType.ROOMMATE_POST, postId);

        // then
        assertThat(outbox.getRoutingType()).isEqualTo(FcmRoutingType.ROOMMATE_POST);
    }

    @Test
    @DisplayName("routing 없는 경우 routingType = null — AC-6: 기존 동작 유지")
    void should_have_null_routingType_when_no_routing_create() {
        // given / when
        FcmOutbox outbox = FcmOutbox.create("token", "제목", "내용");

        // then
        assertThat(outbox.getRoutingType()).isNull();
    }

    @Test
    @DisplayName("routing 없는 경우 routingId = null — AC-6: 기존 동작 유지")
    void should_have_null_routingId_when_no_routing_create() {
        // given / when
        FcmOutbox outbox = FcmOutbox.create("token", "제목", "내용");

        // then
        assertThat(outbox.getRoutingId()).isNull();
    }

    // BR-792 AC1 — userId 저장

    @Test
    @DisplayName("userId 저장 — BR-792 AC1: create(userId, ...) 호출 시 userId 보존")
    void should_preserve_userId_when_create_with_userId() {
        // given
        Long userId = 67890L;

        // when
        FcmOutbox outbox = FcmOutbox.create(userId, "token", "제목", "내용",
                FcmRoutingType.CHAT_OPEN, 42L, 12345L);

        // then
        assertThat(outbox.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("userId = null 허용 — BR-792 AC1: null userId로 create 시 예외 없이 생성")
    void should_allow_null_userId_when_create_with_null_userId() {
        // given / when
        FcmOutbox outbox = FcmOutbox.create(null, "token", "제목", "내용",
                FcmRoutingType.CHAT_OPEN, 42L, 12345L);

        // then
        assertThat(outbox.getUserId()).isNull();
    }

    @Test
    @DisplayName("firebaseMessageId 초기값 null — BR-792 AC5: 신규 Outbox는 firebaseMessageId가 null")
    void should_have_null_firebaseMessageId_when_newly_created() {
        // given / when
        FcmOutbox outbox = FcmOutbox.create(100L, "token", "제목", "내용",
                FcmRoutingType.CHAT_OPEN, 42L, 12345L);

        // then
        assertThat(outbox.getFirebaseMessageId()).isNull();
    }

    @Test
    @DisplayName("recordFirebaseMessageId 저장 — BR-792 AC5: 성공 응답 messageId가 outbox에 저장됨")
    void should_store_firebaseMessageId_when_recordFirebaseMessageId_called() {
        // given
        FcmOutbox outbox = FcmOutbox.create(100L, "token", "제목", "내용",
                FcmRoutingType.CHAT_OPEN, 42L, 12345L);
        String firebaseMessageId = "projects/uni-dorm/messages/0:1234567890";

        // when
        outbox.recordFirebaseMessageId(firebaseMessageId);

        // then
        assertThat(outbox.getFirebaseMessageId()).isEqualTo(firebaseMessageId);
    }

    @Test
    @DisplayName("userId 전파 — BR-792 AC1: createCancellation() 시 this.userId가 파생 outbox에 전파됨")
    void should_propagate_userId_when_createCancellation_called() {
        // given
        Long userId = 67890L;
        FcmOutbox original = FcmOutbox.create(userId, "token", "채팅방", "내용",
                FcmRoutingType.CHAT_OPEN, 42L, 12345L);

        // when
        FcmOutbox cancellation = original.createCancellation();

        // then
        assertThat(cancellation.getUserId()).isEqualTo(userId);
    }
}
