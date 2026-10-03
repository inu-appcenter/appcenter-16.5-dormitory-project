package com.example.appcenter_project.domain.openChat.entity;

import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomRecruitmentStatus;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatRecruitmentStatusFixture;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/*
 * OpenChatRoom 도메인 메서드 — updateRecruitmentStatus TDD Red Phase
 * BR-759: 파생 톡방 모집 상태 변경 도메인 로직
 *
 * 구현 에이전트가 추가해야 할 내용:
 * - OpenChatRoom.updateRecruitmentStatus(OpenChatRoomRecruitmentStatus status, Long actorId)
 * - OpenChatRoom 필드: lastStatusChangedBy(Long), lastStatusChangedAt(LocalDateTime)
 * - OpenChatRoomRecruitmentStatus enum (OPEN, CLOSED)
 * - ErrorCode.OPEN_CHAT_ROOM_ALREADY_OPEN (CONFLICT, 22032)
 */
class OpenChatRoomRecruitmentStatusTest {

    // ============================================================
    // Business Rule — Domain
    // ============================================================

    @Test
    @DisplayName("lastStatusChangedBy/At 세팅 — DERIVED 방 OPEN→CLOSED 전환 시 lastStatusChanged* 갱신")
    void should_set_lastStatusChanged_when_derived_room_transitions_to_closed() {
        // given
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoom(1L);

        // when
        room.updateRecruitmentStatus(OpenChatRoomRecruitmentStatus.CLOSED, 1L);

        // then
        assertThat(room.getLastStatusChangedBy()).isEqualTo(1L);
    }

    @Test
    @DisplayName("lastStatusChangedAt null 리셋 + lastStatusChanged* 세팅 — DERIVED 방 CLOSED→OPEN 전환 시 closedAt/closedBy null, lastStatusChanged* 갱신")
    void should_reset_closedAt_and_closedBy_and_set_lastStatusChanged_when_transitions_to_open() {
        // given
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoomClosed(1L);

        // when
        room.updateRecruitmentStatus(OpenChatRoomRecruitmentStatus.OPEN, 1L);

        // then
        assertThat(room.getClosedAt()).isNull();
    }

    @Test
    @DisplayName("closedBy null 리셋 — DERIVED 방 CLOSED→OPEN 전환 시 closedBy null")
    void should_reset_closedBy_when_transitions_to_open() {
        // given
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoomClosed(1L);

        // when
        room.updateRecruitmentStatus(OpenChatRoomRecruitmentStatus.OPEN, 1L);

        // then
        assertThat(room.getClosedBy()).isNull();
    }

    @Test
    @DisplayName("CustomException 발생 — DERIVED 아닌 방(OPEN 타입)에서 updateRecruitmentStatus 호출 → OPEN_CHAT_ROOM_NOT_DERIVED")
    void should_throw_CustomException_when_room_is_not_derived() {
        // given
        OpenChatRoom openTypeRoom = OpenChatRecruitmentStatusFixture.createOpenTypeRoom(1L);

        // when
        org.assertj.core.api.ThrowableAssert.ThrowingCallable action =
                () -> openTypeRoom.updateRecruitmentStatus(OpenChatRoomRecruitmentStatus.CLOSED, 1L);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_NOT_DERIVED);
    }

    @Test
    @DisplayName("CustomException 발생 — 이미 OPEN 상태에서 OPEN 전환 → OPEN_CHAT_ROOM_ALREADY_OPEN")
    void should_throw_CustomException_when_already_open_and_transition_to_open() {
        // given
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoom(1L);

        // when
        org.assertj.core.api.ThrowableAssert.ThrowingCallable action =
                () -> room.updateRecruitmentStatus(OpenChatRoomRecruitmentStatus.OPEN, 1L);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_ALREADY_OPEN);
    }

    @Test
    @DisplayName("CustomException 발생 — 이미 CLOSED 상태에서 CLOSED 전환 → OPEN_CHAT_ROOM_ALREADY_CLOSED")
    void should_throw_CustomException_when_already_closed_and_transition_to_closed() {
        // given
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoomClosed(1L);

        // when
        org.assertj.core.api.ThrowableAssert.ThrowingCallable action =
                () -> room.updateRecruitmentStatus(OpenChatRoomRecruitmentStatus.CLOSED, 1L);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_ALREADY_CLOSED);
    }
}
