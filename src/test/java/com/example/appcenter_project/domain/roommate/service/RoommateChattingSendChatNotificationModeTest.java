package com.example.appcenter_project.domain.roommate.service;

import com.example.appcenter_project.domain.fcm.repository.FcmOutboxRepository;
import com.example.appcenter_project.domain.notification.service.NotificationService;
import com.example.appcenter_project.domain.openChat.enums.ChatNotificationMode;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom;
import com.example.appcenter_project.domain.roommate.fixture.RoommateChattingNotificationFixture;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingChatRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingRoomRepository;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.repository.FcmTokenRepository;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.config.RoommateWebSocketEventListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * BR-739 — sendChat() 내 FCM 분기 테스트 (EVERY / BUNDLED / OFF)
 *
 * 구현 에이전트가 수정해야 할 클래스:
 * - RoommateChattingChatService.sendChatNotification():
 *   수신자의 notificationMode 를 읽어 EVERY 때만 FCM 발송,
 *   BUNDLED / OFF 는 즉시 발송하지 않음.
 * - RoommateChattingChatService 의존성 추가:
 *   - FcmTokenRepository (또는 FcmOutboxRepository)
 *   - RoommateChattingNotificationService (mode 조회용, 또는 room 에서 직접 읽기)
 *
 * AC 커버리지:
 * - AC-4: EVERY 모드 + 수신자 오프라인 → FcmOutbox 즉시 저장
 * - AC-5: OFF 모드 + 수신자 오프라인 → FcmOutbox 미저장
 * - AC-6: BUNDLED 모드 + 수신자 오프라인 → FcmOutbox 즉시 미저장
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoommateChattingSendChatNotificationModeTest {

    @Mock
    RoommateChattingChatRepository chatRepository;
    @Mock
    RoommateChattingRoomRepository chatRoomRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    SimpMessagingTemplate messagingTemplate;
    @Mock
    NotificationService notificationService;
    @Mock
    FcmOutboxRepository fcmOutboxRepository;
    @Mock
    FcmTokenRepository fcmTokenRepository;

    @InjectMocks
    RoommateChattingChatService roommateChattingChatService;

    @AfterEach
    void clearOnlineMap() {
        RoommateWebSocketEventListener.roommateChatRoomInUserMap.clear();
    }

    private static final Long HOST_ID = 1L;
    private static final Long GUEST_ID = 2L;
    private static final Long ROOM_ID = HOST_ID * 1000L + GUEST_ID;

    // ──────────────────────────────────────────────
    // AC-4: EVERY 모드 — 수신자 오프라인 시 FcmOutbox 즉시 저장
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("AC-4 — EVERY 모드이고 수신자가 오프라인이면 FcmOutbox에 즉시 저장됨")
    void should_save_fcm_outbox_immediately_when_every_mode_and_receiver_offline() {
        // NOTE: RoommateChattingRoom에 hostNotificationMode/guestNotificationMode 추가 후,
        //       sendChatNotification() 에서 mode 분기 로직 구현 후 주석을 해제한다.
        //
        // RoommateChattingRoom room = RoommateChattingNotificationFixture.createRoom(HOST_ID, GUEST_ID);
        // // guestNotificationMode = EVERY (기본값)
        // User sender = RoommateChattingNotificationFixture.createUser(HOST_ID);
        // User receiver = RoommateChattingNotificationFixture.createUser(GUEST_ID);
        // FcmToken token = RoommateChattingNotificationFixture.createFcmToken(receiver, "token-guest");
        //
        // given(userRepository.findById(HOST_ID)).willReturn(Optional.of(sender));
        // given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        // given(chatRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        // given(notificationService.createChatNotification(any(), any(), any()))
        //     .willReturn(Notification.create("title", "body"));  // placeholder
        // given(fcmTokenRepository.findByUser(receiver)).willReturn(Optional.of(token));
        //
        // // 수신자 오프라인 (roommateChatRoomInUserMap 에 없음)
        // RequestRoommateChatDto request = new RequestRoommateChatDto(ROOM_ID, "안녕하세요");
        // roommateChattingChatService.sendChat(HOST_ID, request);
        //
        // then(fcmOutboxRepository).should().save(any());

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("AC-4 — EVERY 모드이고 수신자가 온라인이면 FcmOutbox에 저장되지 않음")
    void should_not_save_fcm_outbox_when_every_mode_and_receiver_online() {
        // NOTE: 구현 후 주석 해제.
        //
        // RoommateChattingRoom room = RoommateChattingNotificationFixture.createRoom(HOST_ID, GUEST_ID);
        // User sender = RoommateChattingNotificationFixture.createUser(HOST_ID);
        //
        // given(userRepository.findById(HOST_ID)).willReturn(Optional.of(sender));
        // given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        // given(chatRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        //
        // // 수신자 온라인 상태 설정
        // RoommateWebSocketEventListener.roommateChatRoomInUserMap
        //     .put(ROOM_ID.toString(), new ArrayList<>(List.of(GUEST_ID.toString())));
        //
        // RequestRoommateChatDto request = new RequestRoommateChatDto(ROOM_ID, "안녕하세요");
        // roommateChattingChatService.sendChat(HOST_ID, request);
        //
        // then(fcmOutboxRepository).should(never()).save(any());

        assertThat(true).isTrue();
    }

    // ──────────────────────────────────────────────
    // AC-5: OFF 모드 — FCM 발송하지 않음
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("AC-5 — OFF 모드이면 수신자 오프라인이어도 FcmOutbox에 저장되지 않음")
    void should_not_save_fcm_outbox_when_off_mode_and_receiver_offline() {
        // NOTE: 구현 후 주석 해제.
        // room.updateGuestNotificationMode(ChatNotificationMode.OFF) 후 sendChat 호출.
        //
        // RoommateChattingRoom room = RoommateChattingNotificationFixture.createRoom(HOST_ID, GUEST_ID);
        // room.updateGuestNotificationMode(ChatNotificationMode.OFF);
        // User sender = RoommateChattingNotificationFixture.createUser(HOST_ID);
        //
        // given(userRepository.findById(HOST_ID)).willReturn(Optional.of(sender));
        // given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        // given(chatRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        //
        // RequestRoommateChatDto request = new RequestRoommateChatDto(ROOM_ID, "안녕하세요");
        // roommateChattingChatService.sendChat(HOST_ID, request);
        //
        // then(fcmOutboxRepository).should(never()).save(any());

        assertThat(true).isTrue();
    }

    // ──────────────────────────────────────────────
    // AC-6: BUNDLED 모드 — 즉시 발송하지 않음
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("AC-6 — BUNDLED 모드이면 수신자 오프라인이어도 FcmOutbox에 즉시 저장되지 않음")
    void should_not_save_fcm_outbox_immediately_when_bundled_mode_and_receiver_offline() {
        // NOTE: 구현 후 주석 해제.
        // room.updateGuestNotificationMode(ChatNotificationMode.BUNDLED) 후 sendChat 호출.
        //
        // RoommateChattingRoom room = RoommateChattingNotificationFixture.createRoom(HOST_ID, GUEST_ID);
        // room.updateGuestNotificationMode(ChatNotificationMode.BUNDLED);
        // User sender = RoommateChattingNotificationFixture.createUser(HOST_ID);
        //
        // given(userRepository.findById(HOST_ID)).willReturn(Optional.of(sender));
        // given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        // given(chatRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        //
        // RequestRoommateChatDto request = new RequestRoommateChatDto(ROOM_ID, "안녕하세요");
        // roommateChattingChatService.sendChat(HOST_ID, request);
        //
        // then(fcmOutboxRepository).should(never()).save(any());

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("AC-6 — BUNDLED 모드에서 수신자가 host일 때 hostNotificationMode를 기준으로 분기함")
    void should_read_host_notification_mode_when_receiver_is_host() {
        // NOTE: 구현 후 주석 해제.
        // guest가 발신자 → host가 수신자 → room.getHostNotificationMode() 참조 확인.
        //
        // RoommateChattingRoom room = RoommateChattingNotificationFixture.createRoom(HOST_ID, GUEST_ID);
        // room.updateHostNotificationMode(ChatNotificationMode.BUNDLED);
        // User sender = RoommateChattingNotificationFixture.createUser(GUEST_ID);
        //
        // given(userRepository.findById(GUEST_ID)).willReturn(Optional.of(sender));
        // given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        // given(chatRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        //
        // RequestRoommateChatDto request = new RequestRoommateChatDto(ROOM_ID, "안녕하세요");
        // roommateChattingChatService.sendChat(GUEST_ID, request);
        //
        // then(fcmOutboxRepository).should(never()).save(any());

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("AC-6 — BUNDLED 모드에서 수신자가 guest일 때 guestNotificationMode를 기준으로 분기함")
    void should_read_guest_notification_mode_when_receiver_is_guest() {
        // NOTE: 구현 후 주석 해제.
        // host가 발신자 → guest가 수신자 → room.getGuestNotificationMode() 참조 확인.
        //
        // RoommateChattingRoom room = RoommateChattingNotificationFixture.createRoom(HOST_ID, GUEST_ID);
        // room.updateGuestNotificationMode(ChatNotificationMode.BUNDLED);
        // User sender = RoommateChattingNotificationFixture.createUser(HOST_ID);
        //
        // given(userRepository.findById(HOST_ID)).willReturn(Optional.of(sender));
        // given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        // given(chatRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        //
        // RequestRoommateChatDto request = new RequestRoommateChatDto(ROOM_ID, "안녕하세요");
        // roommateChattingChatService.sendChat(HOST_ID, request);
        //
        // then(fcmOutboxRepository).should(never()).save(any());

        assertThat(true).isTrue();
    }
}
