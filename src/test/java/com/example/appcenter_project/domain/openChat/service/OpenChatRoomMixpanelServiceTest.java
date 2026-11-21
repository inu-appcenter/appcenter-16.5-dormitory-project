package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.block.service.BlockService;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomRecruitmentStatus;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatRecruitmentStatusFixture;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatRoomFixture;
import com.example.appcenter_project.domain.openChat.repository.OpenChatParticipantRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomRepository;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.mixpanel.ChatMixpanelTracker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpenChatRoomMixpanelServiceTest {

    @Mock OpenChatRoomRepository openChatRoomRepository;
    @Mock OpenChatParticipantRepository openChatParticipantRepository;
    @Mock UserRepository userRepository;
    @Mock BlockService blockService;
    @Mock OpenChatMessageService openChatMessageService;
    @Mock ChatMixpanelTracker chatMixpanelTracker;

    @InjectMocks OpenChatRoomService openChatRoomService;

    private static final Long ROOM_ID = 1L;
    private static final Long USER_ID = 1L;

    private OpenChatRoom derivedRoom(boolean closed) {
        OpenChatRoom room = closed
                ? OpenChatRecruitmentStatusFixture.createDerivedRoomClosed(USER_ID)
                : OpenChatRecruitmentStatusFixture.createDerivedRoom(USER_ID);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(OpenChatRecruitmentStatusFixture.createOwner()));
        return room;
    }

    @Test
    @DisplayName("[BR-TBD] 신규 참여 저장 시 chat_room_joined 발화")
    void newParticipant_fires() {
        OpenChatRoom room = OpenChatRoomFixture.createRoom();
        User user = OpenChatRecruitmentStatusFixture.createOwner();
        given(openChatRoomRepository.findByIdWithLock(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.existsByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(false);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(1L);

        openChatRoomService.joinRoom(USER_ID, ROOM_ID, null);

        then(chatMixpanelTracker).should().trackRoomJoined(USER_ID, ROOM_ID, "OPEN");
    }

    @Test
    @DisplayName("[BR-TBD] 이미 참여 중인 사용자의 재호출은 발화하지 않는다")
    void existingParticipant_noEvent() {
        OpenChatRoom room = OpenChatRoomFixture.createRoom();
        given(openChatRoomRepository.findByIdWithLock(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.existsByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(true);
        given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(1L);

        openChatRoomService.joinRoom(USER_ID, ROOM_ID, null);

        then(chatMixpanelTracker).should(never()).trackRoomJoined(any(), any(), any());
    }

    @Test
    @DisplayName("[BR-TBD] 모집 마감 상태 전환(CLOSED) 시 recruitment_closed 발화")
    void closeViaStatus_fires() {
        derivedRoom(false);
        given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(4L);

        openChatRoomService.updateRecruitmentStatus(USER_ID, ROOM_ID, OpenChatRoomRecruitmentStatus.CLOSED);

        then(chatMixpanelTracker).should().trackRecruitmentClosed(eq(USER_ID), eq(ROOM_ID), any(), eq(4L));
    }

    @Test
    @DisplayName("[BR-TBD] closeRecruitment 호출 시 recruitment_closed 발화")
    void closeRecruitment_fires() {
        derivedRoom(false);
        given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(2L);

        openChatRoomService.closeRecruitment(USER_ID, ROOM_ID);

        then(chatMixpanelTracker).should().trackRecruitmentClosed(eq(USER_ID), eq(ROOM_ID), any(), eq(2L));
    }

    @Test
    @DisplayName("[BR-TBD] 재개(OPEN) 전환은 recruitment_closed 를 발화하지 않는다")
    void reopen_noEvent() {
        derivedRoom(true);

        openChatRoomService.updateRecruitmentStatus(USER_ID, ROOM_ID, OpenChatRoomRecruitmentStatus.OPEN);

        then(chatMixpanelTracker).should(never()).trackRecruitmentClosed(any(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("[BR-TBD] 이미 마감된 방을 다시 마감하면 예외이며 발화하지 않는다")
    void alreadyClosed_noEvent() {
        derivedRoom(true);

        assertThatThrownBy(() -> openChatRoomService.updateRecruitmentStatus(USER_ID, ROOM_ID, OpenChatRoomRecruitmentStatus.CLOSED))
                .isInstanceOf(CustomException.class);

        then(chatMixpanelTracker).should(never()).trackRecruitmentClosed(any(), any(), any(), anyLong());
    }
}
