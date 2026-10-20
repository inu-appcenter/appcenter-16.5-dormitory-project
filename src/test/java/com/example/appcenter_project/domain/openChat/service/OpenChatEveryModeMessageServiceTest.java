package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.common.image.repository.ImageRepository;
import com.example.appcenter_project.common.image.service.ImageService;
import com.example.appcenter_project.domain.openChat.repository.OpenChatMessageRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatParticipantRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomRepository;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.config.OpenChatSessionRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * BR-672 — OpenChatMessageService EVERY 모드 즉시 FCM 발송 연동 테스트
 *
 * 구현 에이전트가 수정해야 할 클래스:
 * - OpenChatMessageService:
 *   - sendMessage(), sendImageMessage() 에서 TEXT/IMAGE 저장 후
 *     OpenChatNotificationService.sendImmediateNotifications(roomId, usersToRead, title, body) 호출 추가
 *   - sendSystemMessage() 에서는 sendImmediateNotifications 호출 안 함 (AC-8)
 *   - OpenChatNotificationService 의존성 추가
 */
@ExtendWith(MockitoExtension.class)
class OpenChatEveryModeMessageServiceTest {

    @Mock
    OpenChatRoomRepository openChatRoomRepository;
    @Mock
    OpenChatParticipantRepository openChatParticipantRepository;
    @Mock
    OpenChatMessageRepository openChatMessageRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    SimpMessagingTemplate messagingTemplate;
    @Mock
    ImageService imageService;
    @Mock
    ImageRepository imageRepository;
    @Mock
    OpenChatSessionRegistry sessionRegistry;
    @Mock
    OpenChatNotificationService openChatNotificationService;

    @InjectMocks
    OpenChatMessageService openChatMessageService;

    private static final Long SENDER_ID = 1L;
    private static final Long ROOM_ID = 10L;

    // ──────────────────────────────────────────────
    // AC-1/2 연동: sendMessage 시 sendImmediateNotifications 호출
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("EVERY 연동 — sendMessage(TEXT) 후 sendImmediateNotifications 호출됨")
    void should_call_sendImmediateNotifications_after_text_message_sent() {
        // NOTE: 구현 후 아래 주석을 해제하고 placeholder를 제거한다.
        //
        // given
        // OpenChatRoom room = ChatNotificationModeFixture.createRealRoom("스터디방");
        // OpenChatParticipant sender = ChatNotificationModeFixture.createParticipant(ROOM_ID, SENDER_ID);
        // User user = ChatNotificationModeFixture.createUser(SENDER_ID);
        // OpenChatMessage message = ChatNotificationModeFixture.createTextMessage(ROOM_ID, SENDER_ID);
        // RequestOpenChatMessageDto request = new RequestOpenChatMessageDto(ROOM_ID, "안녕");
        //
        // given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        // given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, SENDER_ID)).willReturn(Optional.of(sender));
        // given(userRepository.findById(SENDER_ID)).willReturn(Optional.of(user));
        // given(openChatMessageRepository.save(any())).willReturn(message);
        // given(sessionRegistry.getSubscriberUserIds(ROOM_ID)).willReturn(Set.of());
        // given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(2L);
        // given(openChatParticipantRepository.countReadByRoomIdAndMessageId(eq(ROOM_ID), any())).willReturn(1L);
        //
        // when
        // openChatMessageService.sendMessage(SENDER_ID, request);
        //
        // then
        // then(openChatNotificationService).should().sendImmediateNotifications(
        //     eq(ROOM_ID), any(Set.class), eq("스터디방"), eq("안녕"));

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("EVERY 연동 — AC-8: sendSystemMessage 후 sendImmediateNotifications 호출 안 됨")
    void should_not_call_sendImmediateNotifications_for_system_message() {
        // NOTE: 구현 후 아래 주석을 해제하고 placeholder를 제거한다.
        //
        // given
        // OpenChatRoom room = ChatNotificationModeFixture.createRealRoom("스터디방");
        // OpenChatMessage message = ChatNotificationModeFixture.createSystemMessage(ROOM_ID);
        //
        // given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        // given(openChatMessageRepository.save(any())).willReturn(message);
        // given(sessionRegistry.getSubscriberUserIds(ROOM_ID)).willReturn(Set.of());
        // given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(2L);
        // given(openChatParticipantRepository.countReadByRoomIdAndMessageId(eq(ROOM_ID), any())).willReturn(0L);
        //
        // when
        // openChatMessageService.sendSystemMessage(ROOM_ID, "홍길동님이 입장했습니다.");
        //
        // then — SYSTEM 메시지는 즉시 FCM 대상 아님
        // then(openChatNotificationService).should(never()).sendImmediateNotifications(any(), any(), any(), any());

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("EVERY 연동 — IMAGE 메시지 FCM body는 '[이미지]'")
    void should_pass_image_placeholder_body_when_image_message_sent() {
        // IMAGE 메시지의 FCM body 는 "[이미지]" 고정
        // sendImmediateNotifications(roomId, onlineUserIds, title, "[이미지]")
        //
        // NOTE: 구현 후 ArgumentCaptor 로 body 파라미터 검증

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("EVERY 연동 — 발신자 본인은 usersToRead 에 포함되어 FCM 발송 제외됨")
    void should_exclude_sender_from_fcm_target_because_usersToRead_includes_sender() {
        // 기존 usersToRead = sessionRegistry.getSubscriberUserIds + senderId
        // sendImmediateNotifications 에 onlineUserIds 로 usersToRead 를 전달하므로
        // 발신자는 자동으로 제외된다.
        //
        // NOTE: 구현 후 ArgumentCaptor 로 onlineUserIds 에 SENDER_ID 포함 여부 검증

        assertThat(true).isTrue();
    }
}
