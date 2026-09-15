package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.openChat.entity.OpenChatParticipant;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatMultiHostFixture;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.appcenter_project.domain.openChat.fixture.OpenChatMultiHostFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OpenChatMultiHostServiceTest {

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

    @Test
    @DisplayName("방장 부여 성공 — 방장이 참여자에게 방장 부여 후 isHost=true")
    void should_grant_host_when_requester_is_host() {
        OpenChatRoom room = OpenChatMultiHostFixture.createRoom();
        OpenChatParticipant target = createParticipant();
        User requester = mockUser(HOST_USER_ID);

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(userRepository.findById(HOST_USER_ID)).willReturn(Optional.of(requester));
        given(openChatParticipantRepository.existsByRoomIdAndUserIdAndIsHost(ROOM_ID, HOST_USER_ID, true)).willReturn(true);
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, PARTICIPANT_USER_ID)).willReturn(Optional.of(target));

        openChatRoomService.grantHost(ROOM_ID, HOST_USER_ID, PARTICIPANT_USER_ID);

        assertThat(target.isHost()).isTrue();
    }

    @Test
    @DisplayName("방장 부여 — 비ADMIN 미참여자는 방장 부여 불가 (BR-10)")
    void should_throw_when_non_participant_non_host_grants_host() {
        OpenChatRoom room = OpenChatMultiHostFixture.createRoom();
        User nonParticipant = mockUser(NON_PARTICIPANT_USER_ID);

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(userRepository.findById(NON_PARTICIPANT_USER_ID)).willReturn(Optional.of(nonParticipant));
        given(openChatParticipantRepository.existsByRoomIdAndUserIdAndIsHost(ROOM_ID, NON_PARTICIPANT_USER_ID, true)).willReturn(false);

        ThrowingCallable action = () -> openChatRoomService.grantHost(ROOM_ID, NON_PARTICIPANT_USER_ID, PARTICIPANT_USER_ID);
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN);
    }

    @Test
    @DisplayName("방장 부여 성공 — grantHost 이후 target.isHost() = true (dirty checking)")
    void should_keep_host_grant_without_explicit_save() {
        OpenChatRoom room = OpenChatMultiHostFixture.createRoom();
        OpenChatParticipant target = createParticipant();
        User requester = mockUser(HOST_USER_ID);

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(userRepository.findById(HOST_USER_ID)).willReturn(Optional.of(requester));
        given(openChatParticipantRepository.existsByRoomIdAndUserIdAndIsHost(ROOM_ID, HOST_USER_ID, true)).willReturn(true);
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, PARTICIPANT_USER_ID)).willReturn(Optional.of(target));

        openChatRoomService.grantHost(ROOM_ID, HOST_USER_ID, PARTICIPANT_USER_ID);

        assertThat(target.isHost()).isTrue();
        then(openChatParticipantRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("방장 부여 성공 — 파생 방(DERIVED)에서도 방장 부여 정상 처리 (BR-12)")
    void should_grant_host_in_derived_room() {
        OpenChatRoom derivedRoom = OpenChatRoom.createDerived("파생방", "설명", 50, HOST_USER_ID, null, true, null);
        OpenChatParticipant target = createParticipant();
        User requester = mockUser(HOST_USER_ID);

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(derivedRoom));
        given(userRepository.findById(HOST_USER_ID)).willReturn(Optional.of(requester));
        given(openChatParticipantRepository.existsByRoomIdAndUserIdAndIsHost(ROOM_ID, HOST_USER_ID, true)).willReturn(true);
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, PARTICIPANT_USER_ID)).willReturn(Optional.of(target));

        openChatRoomService.grantHost(ROOM_ID, HOST_USER_ID, PARTICIPANT_USER_ID);

        assertThat(target.isHost()).isTrue();
    }

    @Test
    @DisplayName("CustomException 발생 — 비방장이 방장 부여 (BR-03: OPEN_CHAT_ROOM_FORBIDDEN)")
    void should_throw_CustomException_when_requester_is_not_host_nor_admin() {
        User nonHost = mockUser(PARTICIPANT_USER_ID);

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(createRoom()));
        given(userRepository.findById(PARTICIPANT_USER_ID)).willReturn(Optional.of(nonHost));
        given(openChatParticipantRepository.existsByRoomIdAndUserIdAndIsHost(ROOM_ID, PARTICIPANT_USER_ID, true)).willReturn(false);

        ThrowingCallable action = () -> openChatRoomService.grantHost(ROOM_ID, PARTICIPANT_USER_ID, HOST_USER_ID);
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN);
    }

    @Test
    @DisplayName("CustomException 발생 — 이미 방장인 참여자에게 방장 부여 (OPEN_CHAT_ALREADY_HOST)")
    void should_throw_CustomException_when_target_is_already_host() {
        User requester = mockUser(HOST_USER_ID);

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(createRoom()));
        given(userRepository.findById(HOST_USER_ID)).willReturn(Optional.of(requester));
        given(openChatParticipantRepository.existsByRoomIdAndUserIdAndIsHost(ROOM_ID, HOST_USER_ID, true)).willReturn(true);
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, ANOTHER_HOST_USER_ID))
                .willReturn(Optional.of(createAnotherHostParticipant()));

        ThrowingCallable action = () -> openChatRoomService.grantHost(ROOM_ID, HOST_USER_ID, ANOTHER_HOST_USER_ID);
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ALREADY_HOST);
    }

    @Test
    @DisplayName("CustomException 발생 — 자기 자신을 방장 부여 대상으로 지정 (OPEN_CHAT_ALREADY_HOST)")
    void should_throw_CustomException_when_target_is_self() {
        OpenChatParticipant hostParticipant = createHostParticipant();
        User requester = mockUser(HOST_USER_ID);

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(createRoom()));
        given(userRepository.findById(HOST_USER_ID)).willReturn(Optional.of(requester));
        given(openChatParticipantRepository.existsByRoomIdAndUserIdAndIsHost(ROOM_ID, HOST_USER_ID, true)).willReturn(true);
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, HOST_USER_ID)).willReturn(Optional.of(hostParticipant));

        ThrowingCallable action = () -> openChatRoomService.grantHost(ROOM_ID, HOST_USER_ID, HOST_USER_ID);
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ALREADY_HOST);
    }

    @Test
    @DisplayName("CustomException 발생 — 비참여자를 방장 부여 대상으로 지정 (OPEN_CHAT_PARTICIPANT_NOT_FOUND)")
    void should_throw_CustomException_when_target_is_not_participant() {
        User requester = mockUser(HOST_USER_ID);

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(createRoom()));
        given(userRepository.findById(HOST_USER_ID)).willReturn(Optional.of(requester));
        given(openChatParticipantRepository.existsByRoomIdAndUserIdAndIsHost(ROOM_ID, HOST_USER_ID, true)).willReturn(true);
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, NON_PARTICIPANT_USER_ID)).willReturn(Optional.empty());

        ThrowingCallable action = () -> openChatRoomService.grantHost(ROOM_ID, HOST_USER_ID, NON_PARTICIPANT_USER_ID);
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_PARTICIPANT_NOT_FOUND);
    }

    @Test
    @DisplayName("CustomException 발생 — 존재하지 않는 방에 방장 부여 (OPEN_CHAT_ROOM_NOT_FOUND)")
    void should_throw_CustomException_when_room_not_found_on_grant_host() {
        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

        ThrowingCallable action = () -> openChatRoomService.grantHost(ROOM_ID, HOST_USER_ID, PARTICIPANT_USER_ID);
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("나가기 성공 — 일반 참여자 나가기 후 participant row 삭제")
    void should_delete_participant_when_general_participant_leaves() {
        OpenChatParticipant host = createHostParticipant();
        OpenChatParticipant participant = createParticipant();
        List<OpenChatParticipant> lockedList = List.of(host, participant);

        given(openChatParticipantRepository.findAllByRoomIdWithLock(ROOM_ID)).willReturn(lockedList);

        assertThatCode(() -> openChatRoomService.leaveRoom(ROOM_ID, PARTICIPANT_USER_ID, null)).doesNotThrowAnyException();
        then(openChatParticipantRepository).should().delete(participant);
        then(openChatParticipantRepository).should(never()).delete(host);
    }

    @Test
    @DisplayName("나가기 성공 — 복수 방장 중 1명 나가기 후 해당 row만 삭제, 나머지 방장 유지")
    void should_delete_only_leaving_host_when_multiple_hosts_exist() {
        OpenChatParticipant leavingHost = createHostParticipant();
        OpenChatParticipant anotherHost = createAnotherHostParticipant();
        OpenChatParticipant participant = createParticipant();
        List<OpenChatParticipant> lockedList = List.of(leavingHost, anotherHost, participant);

        given(openChatParticipantRepository.findAllByRoomIdWithLock(ROOM_ID)).willReturn(lockedList);

        openChatRoomService.leaveRoom(ROOM_ID, HOST_USER_ID, null);

        then(openChatParticipantRepository).should().delete(leavingHost);
        then(openChatParticipantRepository).should(never()).delete(anotherHost);
    }

    @Test
    @DisplayName("나가기 성공 — 단독 방장 위임+나가기: target isHost=true, 요청자 row 삭제 (BR-06)")
    void should_delegate_and_leave_atomically_when_sole_host_provides_new_host() {
        OpenChatParticipant soleHost = createHostParticipant();
        OpenChatParticipant newHostTarget = createParticipant();
        List<OpenChatParticipant> lockedList = List.of(soleHost, newHostTarget);

        given(openChatParticipantRepository.findAllByRoomIdWithLock(ROOM_ID)).willReturn(lockedList);

        openChatRoomService.leaveRoom(ROOM_ID, HOST_USER_ID, PARTICIPANT_USER_ID);

        assertThat(newHostTarget.isHost()).isTrue();
        then(openChatParticipantRepository).should().delete(soleHost);
        then(openChatParticipantRepository).should(never()).delete(newHostTarget);
    }

    @Test
    @DisplayName("나가기 성공 — 방장 퇴장 후 재입장 시 새 participant row isHost=false (BR-08)")
    void should_create_participant_with_isHost_false_when_host_rejoins() {
        OpenChatParticipant rejoinedParticipant = OpenChatParticipant.create(ROOM_ID, HOST_USER_ID, LocalDateTime.now());
        assertThat(rejoinedParticipant.isHost()).isFalse();
    }

    @Test
    @DisplayName("나가기 성공 — 공식 방 생성 시 isHost 필드 설정 패턴 검증 (BR-09)")
    void should_set_isHost_true_when_participant_created_as_host() {
        OpenChatParticipant hostParticipant = createHostParticipant();
        assertThat(hostParticipant.isHost()).isTrue();
    }

    @Test
    @DisplayName("나가기 성공 — BR-05 복수 방장 중 1명이 newHostUserId 없이 정상 퇴장")
    void should_leave_successfully_when_multiple_hosts_and_no_new_host_provided() {
        OpenChatParticipant leavingHost = createHostParticipant();
        OpenChatParticipant anotherHost = createAnotherHostParticipant();
        List<OpenChatParticipant> lockedList = List.of(leavingHost, anotherHost, createParticipant());

        given(openChatParticipantRepository.findAllByRoomIdWithLock(ROOM_ID)).willReturn(lockedList);

        assertThatCode(() -> openChatRoomService.leaveRoom(ROOM_ID, HOST_USER_ID, null)).doesNotThrowAnyException();
        then(openChatParticipantRepository).should().delete(leavingHost);
    }

    @Test
    @DisplayName("CustomException 발생 — 단독 방장이 newHostUserId 없이 나가기 (OPEN_CHAT_SOLE_HOST_CANNOT_LEAVE)")
    void should_throw_CustomException_when_sole_host_leaves_without_new_host() {
        OpenChatParticipant soleHost = createHostParticipant();
        List<OpenChatParticipant> lockedList = List.of(soleHost, createParticipant());

        given(openChatParticipantRepository.findAllByRoomIdWithLock(ROOM_ID)).willReturn(lockedList);

        ThrowingCallable action = () -> openChatRoomService.leaveRoom(ROOM_ID, HOST_USER_ID, null);
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_SOLE_HOST_CANNOT_LEAVE);
    }

    @Test
    @DisplayName("나가기 성공 — BR-06 단독 방장이 newHostUserId 제공 시 위임+나가기 원자적 처리")
    void should_delegate_and_leave_atomically_when_sole_host_provides_new_host_br06() {
        OpenChatParticipant soleHost = createHostParticipant();
        OpenChatParticipant newHostTarget = createParticipant();
        List<OpenChatParticipant> lockedList = List.of(soleHost, newHostTarget);

        given(openChatParticipantRepository.findAllByRoomIdWithLock(ROOM_ID)).willReturn(lockedList);

        openChatRoomService.leaveRoom(ROOM_ID, HOST_USER_ID, PARTICIPANT_USER_ID);

        assertThat(newHostTarget.isHost()).isTrue();
        then(openChatParticipantRepository).should().delete(soleHost);
    }

    @Test
    @DisplayName("CustomException 발생 — 단독 방장이 자기 자신을 newHostUserId로 지정 (OPEN_CHAT_ALREADY_HOST)")
    void should_throw_CustomException_when_sole_host_delegates_to_self() {
        OpenChatParticipant soleHost = createHostParticipant();
        List<OpenChatParticipant> lockedList = List.of(soleHost, createParticipant());

        given(openChatParticipantRepository.findAllByRoomIdWithLock(ROOM_ID)).willReturn(lockedList);

        ThrowingCallable action = () -> openChatRoomService.leaveRoom(ROOM_ID, HOST_USER_ID, HOST_USER_ID);
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ALREADY_HOST);
    }

    @Test
    @DisplayName("CustomException 발생 — newHostUserId가 해당 방 비참여자 (OPEN_CHAT_PARTICIPANT_NOT_FOUND)")
    void should_throw_CustomException_when_new_host_is_not_participant() {
        OpenChatParticipant soleHost = createHostParticipant();
        List<OpenChatParticipant> lockedList = List.of(soleHost, createParticipant());

        given(openChatParticipantRepository.findAllByRoomIdWithLock(ROOM_ID)).willReturn(lockedList);

        ThrowingCallable action = () -> openChatRoomService.leaveRoom(ROOM_ID, HOST_USER_ID, NON_PARTICIPANT_USER_ID);
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_PARTICIPANT_NOT_FOUND);
    }

    @Test
    @DisplayName("CustomException 발생 — 나가기 시 요청자가 해당 방 미참여자 (OPEN_CHAT_PARTICIPANT_NOT_FOUND)")
    void should_throw_CustomException_when_requester_is_not_participant_on_leave() {
        List<OpenChatParticipant> lockedList = List.of(createHostParticipant(), createParticipant());

        given(openChatParticipantRepository.findAllByRoomIdWithLock(ROOM_ID)).willReturn(lockedList);

        ThrowingCallable action = () -> openChatRoomService.leaveRoom(ROOM_ID, NON_PARTICIPANT_USER_ID, null);
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_PARTICIPANT_NOT_FOUND);
    }

    @Test
    @DisplayName("방 삭제 성공 — 방장이 방 삭제 후 모든 participant row 및 room row 삭제")
    void should_delete_room_and_all_participants_when_host_deletes_room() {
        OpenChatRoom room = OpenChatMultiHostFixture.createRoom();
        List<OpenChatParticipant> participants = OpenChatMultiHostFixture.createParticipantsWithSoleHost();

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.existsByRoomIdAndUserIdAndIsHost(ROOM_ID, HOST_USER_ID, true)).willReturn(true);
        given(openChatParticipantRepository.findAllByRoomId(ROOM_ID)).willReturn(participants);

        assertThatCode(() -> openChatRoomService.deleteRoom(ROOM_ID, HOST_USER_ID)).doesNotThrowAnyException();
        then(openChatParticipantRepository).should(times(1)).deleteAll(participants);
        then(openChatRoomRepository).should(times(1)).delete(room);
    }

    @Test
    @DisplayName("방 삭제 — 공식 방은 방장도 삭제 불가 (OPEN_CHAT_ROOM_FORBIDDEN)")
    void should_throw_when_any_host_deletes_official_room() {
        OpenChatRoom officialRoom = OpenChatMultiHostFixture.createOfficialRoom();

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(officialRoom));
        given(openChatParticipantRepository.existsByRoomIdAndUserIdAndIsHost(ROOM_ID, HOST_USER_ID, true)).willReturn(true);

        ThrowingCallable action = () -> openChatRoomService.deleteRoom(ROOM_ID, HOST_USER_ID);
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN);
    }

    @Test
    @DisplayName("CustomException 발생 — BR-07 비방장이 방 삭제 (OPEN_CHAT_ROOM_FORBIDDEN)")
    void should_throw_CustomException_when_non_host_deletes_room() {
        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(createRoom()));
        given(openChatParticipantRepository.existsByRoomIdAndUserIdAndIsHost(ROOM_ID, PARTICIPANT_USER_ID, true)).willReturn(false);

        ThrowingCallable action = () -> openChatRoomService.deleteRoom(ROOM_ID, PARTICIPANT_USER_ID);
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN);
    }

    @Test
    @DisplayName("CustomException 발생 — BR-07 공식 방을 방장이 삭제 (OPEN_CHAT_ROOM_FORBIDDEN)")
    void should_throw_CustomException_when_non_admin_host_deletes_official_room() {
        OpenChatRoom officialRoom = OpenChatMultiHostFixture.createOfficialRoom();

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(officialRoom));
        given(openChatParticipantRepository.existsByRoomIdAndUserIdAndIsHost(ROOM_ID, HOST_USER_ID, true)).willReturn(true);

        ThrowingCallable action = () -> openChatRoomService.deleteRoom(ROOM_ID, HOST_USER_ID);
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN);
    }

    @Test
    @DisplayName("CustomException 발생 — 방 삭제 시 roomId에 해당하는 방이 없음 (OPEN_CHAT_ROOM_NOT_FOUND)")
    void should_throw_CustomException_when_room_not_found_on_delete() {
        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

        ThrowingCallable action = () -> openChatRoomService.deleteRoom(ROOM_ID, HOST_USER_ID);
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND);
    }

    private static User mockUser(Long userId) {
        User user = org.mockito.Mockito.mock(User.class);
        lenient().when(user.getName()).thenReturn("테스트유저");
        return user;
    }
}
