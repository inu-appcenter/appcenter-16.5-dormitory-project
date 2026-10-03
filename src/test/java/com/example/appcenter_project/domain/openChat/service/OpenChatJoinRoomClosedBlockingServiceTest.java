package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatJoinRoomClosedBlockingFixture;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OpenChatJoinRoomClosedBlockingServiceTest {

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
    // Business Rule — Service
    // ============================================================

    @Test
    @DisplayName("CustomException 발생 — AC-01 BR-761 CLOSED 상태 방에 비참여자 joinRoom → OPEN_CHAT_ROOM_CLOSED_FOR_JOIN")
    void should_throw_CustomException_when_room_is_closed_and_user_is_not_participant() {
        // given
        OpenChatRoom room = OpenChatJoinRoomClosedBlockingFixture.createClosedDerivedRoom(1L);
        given(openChatRoomRepository.findByIdWithLock(1L)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.existsByRoomIdAndUserId(1L, 2L)).willReturn(false);

        // when
        ThrowingCallable action = () -> openChatRoomService.joinRoom(2L, 1L, null);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_CLOSED_FOR_JOIN);
    }

    @Test
    @DisplayName("참여자 저장 미호출 — AC-01 BR-761 CLOSED 방 차단 시 save 미호출")
    void should_not_save_participant_when_room_is_closed() {
        // given
        OpenChatRoom room = OpenChatJoinRoomClosedBlockingFixture.createClosedDerivedRoom(1L);
        given(openChatRoomRepository.findByIdWithLock(1L)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.existsByRoomIdAndUserId(1L, 2L)).willReturn(false);

        // when
        assertThatThrownBy(() -> openChatRoomService.joinRoom(2L, 1L, null))
                .isInstanceOf(CustomException.class);

        // then
        then(openChatParticipantRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("CLOSED 검증 건너뜀 — AC-08 BR-761 이미 참여한 유저는 CLOSED 방에서 CLOSED_FOR_JOIN 미발생")
    void should_not_throw_CLOSED_FOR_JOIN_when_user_is_already_participant() {
        // given
        OpenChatRoom room = OpenChatJoinRoomClosedBlockingFixture.createClosedDerivedRoom(1L);
        given(openChatRoomRepository.findByIdWithLock(1L)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.existsByRoomIdAndUserId(1L, 2L)).willReturn(true);
        given(openChatParticipantRepository.countByRoomId(1L)).willReturn(1L);

        // when
        ThrowingCallable action = () -> openChatRoomService.joinRoom(2L, 1L, null);

        // then
        assertThatCode(action).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CLOSED_FOR_JOIN 미발생 — AC-03 BR-761 CLOSED→OPEN 전환 후 비참여자 joinRoom 시 CLOSED_FOR_JOIN 미발생")
    void should_not_throw_CLOSED_FOR_JOIN_when_room_is_reopened() {
        // given
        OpenChatRoom room = OpenChatJoinRoomClosedBlockingFixture.createReopenedDerivedRoom(1L);
        User user = User.createForTest(2L, "user2", DormType.DORM_1);
        given(openChatRoomRepository.findByIdWithLock(1L)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.existsByRoomIdAndUserId(1L, 2L)).willReturn(false);
        given(userRepository.findById(2L)).willReturn(Optional.of(user));

        // when
        Throwable thrown = catchThrowable(() -> openChatRoomService.joinRoom(2L, 1L, null));

        // then
        if (thrown instanceof CustomException customException) {
            assertThat(customException.getErrorCode())
                    .isNotEqualTo(ErrorCode.OPEN_CHAT_ROOM_CLOSED_FOR_JOIN);
        }
    }

    // ============================================================
    // Error Case — Service
    // ============================================================

    @Test
    @DisplayName("CustomException 발생 — 존재하지 않는 roomId → OPEN_CHAT_ROOM_NOT_FOUND")
    void should_throw_CustomException_when_room_not_found() {
        // given
        given(openChatRoomRepository.findByIdWithLock(999999L)).willReturn(Optional.empty());

        // when
        ThrowingCallable action = () -> openChatRoomService.joinRoom(2L, 999999L, null);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND);
    }

    // ============================================================
    // Edge Case — Service
    // ============================================================

    @Test
    @DisplayName("isRecruitmentClosed() true 확인 — CLOSED 상태 방의 필드 일관성")
    void should_return_true_for_isRecruitmentClosed_when_room_is_closed() {
        // given
        OpenChatRoom room = OpenChatJoinRoomClosedBlockingFixture.createClosedDerivedRoom(1L);

        // when
        boolean closed = room.isRecruitmentClosed();

        // then
        assertThat(closed).isTrue();
    }

    @Test
    @DisplayName("isRecruitmentClosed() false 확인 — OPEN 상태 방의 필드 일관성")
    void should_return_false_for_isRecruitmentClosed_when_room_is_open() {
        // given
        OpenChatRoom room = OpenChatJoinRoomClosedBlockingFixture.createOpenDerivedRoom(1L);

        // when
        boolean closed = room.isRecruitmentClosed();

        // then
        assertThat(closed).isFalse();
    }

    @Test
    @DisplayName("비관적 락 조회 호출 확인 — AC-05 BR-761 joinRoom은 findByIdWithLock 사용")
    void should_call_findByIdWithLock_when_joining_room() {
        // given
        OpenChatRoom room = OpenChatJoinRoomClosedBlockingFixture.createClosedDerivedRoom(1L);
        given(openChatRoomRepository.findByIdWithLock(1L)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.existsByRoomIdAndUserId(1L, 2L)).willReturn(false);

        // when
        assertThatThrownBy(() -> openChatRoomService.joinRoom(2L, 1L, null))
                .isInstanceOf(CustomException.class);

        // then
        then(openChatRoomRepository).should().findByIdWithLock(1L);
    }

    @Test
    @DisplayName("isRecruitmentClosed() false 확인 — CLOSED→OPEN 재개 후 필드 일관성")
    void should_return_false_for_isRecruitmentClosed_after_reopen() {
        // given
        OpenChatRoom room = OpenChatJoinRoomClosedBlockingFixture.createReopenedDerivedRoom(1L);

        // when
        boolean closed = room.isRecruitmentClosed();

        // then
        assertThat(closed).isFalse();
    }
}
