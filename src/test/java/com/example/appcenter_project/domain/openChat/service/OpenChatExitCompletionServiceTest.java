package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.openChat.dto.response.ResponseLeaveOpenChatRoomDto;
import com.example.appcenter_project.domain.openChat.entity.OpenChatParticipant;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.KickReason;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatMultiHostFixture;
import com.example.appcenter_project.domain.openChat.repository.OpenChatMessageRepository;
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

import java.util.List;
import java.util.Optional;

import static com.example.appcenter_project.domain.openChat.fixture.OpenChatMultiHostFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

/*
 * BR-659 — 오픈채팅 퇴장 기능 미구현 항목 완성
 *
 * 대상 메서드:
 *   void kickParticipant(Long actorId, Long roomId, Long targetUserId, KickReason reason, Long newHostUserId)
 *   ResponseLeaveOpenChatRoomDto leaveRoom(Long roomId, Long userId, Long newHostUserId)
 *
 * 변경 내용 (기존 테스트와의 차이):
 *   1. kickParticipant 시그니처에 KickReason reason, Long newHostUserId 추가
 *   2. ADMIN이 방장을 강퇴할 때 newHostUserId 처리 로직 추가
 *   3. leaveRoom — 비공식 방에서 단독 방장 자진 퇴장 시 방 하드 삭제
 *
 * [kickParticipant — reason 파라미터]
 * TC-01: 방장이 일반 참여자를 reason=SPAM으로 강퇴 → 204, participant 삭제
 *
 * [kickParticipant — ADMIN이 방장 강퇴, 다른 참여자 있음]
 * TC-02: ADMIN이 방장(A)을 강퇴 + newHostUserId=B → B에게 방장 부여, A 삭제
 * TC-03: ADMIN이 방장(A)을 강퇴 + newHostUserId 없음 → OPEN_CHAT_NEW_HOST_REQUIRED
 * TC-04: ADMIN이 방장(A)을 강퇴 + newHostUserId가 방 미참여자 → OPEN_CHAT_PARTICIPANT_NOT_FOUND
 * TC-05: ADMIN이 방장(A)을 강퇴 + newHostUserId가 이미 방장 → OPEN_CHAT_ALREADY_HOST
 *
 * [kickParticipant — ADMIN이 방장 강퇴, 방장 혼자]
 * TC-06: 방에 방장A만 존재, ADMIN이 강퇴 → participant 삭제 + room 삭제
 *
 * [kickParticipant — 방장이 일반 참여자 강퇴 시 newHostUserId 무시]
 * TC-07: 방장이 일반 참여자 강퇴 시 newHostUserId 전달해도 무시 → participant만 삭제
 *
 * [leaveRoom — 비공식 방 마지막 방장 자진 퇴장]
 * TC-08: 비공식 방에 방장A 혼자 → participant + room 삭제, roomDeleted=true
 * TC-09: 비공식 방에 방장A + 참여자B, newHostUserId=null → OPEN_CHAT_SOLE_HOST_CANNOT_LEAVE (기존 동작 유지)
 *
 * [leaveRoom — 공식 방 마지막 방장 자진 퇴장]
 * TC-10: 공식 방에 방장A 혼자 → OPEN_CHAT_SOLE_HOST_CANNOT_LEAVE (방 유지)
 *
 * [신규 ErrorCode]
 * OPEN_CHAT_NEW_HOST_REQUIRED(BAD_REQUEST, 22018, "[OpenChat] 방장 강퇴 시 새 방장을 지정해야 합니다.")
 */
@ExtendWith(MockitoExtension.class)
class OpenChatExitCompletionServiceTest {

    @Mock
    private OpenChatRoomRepository openChatRoomRepository;

    @Mock
    private OpenChatParticipantRepository openChatParticipantRepository;

    @Mock
    private OpenChatMessageRepository openChatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OpenChatMessageService openChatMessageService;

    @InjectMocks
    private OpenChatRoomService openChatRoomService;

    // ============================================================
    // TC-01: kickParticipant — reason 파라미터 포함 정상 강퇴
    // ============================================================

    @Test
    @DisplayName("강퇴 성공 — TC-01: 방장이 일반 참여자를 reason=SPAM으로 강퇴하면 participant 삭제됨")
    void should_delete_participant_when_host_kicks_with_reason() {
        // given
        OpenChatRoom room = OpenChatMultiHostFixture.createRoom();
        OpenChatParticipant targetParticipant = OpenChatParticipant.create(ROOM_ID, PARTICIPANT_USER_ID, false);
        User hostUser = mock(User.class);
        given(hostUser.getRole()).willReturn(Role.ROLE_USER);
        User targetUser = mock(User.class);
        given(targetUser.getRole()).willReturn(Role.ROLE_USER);

        given(openChatRoomRepository.findByIdWithLock(ROOM_ID)).willReturn(Optional.of(room));
        given(userRepository.findById(HOST_USER_ID)).willReturn(Optional.of(hostUser));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, PARTICIPANT_USER_ID)).willReturn(Optional.of(targetParticipant));
        given(openChatParticipantRepository.existsByRoomIdAndUserIdAndIsHost(ROOM_ID, HOST_USER_ID, true)).willReturn(true);
        given(userRepository.findById(PARTICIPANT_USER_ID)).willReturn(Optional.of(targetUser));

        // when
        openChatRoomService.kickParticipant(HOST_USER_ID, ROOM_ID, PARTICIPANT_USER_ID, KickReason.SPAM, null);

        // then
        then(openChatParticipantRepository).should(times(1)).delete(targetParticipant);
        then(openChatRoomRepository).should(never()).delete(any());
    }

    // ============================================================
    // TC-02: ADMIN이 방장 강퇴 — newHostUserId 유효 → 새 방장 지정 + 대상 삭제
    // ============================================================

    @Test
    @DisplayName("강퇴 성공 — TC-02: ADMIN이 방장(A)을 강퇴하면서 newHostUserId=B 지정 → B에게 방장 부여, A participant 삭제")
    void should_grant_new_host_and_delete_target_when_admin_kicks_host_with_new_host_id() {
        // given
        OpenChatRoom room = OpenChatMultiHostFixture.createRoom();
        OpenChatParticipant hostParticipant = OpenChatParticipant.create(ROOM_ID, HOST_USER_ID, true);
        OpenChatParticipant participantB = OpenChatParticipant.create(ROOM_ID, PARTICIPANT_USER_ID, false);
        List<OpenChatParticipant> allParticipants = List.of(hostParticipant, participantB);
        User adminUser = mock(User.class);
        given(adminUser.getRole()).willReturn(Role.ROLE_ADMIN);
        User targetUser = mock(User.class);
        given(targetUser.getRole()).willReturn(Role.ROLE_USER);

        given(openChatRoomRepository.findByIdWithLock(ROOM_ID)).willReturn(Optional.of(room));
        given(userRepository.findById(ADMIN_USER_ID)).willReturn(Optional.of(adminUser));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, HOST_USER_ID)).willReturn(Optional.of(hostParticipant));
        given(userRepository.findById(HOST_USER_ID)).willReturn(Optional.of(targetUser));
        given(openChatParticipantRepository.findAllByRoomId(ROOM_ID)).willReturn(allParticipants);
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, PARTICIPANT_USER_ID)).willReturn(Optional.of(participantB));

        // when
        openChatRoomService.kickParticipant(ADMIN_USER_ID, ROOM_ID, HOST_USER_ID, KickReason.ABUSE, PARTICIPANT_USER_ID);

        // then
        assertThat(participantB.isHost()).isTrue();
        then(openChatParticipantRepository).should(times(1)).delete(hostParticipant);
        then(openChatRoomRepository).should(never()).delete(any());
    }

    // ============================================================
    // TC-03: ADMIN이 방장 강퇴 — newHostUserId 없음, 다른 참여자 있음 → OPEN_CHAT_NEW_HOST_REQUIRED
    // ============================================================

    @Test
    @DisplayName("CustomException — TC-03: ADMIN이 방장 강퇴 시 다른 참여자 있는데 newHostUserId 없으면 OPEN_CHAT_NEW_HOST_REQUIRED")
    void should_throw_when_admin_kicks_host_without_new_host_id_and_others_exist() {
        // given
        OpenChatRoom room = OpenChatMultiHostFixture.createRoom();
        OpenChatParticipant hostParticipant = OpenChatParticipant.create(ROOM_ID, HOST_USER_ID, true);
        OpenChatParticipant participantB = OpenChatParticipant.create(ROOM_ID, PARTICIPANT_USER_ID, false);
        List<OpenChatParticipant> allParticipants = List.of(hostParticipant, participantB);
        User adminUser = mock(User.class);
        given(adminUser.getRole()).willReturn(Role.ROLE_ADMIN);
        User targetUser = mock(User.class);
        given(targetUser.getRole()).willReturn(Role.ROLE_USER);

        given(openChatRoomRepository.findByIdWithLock(ROOM_ID)).willReturn(Optional.of(room));
        given(userRepository.findById(ADMIN_USER_ID)).willReturn(Optional.of(adminUser));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, HOST_USER_ID)).willReturn(Optional.of(hostParticipant));
        given(userRepository.findById(HOST_USER_ID)).willReturn(Optional.of(targetUser));
        given(openChatParticipantRepository.findAllByRoomId(ROOM_ID)).willReturn(allParticipants);

        // when
        ThrowingCallable action = () -> openChatRoomService.kickParticipant(
            ADMIN_USER_ID, ROOM_ID, HOST_USER_ID, KickReason.ABUSE, null);

        // then
        assertThatThrownBy(action)
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.OPEN_CHAT_NEW_HOST_REQUIRED);
    }

    // ============================================================
    // TC-04: ADMIN이 방장 강퇴 — newHostUserId가 방 미참여자 → OPEN_CHAT_PARTICIPANT_NOT_FOUND
    // ============================================================

    @Test
    @DisplayName("CustomException — TC-04: ADMIN이 방장 강퇴 시 newHostUserId가 방 미참여자면 OPEN_CHAT_PARTICIPANT_NOT_FOUND")
    void should_throw_when_admin_kicks_host_and_new_host_is_not_participant() {
        // given
        OpenChatRoom room = OpenChatMultiHostFixture.createRoom();
        OpenChatParticipant hostParticipant = OpenChatParticipant.create(ROOM_ID, HOST_USER_ID, true);
        OpenChatParticipant participantB = OpenChatParticipant.create(ROOM_ID, PARTICIPANT_USER_ID, false);
        List<OpenChatParticipant> allParticipants = List.of(hostParticipant, participantB);
        User adminUser = mock(User.class);
        given(adminUser.getRole()).willReturn(Role.ROLE_ADMIN);
        User targetUser = mock(User.class);
        given(targetUser.getRole()).willReturn(Role.ROLE_USER);

        given(openChatRoomRepository.findByIdWithLock(ROOM_ID)).willReturn(Optional.of(room));
        given(userRepository.findById(ADMIN_USER_ID)).willReturn(Optional.of(adminUser));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, HOST_USER_ID)).willReturn(Optional.of(hostParticipant));
        given(userRepository.findById(HOST_USER_ID)).willReturn(Optional.of(targetUser));
        given(openChatParticipantRepository.findAllByRoomId(ROOM_ID)).willReturn(allParticipants);
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, NON_PARTICIPANT_USER_ID)).willReturn(Optional.empty());

        // when
        ThrowingCallable action = () -> openChatRoomService.kickParticipant(
            ADMIN_USER_ID, ROOM_ID, HOST_USER_ID, KickReason.ABUSE, NON_PARTICIPANT_USER_ID);

        // then
        assertThatThrownBy(action)
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.OPEN_CHAT_PARTICIPANT_NOT_FOUND);
    }

    // ============================================================
    // TC-05: ADMIN이 방장 강퇴 — newHostUserId가 이미 방장 → OPEN_CHAT_ALREADY_HOST
    // ============================================================

    @Test
    @DisplayName("CustomException — TC-05: ADMIN이 방장 강퇴 시 newHostUserId가 이미 방장이면 OPEN_CHAT_ALREADY_HOST")
    void should_throw_when_admin_kicks_host_and_new_host_is_already_host() {
        // given
        OpenChatRoom room = OpenChatMultiHostFixture.createRoom();
        OpenChatParticipant hostParticipant = OpenChatParticipant.create(ROOM_ID, HOST_USER_ID, true);
        OpenChatParticipant anotherHostParticipant = OpenChatParticipant.create(ROOM_ID, ANOTHER_HOST_USER_ID, true);
        List<OpenChatParticipant> allParticipants = List.of(hostParticipant, anotherHostParticipant);
        User adminUser = mock(User.class);
        given(adminUser.getRole()).willReturn(Role.ROLE_ADMIN);
        User targetUser = mock(User.class);
        given(targetUser.getRole()).willReturn(Role.ROLE_USER);

        given(openChatRoomRepository.findByIdWithLock(ROOM_ID)).willReturn(Optional.of(room));
        given(userRepository.findById(ADMIN_USER_ID)).willReturn(Optional.of(adminUser));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, HOST_USER_ID)).willReturn(Optional.of(hostParticipant));
        given(userRepository.findById(HOST_USER_ID)).willReturn(Optional.of(targetUser));
        given(openChatParticipantRepository.findAllByRoomId(ROOM_ID)).willReturn(allParticipants);
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, ANOTHER_HOST_USER_ID)).willReturn(Optional.of(anotherHostParticipant));

        // when
        ThrowingCallable action = () -> openChatRoomService.kickParticipant(
            ADMIN_USER_ID, ROOM_ID, HOST_USER_ID, KickReason.ABUSE, ANOTHER_HOST_USER_ID);

        // then
        assertThatThrownBy(action)
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.OPEN_CHAT_ALREADY_HOST);
    }

    // ============================================================
    // TC-06: ADMIN이 방장 강퇴 — 방에 방장 혼자 → participant + room 삭제
    // ============================================================

    @Test
    @DisplayName("강퇴 성공 — TC-06: 방에 방장A만 존재할 때 ADMIN이 강퇴하면 participant + room 삭제")
    void should_delete_participant_and_room_when_admin_kicks_sole_host() {
        // given
        OpenChatRoom room = OpenChatMultiHostFixture.createRoom();
        OpenChatParticipant hostParticipant = OpenChatParticipant.create(ROOM_ID, HOST_USER_ID, true);
        List<OpenChatParticipant> allParticipants = List.of(hostParticipant);
        User adminUser = mock(User.class);
        given(adminUser.getRole()).willReturn(Role.ROLE_ADMIN);
        User targetUser = mock(User.class);
        given(targetUser.getRole()).willReturn(Role.ROLE_USER);

        given(openChatRoomRepository.findByIdWithLock(ROOM_ID)).willReturn(Optional.of(room));
        given(userRepository.findById(ADMIN_USER_ID)).willReturn(Optional.of(adminUser));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, HOST_USER_ID)).willReturn(Optional.of(hostParticipant));
        given(userRepository.findById(HOST_USER_ID)).willReturn(Optional.of(targetUser));
        given(openChatParticipantRepository.findAllByRoomId(ROOM_ID)).willReturn(allParticipants);

        // when
        openChatRoomService.kickParticipant(ADMIN_USER_ID, ROOM_ID, HOST_USER_ID, KickReason.OTHER, null);

        // then
        then(openChatParticipantRepository).should(times(1)).delete(hostParticipant);
        then(openChatRoomRepository).should(times(1)).delete(room);
    }

    // ============================================================
    // TC-07: 방장이 일반 참여자 강퇴 시 newHostUserId 무시
    // ============================================================

    @Test
    @DisplayName("강퇴 성공 — TC-07: 방장이 일반 참여자 강퇴 시 newHostUserId 전달해도 participant만 삭제, 방 유지")
    void should_ignore_new_host_user_id_when_host_kicks_normal_participant() {
        // given
        OpenChatRoom room = OpenChatMultiHostFixture.createRoom();
        OpenChatParticipant targetParticipant = OpenChatParticipant.create(ROOM_ID, PARTICIPANT_USER_ID, false);
        User hostUser = mock(User.class);
        given(hostUser.getRole()).willReturn(Role.ROLE_USER);
        User targetUser = mock(User.class);
        given(targetUser.getRole()).willReturn(Role.ROLE_USER);

        given(openChatRoomRepository.findByIdWithLock(ROOM_ID)).willReturn(Optional.of(room));
        given(userRepository.findById(HOST_USER_ID)).willReturn(Optional.of(hostUser));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, PARTICIPANT_USER_ID)).willReturn(Optional.of(targetParticipant));
        given(openChatParticipantRepository.existsByRoomIdAndUserIdAndIsHost(ROOM_ID, HOST_USER_ID, true)).willReturn(true);
        given(userRepository.findById(PARTICIPANT_USER_ID)).willReturn(Optional.of(targetUser));

        // when
        openChatRoomService.kickParticipant(HOST_USER_ID, ROOM_ID, PARTICIPANT_USER_ID, KickReason.SPAM, ANOTHER_HOST_USER_ID);

        // then
        then(openChatParticipantRepository).should(times(1)).delete(targetParticipant);
        then(openChatRoomRepository).should(never()).delete(any());
    }

    // ============================================================
    // TC-08: leaveRoom — 비공식 방 단독 방장 자진 퇴장 → 방 하드 삭제
    // ============================================================

    @Test
    @DisplayName("퇴장 성공 — TC-08: 비공식 방에 방장A 혼자일 때 자진 퇴장하면 participant + room 삭제, roomDeleted=true")
    void should_delete_participant_and_room_when_sole_host_leaves_unofficial_room() {
        // given
        OpenChatRoom unofficialRoom = OpenChatMultiHostFixture.createRoom();
        OpenChatParticipant soleHost = OpenChatParticipant.create(ROOM_ID, HOST_USER_ID, true);
        List<OpenChatParticipant> lockedList = List.of(soleHost);

        given(openChatParticipantRepository.findAllByRoomIdWithLock(ROOM_ID)).willReturn(lockedList);
        given(openChatRoomRepository.findByIdWithLock(ROOM_ID)).willReturn(Optional.of(unofficialRoom));

        // when
        ResponseLeaveOpenChatRoomDto result = openChatRoomService.leaveRoom(ROOM_ID, HOST_USER_ID, null);

        // then
        assertThat(result.isRoomDeleted()).isTrue();
        then(openChatParticipantRepository).should(times(1)).deleteAll(lockedList);
        then(openChatRoomRepository).should(times(1)).delete(unofficialRoom);
    }

    // ============================================================
    // TC-09: leaveRoom — 비공식 방 방장 + 다른 참여자 있음, newHostUserId=null → 기존 예외 유지
    // ============================================================

    @Test
    @DisplayName("CustomException — TC-09: 비공식 방에 방장A + 참여자B인데 newHostUserId=null로 자진 퇴장 시도 → OPEN_CHAT_SOLE_HOST_CANNOT_LEAVE")
    void should_throw_when_sole_host_leaves_unofficial_room_with_other_participants() {
        // given
        OpenChatParticipant soleHost = OpenChatParticipant.create(ROOM_ID, HOST_USER_ID, true);
        OpenChatParticipant participantB = OpenChatParticipant.create(ROOM_ID, PARTICIPANT_USER_ID, false);
        List<OpenChatParticipant> lockedList = List.of(soleHost, participantB);

        given(openChatParticipantRepository.findAllByRoomIdWithLock(ROOM_ID)).willReturn(lockedList);

        // when
        ThrowingCallable action = () -> openChatRoomService.leaveRoom(ROOM_ID, HOST_USER_ID, null);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_SOLE_HOST_CANNOT_LEAVE);
    }

    // ============================================================
    // TC-10: leaveRoom — 공식 방 단독 방장 자진 퇴장 → OPEN_CHAT_SOLE_HOST_CANNOT_LEAVE
    // ============================================================

    @Test
    @DisplayName("CustomException — TC-10: 공식 방에 방장A 혼자일 때 자진 퇴장 시도 → OPEN_CHAT_SOLE_HOST_CANNOT_LEAVE (방 유지)")
    void should_throw_when_sole_host_leaves_official_room() {
        // given
        OpenChatRoom officialRoom = OpenChatMultiHostFixture.createOfficialRoom();
        OpenChatParticipant soleHost = OpenChatParticipant.create(ROOM_ID, ADMIN_USER_ID, true);
        List<OpenChatParticipant> lockedList = List.of(soleHost);

        given(openChatParticipantRepository.findAllByRoomIdWithLock(ROOM_ID)).willReturn(lockedList);
        given(openChatRoomRepository.findByIdWithLock(ROOM_ID)).willReturn(Optional.of(officialRoom));

        // when
        ThrowingCallable action = () -> openChatRoomService.leaveRoom(ROOM_ID, ADMIN_USER_ID, null);

        // then
        assertThatThrownBy(action)
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.OPEN_CHAT_SOLE_HOST_CANNOT_LEAVE);
        then(openChatRoomRepository).should(never()).delete(any());
    }
}
