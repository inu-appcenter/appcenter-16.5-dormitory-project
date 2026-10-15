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
}
