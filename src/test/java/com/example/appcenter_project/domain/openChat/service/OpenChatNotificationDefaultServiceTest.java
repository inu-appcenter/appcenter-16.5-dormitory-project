package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.openChat.dto.response.ResponseNotificationModeDto;
import com.example.appcenter_project.domain.openChat.entity.OpenChatParticipant;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.ChatNotificationMode;
import com.example.appcenter_project.domain.openChat.repository.OpenChatParticipantRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomRepository;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.enums.DormType;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OpenChatNotificationDefaultServiceTest {

    @Mock OpenChatRoomRepository openChatRoomRepository;
    @Mock OpenChatParticipantRepository openChatParticipantRepository;
    @Mock UserRepository userRepository;
    @Mock OpenChatMessageService openChatMessageService;

    @InjectMocks OpenChatRoomService openChatRoomService;

    private static final Long USER_ID = 10L;

    @Test
    @DisplayName("joinRoom — 공식 기숙사방 입장 시 기본 알림 = EVERY")
    void official_room_join_defaults_every() {
        OpenChatRoom room = OpenChatRoom.createDormOfficialForTest(1L, "1기숙사 공식방", DormType.DORM_1);
        User user = User.createForTest(USER_ID, "tester");
        given(openChatRoomRepository.findByIdWithLock(1L)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.existsByRoomIdAndUserId(1L, USER_ID)).willReturn(false);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(openChatParticipantRepository.countByRoomId(1L)).willReturn(0L);
        ArgumentCaptor<OpenChatParticipant> captor = ArgumentCaptor.forClass(OpenChatParticipant.class);

        openChatRoomService.joinRoom(USER_ID, 1L, null);

        then(openChatParticipantRepository).should().save(captor.capture());
        assertThat(captor.getValue().getNotificationMode()).isEqualTo(ChatNotificationMode.EVERY);
    }

    @Test
    @DisplayName("joinRoom — 일반 방 입장 시 기본 알림 = EVERY")
    void regular_room_join_defaults_every() {
        OpenChatRoom room = OpenChatRoom.createForTest(2L, "일반방");
        User user = User.createForTest(USER_ID, "tester");
        given(openChatRoomRepository.findByIdWithLock(2L)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.existsByRoomIdAndUserId(2L, USER_ID)).willReturn(false);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(openChatParticipantRepository.countByRoomId(2L)).willReturn(0L);
        ArgumentCaptor<OpenChatParticipant> captor = ArgumentCaptor.forClass(OpenChatParticipant.class);

        openChatRoomService.joinRoom(USER_ID, 2L, null);

        then(openChatParticipantRepository).should().save(captor.capture());
        assertThat(captor.getValue().getNotificationMode()).isEqualTo(ChatNotificationMode.EVERY);
    }

    @Test
    @DisplayName("getNotificationMode — 참여자의 현재 알림 모드 반환")
    void getNotificationMode_returns_participant_mode() {
        OpenChatParticipant p = OpenChatParticipant.create(1L, USER_ID, LocalDateTime.now(), ChatNotificationMode.BUNDLED);
        given(openChatParticipantRepository.findByRoomIdAndUserId(1L, USER_ID)).willReturn(Optional.of(p));

        ResponseNotificationModeDto result = openChatRoomService.getNotificationMode(USER_ID, 1L);

        assertThat(result.getMode()).isEqualTo(ChatNotificationMode.BUNDLED);
    }

    @Test
    @DisplayName("getNotificationMode — 비참여자면 OPEN_CHAT_PARTICIPANT_NOT_FOUND")
    void getNotificationMode_throws_when_not_participant() {
        given(openChatParticipantRepository.findByRoomIdAndUserId(1L, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> openChatRoomService.getNotificationMode(USER_ID, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.OPEN_CHAT_PARTICIPANT_NOT_FOUND);
    }
}