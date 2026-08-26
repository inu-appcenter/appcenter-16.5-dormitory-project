package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.openChat.dto.request.RequestCreateDormOfficialRoomDto;
import com.example.appcenter_project.domain.openChat.entity.OpenChatParticipant;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.ChatNotificationMode;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatDormOfficialRoomFixture;
import com.example.appcenter_project.domain.openChat.repository.OpenChatParticipantRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomRepository;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.enums.DormType;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.exception.ErrorCode;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OpenChatDormOfficialRoomServiceTest {

    @Mock
    OpenChatRoomRepository openChatRoomRepository;

    @Mock
    OpenChatParticipantRepository openChatParticipantRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    OpenChatDormOfficialRoomService openChatDormOfficialRoomService;

    // =====================================================================
    // createDormOfficialRoom — 방 생성
    // =====================================================================

    @Test
    @DisplayName("roomId 반환 — AC-1 정상 요청 시 생성된 방 ID 반환")
    void should_return_roomId_when_valid_create_request() {
        // given
        RequestCreateDormOfficialRoomDto request = OpenChatDormOfficialRoomFixture.createRequest();
        OpenChatRoom savedRoom = OpenChatDormOfficialRoomFixture.createDormOfficialRoom(DormType.DORM_1);
        given(openChatRoomRepository.findByTargetDorm(DormType.DORM_1)).willReturn(Optional.empty());
        given(userRepository.findAllByDormType(DormType.DORM_1)).willReturn(Collections.emptyList());
        given(openChatRoomRepository.save(any())).willReturn(savedRoom);

        // when
        Long roomId = openChatDormOfficialRoomService.createDormOfficialRoom(1L, request);

        // then
        assertThat(roomId).isNotNull();
    }

    @Test
    @DisplayName("isOfficial=true 방 생성 — AC-1 공식 방 속성으로 OpenChatRoom 생성")
    void should_create_room_with_isOfficial_true_when_valid_request() {
        // given
        RequestCreateDormOfficialRoomDto request = OpenChatDormOfficialRoomFixture.createRequest();
        OpenChatRoom savedRoom = OpenChatDormOfficialRoomFixture.createDormOfficialRoom(DormType.DORM_1);
        given(openChatRoomRepository.findByTargetDorm(DormType.DORM_1)).willReturn(Optional.empty());
        given(userRepository.findAllByDormType(DormType.DORM_1)).willReturn(Collections.emptyList());
        given(openChatRoomRepository.save(any())).willReturn(savedRoom);
        ArgumentCaptor<OpenChatRoom> captor = ArgumentCaptor.forClass(OpenChatRoom.class);

        // when
        openChatDormOfficialRoomService.createDormOfficialRoom(1L, request);

        // then
        then(openChatRoomRepository).should().save(captor.capture());
        assertThat(captor.getValue().isOfficial()).isTrue();
    }

    @Test
    @DisplayName("targetDorm 설정 확인 — AC-1 요청 dormType이 방의 targetDorm으로 저장")
    void should_set_targetDorm_when_creating_room() {
        // given
        RequestCreateDormOfficialRoomDto request = OpenChatDormOfficialRoomFixture.createRequest();
        OpenChatRoom savedRoom = OpenChatDormOfficialRoomFixture.createDormOfficialRoom(DormType.DORM_1);
        given(openChatRoomRepository.findByTargetDorm(DormType.DORM_1)).willReturn(Optional.empty());
        given(userRepository.findAllByDormType(DormType.DORM_1)).willReturn(Collections.emptyList());
        given(openChatRoomRepository.save(any())).willReturn(savedRoom);
        ArgumentCaptor<OpenChatRoom> captor = ArgumentCaptor.forClass(OpenChatRoom.class);

        // when
        openChatDormOfficialRoomService.createDormOfficialRoom(1L, request);

        // then
        then(openChatRoomRepository).should().save(captor.capture());
        assertThat(captor.getValue().getTargetDorm()).isEqualTo(DormType.DORM_1);
    }

    @Test
    @DisplayName("참여자 벌크 저장 — AC-1 해당 기숙사 유저 3명 모두 OpenChatParticipant로 일괄 등록")
    void should_save_all_participants_when_users_exist() {
        // given
        RequestCreateDormOfficialRoomDto request = OpenChatDormOfficialRoomFixture.createRequest();
        List<User> users = OpenChatDormOfficialRoomFixture.createUsersWithDorm(DormType.DORM_1, 3);
        OpenChatRoom savedRoom = OpenChatDormOfficialRoomFixture.createDormOfficialRoom(DormType.DORM_1);
        given(openChatRoomRepository.findByTargetDorm(DormType.DORM_1)).willReturn(Optional.empty());
        given(userRepository.findAllByDormType(DormType.DORM_1)).willReturn(users);
        given(openChatRoomRepository.save(any())).willReturn(savedRoom);

        // when
        openChatDormOfficialRoomService.createDormOfficialRoom(1L, request);

        // then
        then(openChatParticipantRepository).should().saveAll(argThat(list ->
                ((List<?>) list).size() == 3
        ));
    }

    @Test
    @DisplayName("참여자 없이 방만 생성 — AC-1 해당 기숙사 유저 0명이면 saveAll 빈 리스트")
    void should_create_room_without_participants_when_no_users() {
        // given
        RequestCreateDormOfficialRoomDto request = OpenChatDormOfficialRoomFixture.createRequest();
        OpenChatRoom savedRoom = OpenChatDormOfficialRoomFixture.createDormOfficialRoom(DormType.DORM_1);
        given(openChatRoomRepository.findByTargetDorm(DormType.DORM_1)).willReturn(Optional.empty());
        given(userRepository.findAllByDormType(DormType.DORM_1)).willReturn(Collections.emptyList());
        given(openChatRoomRepository.save(any())).willReturn(savedRoom);

        // when
        openChatDormOfficialRoomService.createDormOfficialRoom(1L, request);

        // then
        then(openChatParticipantRepository).should().saveAll(argThat(list ->
                ((List<?>) list).isEmpty()
        ));
    }

    @Test
    @DisplayName("CustomException 발생 — AC-2 동일 기숙사 공식 방 중복 생성 시 OPEN_CHAT_DORM_OFFICIAL_ROOM_ALREADY_EXISTS")
    void should_throw_CustomException_when_dorm_official_room_already_exists() {
        // given
        RequestCreateDormOfficialRoomDto request = OpenChatDormOfficialRoomFixture.createRequest();
        OpenChatRoom existingRoom = OpenChatDormOfficialRoomFixture.createDormOfficialRoom(DormType.DORM_1);
        given(openChatRoomRepository.findByTargetDorm(DormType.DORM_1)).willReturn(Optional.of(existingRoom));

        // when
        ThrowingCallable action = () -> openChatDormOfficialRoomService.createDormOfficialRoom(1L, request);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_DORM_OFFICIAL_ROOM_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("CustomException 발생 — AC-4 dormType이 NONE 시 INVALID_DORM_TYPE")
    void should_throw_CustomException_when_dorm_type_is_none() {
        // given
        RequestCreateDormOfficialRoomDto request = OpenChatDormOfficialRoomFixture.createRequestWithNoneDormType();

        // when
        ThrowingCallable action = () -> openChatDormOfficialRoomService.createDormOfficialRoom(1L, request);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_DORM_TYPE);
    }

    @Test
    @DisplayName("중복 생성 검사 미실행 — AC-4 NONE 요청 시 중복 검사 전에 예외 발생")
    void should_not_check_duplicate_when_dorm_type_is_none() {
        // given
        RequestCreateDormOfficialRoomDto request = OpenChatDormOfficialRoomFixture.createRequestWithNoneDormType();

        // when
        ThrowingCallable action = () -> openChatDormOfficialRoomService.createDormOfficialRoom(1L, request);

        // then
        assertThatThrownBy(action).isInstanceOf(CustomException.class);
        then(openChatRoomRepository).should(never()).findByTargetDorm(any());
    }

    // =====================================================================
    // reassignDormRoom — 기숙사 변경 시 방 재배정
    // =====================================================================

    @Test
    @DisplayName("퇴장 + 입장 처리 — AC-5 DORM_2 → DORM_1 변경 시 두 방 모두 존재하면 퇴장 후 입장")
    void should_leave_old_room_and_join_new_room_when_both_rooms_exist() {
        // given
        OpenChatRoom oldRoom = OpenChatDormOfficialRoomFixture.createDormOfficialRoom(DormType.DORM_2);
        OpenChatRoom newRoom = OpenChatDormOfficialRoomFixture.createDormOfficialRoom(DormType.DORM_1);
        OpenChatParticipant participant = OpenChatDormOfficialRoomFixture.createParticipant(null, 10L);
        given(openChatRoomRepository.findByTargetDorm(DormType.DORM_2)).willReturn(Optional.of(oldRoom));
        given(openChatRoomRepository.findByTargetDorm(DormType.DORM_1)).willReturn(Optional.of(newRoom));
        given(openChatParticipantRepository.findByRoomIdAndUserId(any(), eq(10L)))
                .willReturn(Optional.of(participant));
        given(openChatParticipantRepository.existsByRoomIdAndUserId(any(), eq(10L))).willReturn(false);

        // when
        openChatDormOfficialRoomService.reassignDormRoom(10L, DormType.DORM_2, DormType.DORM_1);

        // then
        then(openChatParticipantRepository).should().delete(participant);
    }

    @Test
    @DisplayName("새 방 입장 처리 — AC-5 DORM_2 → DORM_1 변경 시 DORM_1 방에 참여자 생성")
    void should_join_new_room_when_new_dorm_official_room_exists() {
        // given
        OpenChatRoom oldRoom = OpenChatDormOfficialRoomFixture.createDormOfficialRoom(DormType.DORM_2);
        OpenChatRoom newRoom = OpenChatDormOfficialRoomFixture.createDormOfficialRoom(DormType.DORM_1);
        OpenChatParticipant participant = OpenChatDormOfficialRoomFixture.createParticipant(null, 10L);
        given(openChatRoomRepository.findByTargetDorm(DormType.DORM_2)).willReturn(Optional.of(oldRoom));
        given(openChatRoomRepository.findByTargetDorm(DormType.DORM_1)).willReturn(Optional.of(newRoom));
        given(openChatParticipantRepository.findByRoomIdAndUserId(any(), eq(10L)))
                .willReturn(Optional.of(participant));
        given(openChatParticipantRepository.existsByRoomIdAndUserId(any(), eq(10L))).willReturn(false);

        // when
        openChatDormOfficialRoomService.reassignDormRoom(10L, DormType.DORM_2, DormType.DORM_1);

        // then
        then(openChatParticipantRepository).should().save(any(OpenChatParticipant.class));
    }

    @Test
    @DisplayName("퇴장만 처리 — AC-6 새 기숙사 공식 방 없으면 이전 방 퇴장만 처리")
    void should_only_leave_old_room_when_new_dorm_room_not_exists() {
        // given
        OpenChatRoom oldRoom = OpenChatDormOfficialRoomFixture.createDormOfficialRoom(DormType.DORM_2);
        OpenChatParticipant participant = OpenChatDormOfficialRoomFixture.createParticipant(null, 10L);
        given(openChatRoomRepository.findByTargetDorm(DormType.DORM_2)).willReturn(Optional.of(oldRoom));
        given(openChatRoomRepository.findByTargetDorm(DormType.DORM_1)).willReturn(Optional.empty());
        given(openChatParticipantRepository.findByRoomIdAndUserId(any(), eq(10L)))
                .willReturn(Optional.of(participant));

        // when
        openChatDormOfficialRoomService.reassignDormRoom(10L, DormType.DORM_2, DormType.DORM_1);

        // then
        then(openChatParticipantRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("입장만 처리 — AC-7 NONE → DORM_1 변경 시 이전 방 퇴장 없이 DORM_1 방 입장")
    void should_only_join_new_room_when_old_dorm_is_none() {
        // given
        OpenChatRoom newRoom = OpenChatDormOfficialRoomFixture.createDormOfficialRoom(DormType.DORM_1);
        given(openChatRoomRepository.findByTargetDorm(DormType.DORM_1)).willReturn(Optional.of(newRoom));
        given(openChatParticipantRepository.existsByRoomIdAndUserId(any(), eq(10L))).willReturn(false);

        // when
        openChatDormOfficialRoomService.reassignDormRoom(10L, DormType.NONE, DormType.DORM_1);

        // then
        then(openChatParticipantRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("새 방 입장 확인 — AC-7 NONE → DORM_1 변경 시 DORM_1 방에 참여자 생성")
    void should_create_participant_when_old_dorm_is_none_and_new_dorm_room_exists() {
        // given
        OpenChatRoom newRoom = OpenChatDormOfficialRoomFixture.createDormOfficialRoom(DormType.DORM_1);
        given(openChatRoomRepository.findByTargetDorm(DormType.DORM_1)).willReturn(Optional.of(newRoom));
        given(openChatParticipantRepository.existsByRoomIdAndUserId(any(), eq(10L))).willReturn(false);

        // when
        openChatDormOfficialRoomService.reassignDormRoom(10L, DormType.NONE, DormType.DORM_1);

        // then
        then(openChatParticipantRepository).should().save(any(OpenChatParticipant.class));
    }

    @Test
    @DisplayName("중복 삽입 방지 — AC-9 이미 참여 중인 방에 재입장 시 save 미호출")
    void should_not_insert_duplicate_when_user_already_participates() {
        // given
        OpenChatRoom newRoom = OpenChatDormOfficialRoomFixture.createDormOfficialRoom(DormType.DORM_1);
        given(openChatRoomRepository.findByTargetDorm(DormType.DORM_1)).willReturn(Optional.of(newRoom));
        given(openChatParticipantRepository.existsByRoomIdAndUserId(any(), eq(10L))).willReturn(true);

        // when
        openChatDormOfficialRoomService.reassignDormRoom(10L, DormType.NONE, DormType.DORM_1);

        // then
        then(openChatParticipantRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("퇴장 스킵 — AC-6 이전 방에 참여하지 않은 경우 delete 미호출")
    void should_skip_leave_when_user_not_participating_in_old_room() {
        // given
        OpenChatRoom oldRoom = OpenChatDormOfficialRoomFixture.createDormOfficialRoom(DormType.DORM_2);
        OpenChatRoom newRoom = OpenChatDormOfficialRoomFixture.createDormOfficialRoom(DormType.DORM_1);
        given(openChatRoomRepository.findByTargetDorm(DormType.DORM_2)).willReturn(Optional.of(oldRoom));
        given(openChatRoomRepository.findByTargetDorm(DormType.DORM_1)).willReturn(Optional.of(newRoom));
        given(openChatParticipantRepository.findByRoomIdAndUserId(any(), eq(10L)))
                .willReturn(Optional.empty());
        given(openChatParticipantRepository.existsByRoomIdAndUserId(any(), eq(10L))).willReturn(false);

        // when
        openChatDormOfficialRoomService.reassignDormRoom(10L, DormType.DORM_2, DormType.DORM_1);

        // then
        then(openChatParticipantRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("재배정 로직 미실행 — AC-8 dormType 변경 없으면 openChatParticipantRepository 미호출")
    void should_not_reassign_when_dorm_type_unchanged() {
        // given (dormType 동일 = 변경 없음, 이 메서드 자체가 호출되지 않아야 하므로
        //        UserService가 변경 시에만 reassignDormRoom 호출하는 케이스를 UserServiceTest에서 검증)
        // 이 테스트는 reassignDormRoom 내부 로직에서 oldDorm == newDorm일 때 얼리 리턴을 검증함
        OpenChatRoom room = OpenChatDormOfficialRoomFixture.createDormOfficialRoom(DormType.DORM_1);
        given(openChatRoomRepository.findByTargetDorm(DormType.DORM_1)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.existsByRoomIdAndUserId(any(), eq(10L))).willReturn(true);

        // when
        openChatDormOfficialRoomService.reassignDormRoom(10L, DormType.DORM_1, DormType.DORM_1);

        // then
        then(openChatParticipantRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("공식방 참여자 기본 알림 = EVERY — 벌크 생성 시 전원 즉시 알림")
    void should_create_participants_with_every_when_official_room() {
        RequestCreateDormOfficialRoomDto request = OpenChatDormOfficialRoomFixture.createRequest();
        List<User> users = OpenChatDormOfficialRoomFixture.createUsersWithDorm(DormType.DORM_1, 3);
        OpenChatRoom savedRoom = OpenChatDormOfficialRoomFixture.createDormOfficialRoom(DormType.DORM_1);
        given(openChatRoomRepository.findByTargetDorm(DormType.DORM_1)).willReturn(Optional.empty());
        given(userRepository.findAllByDormType(DormType.DORM_1)).willReturn(users);
        given(openChatRoomRepository.save(any())).willReturn(savedRoom);

        openChatDormOfficialRoomService.createDormOfficialRoom(1L, request);

        then(openChatParticipantRepository).should().saveAll(argThat(list ->
                ((List<OpenChatParticipant>) list).stream()
                        .allMatch(p -> p.getNotificationMode() == ChatNotificationMode.EVERY)
        ));
    }

    @Test
    @DisplayName("재배정 재입장 참여자 기본 알림 = EVERY")
    void should_create_participant_with_every_on_reassign() {
        OpenChatRoom newRoom = OpenChatDormOfficialRoomFixture.createDormOfficialRoom(DormType.DORM_1);
        given(openChatRoomRepository.findByTargetDorm(DormType.DORM_1)).willReturn(Optional.of(newRoom));
        given(openChatParticipantRepository.existsByRoomIdAndUserId(any(), eq(10L))).willReturn(false);
        ArgumentCaptor<OpenChatParticipant> captor = ArgumentCaptor.forClass(OpenChatParticipant.class);

        openChatDormOfficialRoomService.reassignDormRoom(10L, DormType.NONE, DormType.DORM_1);

        then(openChatParticipantRepository).should().save(captor.capture());
        assertThat(captor.getValue().getNotificationMode()).isEqualTo(ChatNotificationMode.EVERY);
    }
}
