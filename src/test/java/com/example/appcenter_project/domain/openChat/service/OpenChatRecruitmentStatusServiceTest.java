package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomRecruitmentStatus;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatRecruitmentStatusFixture;
import com.example.appcenter_project.domain.openChat.repository.OpenChatParticipantRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomRepository;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.enums.Role;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.exception.ErrorCode;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/*
 * OpenChatRoomService — 파생 톡방 모집 상태 변경 서비스 TDD Red Phase
 * BR-759: updateRecruitmentStatus(Long actorId, Long roomId, OpenChatRoomRecruitmentStatus status)
 *
 * 구현 에이전트가 추가해야 할 내용:
 * - OpenChatRoomService.updateRecruitmentStatus(Long actorId, Long roomId, OpenChatRoomRecruitmentStatus status)
 * - OpenChatRoom.updateRecruitmentStatus(OpenChatRoomRecruitmentStatus status, Long actorId) 도메인 메서드
 * - OpenChatRoomRecruitmentStatus enum (OPEN, CLOSED)
 * - ErrorCode.OPEN_CHAT_ROOM_ALREADY_OPEN (CONFLICT, 22032)
 */
@ExtendWith(MockitoExtension.class)
class OpenChatRecruitmentStatusServiceTest {

    @Mock
    OpenChatRoomRepository openChatRoomRepository;

    @Mock
    OpenChatParticipantRepository openChatParticipantRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    com.example.appcenter_project.domain.block.service.BlockService blockService;

    @InjectMocks
    OpenChatRoomService openChatRoomService;

    // ============================================================
    // Happy Path — Service
    // ============================================================

    @Test
    @DisplayName("recruitmentClosed=true — AC-01 OPEN → CLOSED 전환 시 updateRecruitmentStatus 호출 후 recruitmentClosed=true")
    void should_set_recruitmentClosed_true_when_owner_transitions_to_closed() {
        // given
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoom(1L);
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        given(openChatRoomRepository.findById(1L)).willReturn(Optional.of(room));
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));

        // when
        openChatRoomService.updateRecruitmentStatus(1L, 1L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        assertThat(room.isRecruitmentClosed()).isTrue();
    }

    @Test
    @DisplayName("recruitmentClosed=false — AC-02 CLOSED → OPEN 재개 시 updateRecruitmentStatus 호출 후 recruitmentClosed=false")
    void should_set_recruitmentClosed_false_when_owner_transitions_to_open() {
        // given
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoomClosed(1L);
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        given(openChatRoomRepository.findById(1L)).willReturn(Optional.of(room));
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));

        // when
        openChatRoomService.updateRecruitmentStatus(1L, 1L, OpenChatRoomRecruitmentStatus.OPEN);

        // then
        assertThat(room.isRecruitmentClosed()).isFalse();
    }

    @Test
    @DisplayName("전환 성공 — AC-06 관리자(ROLE_ADMIN)는 방장 아니어도 CLOSED 전환 가능")
    void should_allow_admin_to_close_room_regardless_of_ownership() {
        // given
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoom(1L);
        User admin = OpenChatRecruitmentStatusFixture.createAdmin();
        setUserRole(admin, Role.ROLE_ADMIN);
        given(openChatRoomRepository.findById(1L)).willReturn(Optional.of(room));
        given(userRepository.findById(99L)).willReturn(Optional.of(admin));

        // when
        ThrowingCallable action = () ->
                openChatRoomService.updateRecruitmentStatus(99L, 1L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        assertThatCode(action).doesNotThrowAnyException();
    }

    // ============================================================
    // Business Rule — Service
    // ============================================================

    @Test
    @DisplayName("CustomException 발생 — BR-759-AC-03 이미 OPEN 상태에서 OPEN 요청 → OPEN_CHAT_ROOM_ALREADY_OPEN")
    void should_throw_CustomException_when_already_open_and_request_open() {
        // given
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoom(1L);
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        given(openChatRoomRepository.findById(1L)).willReturn(Optional.of(room));
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));

        // when
        ThrowingCallable action = () ->
                openChatRoomService.updateRecruitmentStatus(1L, 1L, OpenChatRoomRecruitmentStatus.OPEN);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_ALREADY_OPEN);
    }

    @Test
    @DisplayName("CustomException 발생 — BR-759-AC-04 이미 CLOSED 상태에서 CLOSED 요청 → OPEN_CHAT_ROOM_ALREADY_CLOSED")
    void should_throw_CustomException_when_already_closed_and_request_closed() {
        // given
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoomClosed(1L);
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        given(openChatRoomRepository.findById(1L)).willReturn(Optional.of(room));
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));

        // when
        ThrowingCallable action = () ->
                openChatRoomService.updateRecruitmentStatus(1L, 1L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_ALREADY_CLOSED);
    }

    @Test
    @DisplayName("CustomException 발생 — BR-759-AC-05 방장도 관리자도 아닌 유저 → OPEN_CHAT_ROOM_FORBIDDEN")
    void should_throw_CustomException_when_actor_is_not_owner_nor_admin() {
        // given
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoom(1L);
        User nonOwner = OpenChatRecruitmentStatusFixture.createNonOwnerUser();
        given(openChatRoomRepository.findById(1L)).willReturn(Optional.of(room));
        given(userRepository.findById(2L)).willReturn(Optional.of(nonOwner));

        // when
        ThrowingCallable action = () ->
                openChatRoomService.updateRecruitmentStatus(2L, 1L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN);
    }

    @Test
    @DisplayName("CustomException 발생 — BR-759-AC-08 DERIVED 아닌 방(OPEN 타입) → OPEN_CHAT_ROOM_NOT_DERIVED")
    void should_throw_CustomException_when_room_is_not_derived_type() {
        // given
        OpenChatRoom openTypeRoom = OpenChatRecruitmentStatusFixture.createOpenTypeRoom(1L);
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        given(openChatRoomRepository.findById(1L)).willReturn(Optional.of(openTypeRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));

        // when
        ThrowingCallable action = () ->
                openChatRoomService.updateRecruitmentStatus(1L, 1L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_NOT_DERIVED);
    }

    @Test
    @DisplayName("CustomException 발생 — BR-759-AC-09 존재하지 않는 roomId → OPEN_CHAT_ROOM_NOT_FOUND")
    void should_throw_CustomException_when_room_not_found() {
        // given
        given(openChatRoomRepository.findById(999999L)).willReturn(Optional.empty());

        // when
        ThrowingCallable action = () ->
                openChatRoomService.updateRecruitmentStatus(1L, 999999L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("lastStatusChangedBy/At null 유지 — BR-759-AC-14 기존 closeRecruitment 호출 시 lastStatusChanged* null 공존 검증")
    void should_keep_lastStatusChanged_null_when_close_recruitment_called() {
        // given
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoom(1L);
        room.closeRecruitment(1L);

        // when
        // closeRecruitment 호출 후 lastStatusChangedBy/At은 갱신되지 않아야 한다

        // then
        assertThat(room.getLastStatusChangedBy()).isNull();
        assertThat(room.getLastStatusChangedAt()).isNull();
    }

    // ============================================================
    // Edge Case — Service
    // ============================================================

    @Test
    @DisplayName("recruitmentStatus=CLOSED 및 recruitmentClosed=true 일관성 — AC-15 어떤 경로로 CLOSED가 되든 일관성 유지")
    void should_keep_recruitmentClosed_true_consistent_regardless_of_path() {
        // given
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoom(1L);
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        given(openChatRoomRepository.findById(1L)).willReturn(Optional.of(room));
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));

        // when
        openChatRoomService.updateRecruitmentStatus(1L, 1L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        assertThat(room.isRecruitmentClosed()).isTrue();
    }

    private void setUserRole(User user, Role role) {
        try {
            Class<?> clazz = user.getClass();
            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField("role");
                    field.setAccessible(true);
                    field.set(user, role);
                    return;
                } catch (NoSuchFieldException ignored) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
