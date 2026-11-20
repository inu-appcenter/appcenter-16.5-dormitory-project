package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.common.image.repository.ImageRepository;
import com.example.appcenter_project.common.image.service.ImageService;
import com.example.appcenter_project.domain.openChat.dto.request.RequestOpenChatMessageDto;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatMultiImageFixture;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatRecruitmentStatusFixture;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatRoomFixture;
import com.example.appcenter_project.domain.openChat.repository.OpenChatMessageRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatParticipantRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomRepository;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.config.OpenChatSessionRegistry;
import com.example.appcenter_project.global.mixpanel.ChatMixpanelTracker;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpenChatMessageMixpanelServiceTest {

    @Mock OpenChatRoomRepository openChatRoomRepository;
    @Mock OpenChatParticipantRepository openChatParticipantRepository;
    @Mock OpenChatMessageRepository openChatMessageRepository;
    @Mock UserRepository userRepository;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock ImageService imageService;
    @Mock ImageRepository imageRepository;
    @Mock OpenChatSessionRegistry sessionRegistry;
    @Mock HttpServletRequest httpServletRequest;
    @Mock ChatMixpanelTracker chatMixpanelTracker;

    @InjectMocks OpenChatMessageService openChatMessageService;

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 10L;

    private RequestOpenChatMessageDto textRequest() {
        RequestOpenChatMessageDto request = new RequestOpenChatMessageDto();
        ReflectionTestUtils.setField(request, "roomId", ROOM_ID);
        ReflectionTestUtils.setField(request, "content", "안녕");
        return request;
    }

    private void givenParticipantInRoom(OpenChatRoom room) {
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
        User sender = OpenChatRecruitmentStatusFixture.createOwner();
        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID))
                .willReturn(Optional.of(OpenChatRoomFixture.createParticipant(USER_ID)));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(sender));
    }

    @Test
    @DisplayName("[BR-TBD] TEXT 저장 + 방의 첫 메시지(영향 행 1) → first=true 로 발화")
    void textMessage_first() {
        givenParticipantInRoom(OpenChatRoomFixture.createRoom());
        given(openChatRoomRepository.markFirstMessageSent(eq(ROOM_ID), any(LocalDateTime.class))).willReturn(1);

        openChatMessageService.sendMessage(USER_ID, textRequest());

        then(chatMixpanelTracker).should().trackMessageSent(eq(USER_ID), eq(ROOM_ID), eq("OPEN"), any(), eq("TEXT"), eq(true));
    }

    @Test
    @DisplayName("[BR-TBD] 이미 첫 메시지가 있는 방(영향 행 0) → first=false")
    void textMessage_notFirst() {
        givenParticipantInRoom(OpenChatRoomFixture.createRoom());
        given(openChatRoomRepository.markFirstMessageSent(eq(ROOM_ID), any(LocalDateTime.class))).willReturn(0);

        openChatMessageService.sendMessage(USER_ID, textRequest());

        then(chatMixpanelTracker).should().trackMessageSent(eq(USER_ID), eq(ROOM_ID), eq("OPEN"), any(), eq("TEXT"), eq(false));
    }

    @Test
    @DisplayName("[BR-TBD] 파생방은 room_type=DERIVED 로 발화")
    void derivedRoomType() {
        givenParticipantInRoom(OpenChatRecruitmentStatusFixture.createDerivedRoom(USER_ID));

        openChatMessageService.sendMessage(USER_ID, textRequest());

        then(chatMixpanelTracker).should().trackMessageSent(eq(USER_ID), eq(ROOM_ID), eq("DERIVED"), any(), eq("TEXT"), eq(false));
    }

    @Test
    @DisplayName("[BR-TBD] 참여자가 아니어서 저장되지 않으면 발화하지 않는다")
    void notParticipant_noEvent() {
        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(OpenChatRoomFixture.createRoom()));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.empty());

        openChatMessageService.sendMessage(USER_ID, textRequest());

        then(chatMixpanelTracker).should(never()).trackMessageSent(any(), any(), any(), any(), any(), anyBoolean());
        then(openChatRoomRepository).should(never()).markFirstMessageSent(any(), any());
    }

    @Test
    @DisplayName("[BR-TBD] 시스템 메시지는 발화하지 않고 첫 메시지로도 세지 않는다")
    void systemMessage_noEvent() {
        OpenChatRoom room = OpenChatRoomFixture.createRoom();
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));

        openChatMessageService.sendSystemMessage(ROOM_ID, "홍길동님이 입장했습니다.");

        then(chatMixpanelTracker).should(never()).trackMessageSent(any(), any(), any(), any(), any(), anyBoolean());
        then(openChatRoomRepository).should(never()).markFirstMessageSent(any(), any());
    }

    @Test
    @DisplayName("[BR-TBD] 이미지 3장 → 메시지마다 IMAGE 발화, 첫 장만 first=true")
    void imageMessages_onlyFirstIsFirst() {
        givenParticipantInRoom(OpenChatRoomFixture.createRoom());
        List<MultipartFile> images = OpenChatMultiImageFixture.createImageList(3);
        given(openChatRoomRepository.markFirstMessageSent(eq(ROOM_ID), any(LocalDateTime.class))).willReturn(1, 0, 0);

        openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        then(chatMixpanelTracker).should(times(3))
                .trackMessageSent(eq(USER_ID), eq(ROOM_ID), eq("OPEN"), any(), eq("IMAGE"), anyBoolean());
        then(chatMixpanelTracker).should(times(1))
                .trackMessageSent(any(), any(), any(), any(), any(), eq(true));
    }
}
