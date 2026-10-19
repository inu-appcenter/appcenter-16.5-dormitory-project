package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomRecruitmentStatus;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatRecruitmentStatusFixture;
import com.example.appcenter_project.domain.openChat.repository.OpenChatMessageRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatParticipantRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomRepository;
import com.example.appcenter_project.domain.user.entity.User;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OpenChatRoomRecruitmentStatusServiceTest {

    @Mock
    OpenChatRoomRepository openChatRoomRepository;

    @Mock
    OpenChatParticipantRepository openChatParticipantRepository;

    @Mock
    OpenChatMessageRepository openChatMessageRepository;

    @Mock
    OpenChatMessageService openChatMessageService;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    OpenChatRoomService openChatRoomService;

    @Test
    @DisplayName("204 성공 — BR-759 AC-01: 방장이 OPEN 파생 방을 CLOSED로 전환")
    void should_succeed_when_owner_changes_OPEN_to_CLOSED() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoom(owner.getId());
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(owner.getId())).willReturn(Optional.of(owner));

        // when
        ThrowingCallable action = () -> openChatRoomService.updateRecruitmentStatus(
                owner.getId(), 10L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        assertThatCode(action).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("recruitmentClosed=true 저장 — BR-759 AC-01: OPEN→CLOSED 전환 후 엔티티 상태")
    void should_set_recruitmentClosed_true_when_owner_changes_OPEN_to_CLOSED() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoom(owner.getId());
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(owner.getId())).willReturn(Optional.of(owner));

        // when
        openChatRoomService.updateRecruitmentStatus(
                owner.getId(), 10L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        assertThat(room.isRecruitmentClosed()).isTrue();
    }

    @Test
    @DisplayName("lastStatusChangedBy 세팅 — BR-759 AC-01: OPEN→CLOSED 전환 후 lastStatusChangedBy=actorId")
    void should_set_lastStatusChangedBy_when_owner_changes_OPEN_to_CLOSED() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoom(owner.getId());
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(owner.getId())).willReturn(Optional.of(owner));

        // when
        openChatRoomService.updateRecruitmentStatus(
                owner.getId(), 10L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        assertThat(room.getLastStatusChangedBy()).isEqualTo(owner.getId());
    }

    @Test
    @DisplayName("lastStatusChangedAt 세팅 — BR-759 AC-01: OPEN→CLOSED 전환 후 lastStatusChangedAt != null")
    void should_set_lastStatusChangedAt_when_owner_changes_OPEN_to_CLOSED() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoom(owner.getId());
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(owner.getId())).willReturn(Optional.of(owner));

        // when
        openChatRoomService.updateRecruitmentStatus(
                owner.getId(), 10L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        assertThat(room.getLastStatusChangedAt()).isNotNull();
    }

    @Test
    @DisplayName("204 성공 — BR-759 AC-02: 방장이 CLOSED 파생 방을 OPEN으로 재개")
    void should_succeed_when_owner_changes_CLOSED_to_OPEN() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoomClosed(owner.getId());
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(owner.getId())).willReturn(Optional.of(owner));

        // when
        ThrowingCallable action = () -> openChatRoomService.updateRecruitmentStatus(
                owner.getId(), 10L, OpenChatRoomRecruitmentStatus.OPEN);

        // then
        assertThatCode(action).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("recruitmentClosed=false 저장 — BR-759 AC-02: CLOSED→OPEN 재개 후 엔티티 상태")
    void should_set_recruitmentClosed_false_when_owner_changes_CLOSED_to_OPEN() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoomClosed(owner.getId());
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(owner.getId())).willReturn(Optional.of(owner));

        // when
        openChatRoomService.updateRecruitmentStatus(
                owner.getId(), 10L, OpenChatRoomRecruitmentStatus.OPEN);

        // then
        assertThat(room.isRecruitmentClosed()).isFalse();
    }

    @Test
    @DisplayName("closedAt=null 리셋 — BR-759 AC-02: CLOSED→OPEN 재개 후 closedAt 초기화")
    void should_reset_closedAt_to_null_when_owner_changes_CLOSED_to_OPEN() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoomClosed(owner.getId());
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(owner.getId())).willReturn(Optional.of(owner));

        // when
        openChatRoomService.updateRecruitmentStatus(
                owner.getId(), 10L, OpenChatRoomRecruitmentStatus.OPEN);

        // then
        assertThat(room.getClosedAt()).isNull();
    }

    @Test
    @DisplayName("closedBy=null 리셋 — BR-759 AC-02: CLOSED→OPEN 재개 후 closedBy 초기화")
    void should_reset_closedBy_to_null_when_owner_changes_CLOSED_to_OPEN() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoomClosed(owner.getId());
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(owner.getId())).willReturn(Optional.of(owner));

        // when
        openChatRoomService.updateRecruitmentStatus(
                owner.getId(), 10L, OpenChatRoomRecruitmentStatus.OPEN);

        // then
        assertThat(room.getClosedBy()).isNull();
    }

    @Test
    @DisplayName("lastStatusChangedBy 세팅 — BR-759 AC-02: CLOSED→OPEN 재개 후 lastStatusChangedBy=actorId")
    void should_set_lastStatusChangedBy_when_owner_changes_CLOSED_to_OPEN() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoomClosed(owner.getId());
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(owner.getId())).willReturn(Optional.of(owner));

        // when
        openChatRoomService.updateRecruitmentStatus(
                owner.getId(), 10L, OpenChatRoomRecruitmentStatus.OPEN);

        // then
        assertThat(room.getLastStatusChangedBy()).isEqualTo(owner.getId());
    }

    @Test
    @DisplayName("409 ALREADY_OPEN — BR-759 AC-03: 이미 OPEN인 방에 OPEN 요청")
    void should_throw_ALREADY_OPEN_when_room_is_already_OPEN() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoom(owner.getId());
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(owner.getId())).willReturn(Optional.of(owner));

        // when
        ThrowingCallable action = () -> openChatRoomService.updateRecruitmentStatus(
                owner.getId(), 10L, OpenChatRoomRecruitmentStatus.OPEN);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_ALREADY_OPEN);
    }

    @Test
    @DisplayName("lastStatusChanged* 미변경 — BR-759 AC-03: 이미 OPEN인 방에 OPEN 요청 시 상태 불변")
    void should_not_change_lastStatusChangedAt_when_room_is_already_OPEN() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoom(owner.getId());
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(owner.getId())).willReturn(Optional.of(owner));

        // when
        try {
            openChatRoomService.updateRecruitmentStatus(
                    owner.getId(), 10L, OpenChatRoomRecruitmentStatus.OPEN);
        } catch (CustomException ignored) {
        }

        // then
        assertThat(room.getLastStatusChangedAt()).isNull();
    }

    @Test
    @DisplayName("409 ALREADY_CLOSED — BR-759 AC-04: 이미 CLOSED인 방에 CLOSED 요청")
    void should_throw_ALREADY_CLOSED_when_room_is_already_CLOSED() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoomClosed(owner.getId());
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(owner.getId())).willReturn(Optional.of(owner));

        // when
        ThrowingCallable action = () -> openChatRoomService.updateRecruitmentStatus(
                owner.getId(), 10L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_ALREADY_CLOSED);
    }

    @Test
    @DisplayName("403 FORBIDDEN — BR-759 AC-05: 방장도 관리자도 아닌 참여자가 상태 변경 시도")
    void should_throw_FORBIDDEN_when_non_owner_non_admin_requests() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoom(owner.getId());
        User nonOwner = OpenChatRecruitmentStatusFixture.createNonOwnerUser();
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(nonOwner.getId())).willReturn(Optional.of(nonOwner));

        // when
        ThrowingCallable action = () -> openChatRoomService.updateRecruitmentStatus(
                nonOwner.getId(), 10L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN);
    }

    @Test
    @DisplayName("204 성공 — BR-759 AC-06: 관리자가 임의의 DERIVED 방을 CLOSED로 전환")
    void should_succeed_when_admin_changes_any_derived_room_to_CLOSED() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoom(owner.getId());
        User admin = OpenChatRecruitmentStatusFixture.createAdmin();
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(admin.getId())).willReturn(Optional.of(admin));

        // when
        ThrowingCallable action = () -> openChatRoomService.updateRecruitmentStatus(
                admin.getId(), 10L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        assertThatCode(action).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("closedBy=adminId 세팅 — BR-759 AC-06: 관리자가 CLOSED 전환 시 closedBy=관리자 ID")
    void should_set_closedBy_to_adminId_when_admin_closes_room() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoom(owner.getId());
        User admin = OpenChatRecruitmentStatusFixture.createAdmin();
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(admin.getId())).willReturn(Optional.of(admin));

        // when
        openChatRoomService.updateRecruitmentStatus(
                admin.getId(), 10L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        assertThat(room.getClosedBy()).isEqualTo(admin.getId());
    }

    @Test
    @DisplayName("lastStatusChangedBy=adminId 세팅 — BR-759 AC-06: 관리자가 CLOSED 전환 시 lastStatusChangedBy=관리자 ID")
    void should_set_lastStatusChangedBy_to_adminId_when_admin_closes_room() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoom(owner.getId());
        User admin = OpenChatRecruitmentStatusFixture.createAdmin();
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(admin.getId())).willReturn(Optional.of(admin));

        // when
        openChatRoomService.updateRecruitmentStatus(
                admin.getId(), 10L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        assertThat(room.getLastStatusChangedBy()).isEqualTo(admin.getId());
    }

    @Test
    @DisplayName("204 성공 — BR-759 AC-07: 관리자가 CLOSED 파생 방을 OPEN으로 재개")
    void should_succeed_when_admin_changes_CLOSED_derived_room_to_OPEN() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoomClosed(owner.getId());
        User admin = OpenChatRecruitmentStatusFixture.createAdmin();
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(admin.getId())).willReturn(Optional.of(admin));

        // when
        ThrowingCallable action = () -> openChatRoomService.updateRecruitmentStatus(
                admin.getId(), 10L, OpenChatRoomRecruitmentStatus.OPEN);

        // then
        assertThatCode(action).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("lastStatusChangedBy=adminId 세팅 — BR-759 AC-07: 관리자가 OPEN 재개 시 lastStatusChangedBy=관리자 ID")
    void should_set_lastStatusChangedBy_to_adminId_when_admin_reopens_room() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoomClosed(owner.getId());
        User admin = OpenChatRecruitmentStatusFixture.createAdmin();
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(admin.getId())).willReturn(Optional.of(admin));

        // when
        openChatRoomService.updateRecruitmentStatus(
                admin.getId(), 10L, OpenChatRoomRecruitmentStatus.OPEN);

        // then
        assertThat(room.getLastStatusChangedBy()).isEqualTo(admin.getId());
    }

    // ── BR-782: OPEN 타입 방 모집 상태 변경 허용 ──────────────────────────────

    @Test
    @DisplayName("204 성공 — BR-782 AC-1: OPEN 타입 비공식 방의 방장이 CLOSED 요청")
    void should_succeed_when_owner_closes_OPEN_type_room() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createOpenTypeRoom(owner.getId());
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(owner.getId())).willReturn(Optional.of(owner));

        // when
        ThrowingCallable action = () -> openChatRoomService.updateRecruitmentStatus(
                owner.getId(), 10L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        assertThatCode(action).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("recruitmentClosed=true — BR-782 AC-1: OPEN 타입 방 CLOSED 전환 후 엔티티 상태")
    void should_set_recruitmentClosed_true_when_owner_closes_OPEN_type_room() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createOpenTypeRoom(owner.getId());
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(owner.getId())).willReturn(Optional.of(owner));

        // when
        openChatRoomService.updateRecruitmentStatus(
                owner.getId(), 10L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        assertThat(room.isRecruitmentClosed()).isTrue();
    }

    @Test
    @DisplayName("204 성공 — BR-782 AC-2: OPEN 타입 공식방 방장이 CLOSED 요청")
    void should_succeed_when_owner_closes_OFFICIAL_OPEN_type_room() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createOpenTypeOfficialRoom(owner.getId());
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(owner.getId())).willReturn(Optional.of(owner));

        // when
        ThrowingCallable action = () -> openChatRoomService.updateRecruitmentStatus(
                owner.getId(), 10L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        assertThatCode(action).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("204 성공 — BR-782 AC-3: ADMIN이 CLOSED 상태 OPEN 타입 방을 OPEN으로 재개")
    void should_succeed_when_admin_reopens_CLOSED_OPEN_type_room() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createOpenTypeRoomClosed(owner.getId());
        User admin = OpenChatRecruitmentStatusFixture.createAdmin();
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(admin.getId())).willReturn(Optional.of(admin));

        // when
        ThrowingCallable action = () -> openChatRoomService.updateRecruitmentStatus(
                admin.getId(), 10L, OpenChatRoomRecruitmentStatus.OPEN);

        // then
        assertThatCode(action).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("400 NOT_ALLOWED_TYPE — BR-782 AC-5: PERSONAL 타입 방에 방장이 CLOSED 요청")
    void should_throw_NOT_ALLOWED_TYPE_when_PERSONAL_type_room_requested() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createPersonalTypeRoom(owner.getId());
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(owner.getId())).willReturn(Optional.of(owner));

        // when
        ThrowingCallable action = () -> openChatRoomService.updateRecruitmentStatus(
                owner.getId(), 10L, OpenChatRoomRecruitmentStatus.CLOSED);

        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_NOT_ALLOWED_TYPE);
    }

    @Test
    @DisplayName("404 NOT_FOUND — BR-759 AC-09: 존재하지 않는 roomId로 상태 변경 시도")
    void should_throw_NOT_FOUND_when_room_does_not_exist() {
        // given
        given(openChatRoomRepository.findById(999999L)).willReturn(Optional.empty());

        // when
        ThrowingCallable action = () -> openChatRoomService.updateRecruitmentStatus(
                1L, 999999L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("404 USER_NOT_FOUND — BR-759: 존재하지 않는 actorId로 상태 변경 시도")
    void should_throw_USER_NOT_FOUND_when_actor_does_not_exist() {
        // given
        User owner = OpenChatRecruitmentStatusFixture.createOwner();
        OpenChatRoom room = OpenChatRecruitmentStatusFixture.createDerivedRoom(owner.getId());
        given(openChatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when
        ThrowingCallable action = () -> openChatRoomService.updateRecruitmentStatus(
                999L, 10L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
