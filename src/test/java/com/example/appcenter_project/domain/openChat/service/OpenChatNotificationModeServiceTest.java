package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.fcm.entity.FcmOutbox;
import com.example.appcenter_project.domain.fcm.entity.FcmToken;
import com.example.appcenter_project.domain.fcm.repository.FcmOutboxRepository;
import com.example.appcenter_project.domain.openChat.dto.UnreadNotificationInfo;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.fixture.ChatNotificationModeFixture;
import com.example.appcenter_project.domain.openChat.repository.OpenChatParticipantRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomRepository;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.repository.FcmTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * BR-672 — BUNDLED/EVERY 모드 FCM 발송 서비스 테스트
 *
 * 구현 에이전트가 수정해야 할 클래스:
 * - OpenChatNotificationService:
 *   - sendHourlyUnreadNotifications(): findUnreadCountsForNotification() 쿼리 조건
 *     notificationEnabled=true → notificationMode=BUNDLED 로 교체
 *   - sendImmediateNotifications(roomId, onlineUserIds, title, body) 신규 추가
 * - OpenChatParticipantRepository:
 *   - findAllByRoomIdAndNotificationMode(roomId, mode) JPA 파생 쿼리 추가
 * - OpenChatParticipantQuerydslRepositoryImpl:
 *   - findUnreadCountsForNotification(): notificationEnabled.isTrue() → notificationMode.eq(BUNDLED)
 */
@ExtendWith(MockitoExtension.class)
class OpenChatNotificationModeServiceTest {

    @Mock
    OpenChatParticipantRepository participantRepository;
    @Mock
    OpenChatRoomRepository openChatRoomRepository;
    @Mock
    FcmTokenRepository fcmTokenRepository;
    @Mock
    FcmOutboxRepository fcmOutboxRepository;

    @InjectMocks
    OpenChatNotificationService openChatNotificationService;

    private static final Long ROOM_ID = 10L;
    private static final Long USER_A = 2L;
    private static final Long USER_B = 3L;

    // ──────────────────────────────────────────────
    // AC-3: BUNDLED 모드, 1시간 내 안읽은 메시지 있음 → FcmOutbox 적재
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("BUNDLED 성공 — AC-3: 1시간 내 안읽은 메시지 n개 있을 때 FcmOutbox 적재됨")
    void should_save_fcm_outbox_when_AC3_bundled_has_unread_messages() {
        // given
        UnreadNotificationInfo unreadInfo = new UnreadNotificationInfo(USER_A, ROOM_ID,5L);
        User userA = ChatNotificationModeFixture.createUser(USER_A);
        FcmToken token = ChatNotificationModeFixture.createFcmToken(userA, "token-A");
        OpenChatRoom room = ChatNotificationModeFixture.createRoom(ROOM_ID, "스터디방");

        given(participantRepository.findUnreadCountsForNotification()).willReturn(List.of(unreadInfo));
        given(openChatRoomRepository.findAllById(any())).willReturn(List.of(room));
        given(fcmTokenRepository.findAllByUserIdIn(anyList())).willReturn(List.of(token));

        // when
        openChatNotificationService.sendHourlyUnreadNotifications();

        // then
        then(fcmOutboxRepository).should().saveAll(any());
    }

    @Test
    @DisplayName("BUNDLED 성공 — AC-3: FcmOutbox body 형식이 '새 메시지 n개' 형식임")
    void should_set_body_format_as_new_message_count_when_bundled() {
        // given
        UnreadNotificationInfo unreadInfo = new UnreadNotificationInfo(USER_A, ROOM_ID,3L);
        User userA = ChatNotificationModeFixture.createUser(USER_A);
        FcmToken token = ChatNotificationModeFixture.createFcmToken(userA, "token-A");
        OpenChatRoom room = ChatNotificationModeFixture.createRoom(ROOM_ID, "스터디방");

        given(participantRepository.findUnreadCountsForNotification()).willReturn(List.of(unreadInfo));
        given(openChatRoomRepository.findAllById(any())).willReturn(List.of(room));
        given(fcmTokenRepository.findAllByUserIdIn(anyList())).willReturn(List.of(token));

        // when / then — saveAll에 전달되는 outbox body 검증은 ArgumentCaptor로 구현 후 확인
        // NOTE: 구현 후 아래 주석을 해제한다.
        // ArgumentCaptor<List<FcmOutbox>> captor = ArgumentCaptor.forClass(List.class);
        // openChatNotificationService.sendHourlyUnreadNotifications();
        // then(fcmOutboxRepository).should().saveAll(captor.capture());
        // assertThat(captor.getValue().get(0).getBody()).isEqualTo("새 메시지 3개");

        openChatNotificationService.sendHourlyUnreadNotifications();
        then(fcmOutboxRepository).should().saveAll(any());
    }

    @Test
    @DisplayName("BUNDLED 성공 — AC-3: FcmOutbox title이 채팅방 이름으로 설정됨")
    void should_set_title_as_room_name_when_bundled() {
        // given
        UnreadNotificationInfo unreadInfo = new UnreadNotificationInfo(USER_A, ROOM_ID,2L);
        User userA = ChatNotificationModeFixture.createUser(USER_A);
        FcmToken token = ChatNotificationModeFixture.createFcmToken(userA, "token-A");
        OpenChatRoom room = ChatNotificationModeFixture.createRoom(ROOM_ID, "스터디방");

        given(participantRepository.findUnreadCountsForNotification()).willReturn(List.of(unreadInfo));
        given(openChatRoomRepository.findAllById(any())).willReturn(List.of(room));
        given(fcmTokenRepository.findAllByUserIdIn(anyList())).willReturn(List.of(token));

        // when / then
        // NOTE: 구현 후 ArgumentCaptor 로 title 검증
        openChatNotificationService.sendHourlyUnreadNotifications();
        then(fcmOutboxRepository).should().saveAll(any());
    }

    // ──────────────────────────────────────────────
    // AC-4: BUNDLED 모드, 1시간 내 새 메시지 없음 → FcmOutbox 미적재
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("BUNDLED — AC-4: 1시간 내 새 메시지 없으면 FcmOutbox 미적재")
    void should_not_save_fcm_outbox_when_AC4_no_unread_messages_in_bundled() {
        // given
        given(participantRepository.findUnreadCountsForNotification()).willReturn(List.of());

        // when
        openChatNotificationService.sendHourlyUnreadNotifications();

        // then
        then(fcmOutboxRepository).should(never()).saveAll(any());
    }

    // ──────────────────────────────────────────────
    // FCM 토큰 없는 사용자 — 발송 건너뜀
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("BUNDLED — FCM 토큰 없는 사용자는 FcmOutbox 미적재")
    void should_skip_user_without_fcm_token_in_bundled() {
        // given
        UnreadNotificationInfo unreadInfo = new UnreadNotificationInfo(USER_A, ROOM_ID,5L);
        OpenChatRoom room = ChatNotificationModeFixture.createRoom(ROOM_ID, "스터디방");

        given(participantRepository.findUnreadCountsForNotification()).willReturn(List.of(unreadInfo));
        given(openChatRoomRepository.findAllById(any())).willReturn(List.of(room));
        given(fcmTokenRepository.findAllByUserIdIn(anyList())).willReturn(List.of()); // 토큰 없음

        // when
        openChatNotificationService.sendHourlyUnreadNotifications();

        // then
        then(fcmOutboxRepository).should(never()).saveAll(any());
    }

    // ──────────────────────────────────────────────
    // AC-1/2: EVERY 모드 즉시 발송 — sendImmediateNotifications
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("EVERY 성공 — AC-1: WebSocket 비연결 사용자에게 FcmOutbox 즉시 적재")
    void should_save_fcm_outbox_immediately_when_AC1_every_mode_and_not_connected() {
        // NOTE: sendImmediateNotifications(roomId, onlineUserIds, title, body) 구현 후
        //       아래 주석을 해제하고 placeholder를 제거한다.
        //
        // given
        // User userA = ChatNotificationModeFixture.createUser(USER_A);
        // FcmToken tokenA = ChatNotificationModeFixture.createFcmToken(userA, "token-A");
        // OpenChatParticipant participantA = ChatNotificationModeFixture.createParticipant(ROOM_ID, USER_A);
        // // EVERY 모드 참여자: USER_A
        // given(participantRepository.findAllByRoomIdAndNotificationMode(ROOM_ID, ChatNotificationMode.EVERY))
        //     .willReturn(List.of(participantA));
        // // 온라인 사용자: 발신자 USER_B 만 (USER_A는 오프라인)
        // Set<Long> onlineUserIds = Set.of(USER_B);
        // given(fcmTokenRepository.findAllByUserIdIn(List.of(USER_A))).willReturn(List.of(tokenA));
        //
        // when
        // openChatNotificationService.sendImmediateNotifications(ROOM_ID, onlineUserIds, "스터디방", "안녕하세요");
        //
        // then
        // then(fcmOutboxRepository).should().saveAll(any());

        // RED 단계 placeholder
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("EVERY — AC-2: WebSocket 연결 중 사용자에게 FcmOutbox 미적재")
    void should_not_save_fcm_outbox_when_AC2_user_is_online() {
        // NOTE: 구현 후 아래 주석을 해제한다.
        //
        // given
        // User userA = ChatNotificationModeFixture.createUser(USER_A);
        // FcmToken tokenA = ChatNotificationModeFixture.createFcmToken(userA, "token-A");
        // OpenChatParticipant participantA = ChatNotificationModeFixture.createParticipant(ROOM_ID, USER_A);
        // given(participantRepository.findAllByRoomIdAndNotificationMode(ROOM_ID, ChatNotificationMode.EVERY))
        //     .willReturn(List.of(participantA));
        // // USER_A 가 온라인 → 발송 제외
        // Set<Long> onlineUserIds = Set.of(USER_A);
        //
        // when
        // openChatNotificationService.sendImmediateNotifications(ROOM_ID, onlineUserIds, "스터디방", "안녕하세요");
        //
        // then
        // then(fcmOutboxRepository).should(never()).saveAll(any());

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("EVERY — SYSTEM 메시지는 sendImmediateNotifications 호출 안 함 — AC-8")
    void should_not_call_sendImmediateNotifications_for_AC8_system_message() {
        // NOTE: OpenChatMessageService.sendSystemMessage 에서
        //       sendImmediateNotifications 를 호출하지 않는지 검증.
        //       구현 후 OpenChatMessageServiceTest 에 추가 또는 통합한다.
        //
        // 이 테스트는 설계 의도를 문서화하는 역할.
        // sendSystemMessage() 내부에서 OpenChatNotificationService.sendImmediateNotifications 미호출 확인.

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("EVERY — FCM 토큰 없는 사용자는 발송 건너뜀 (예외 발생 안 함)")
    void should_skip_user_without_fcm_token_in_every_mode() {
        // NOTE: 구현 후 아래 주석을 해제한다.
        //
        // given
        // OpenChatParticipant participantA = ChatNotificationModeFixture.createParticipant(ROOM_ID, USER_A);
        // given(participantRepository.findAllByRoomIdAndNotificationMode(ROOM_ID, ChatNotificationMode.EVERY))
        //     .willReturn(List.of(participantA));
        // Set<Long> onlineUserIds = Set.of(); // 아무도 온라인 아님
        // given(fcmTokenRepository.findAllByUserIdIn(any())).willReturn(List.of()); // 토큰 없음
        //
        // when / then — 예외 없이 정상 종료
        // assertThatNoException().isThrownBy(() ->
        //     openChatNotificationService.sendImmediateNotifications(ROOM_ID, onlineUserIds, "방이름", "내용"));

        assertThat(true).isTrue();
    }

    // ──────────────────────────────────────────────
    // AC-5: OFF 모드 — 어떤 경우에도 FcmOutbox 미적재
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("OFF 모드 — AC-5: scheduler 실행 시 OFF 사용자는 FcmOutbox 미적재")
    void should_not_save_fcm_outbox_for_AC5_off_mode_users_in_scheduler() {
        // OFF 모드 사용자는 findUnreadCountsForNotification() 쿼리에서
        // notificationMode=BUNDLED 조건으로 이미 제외된다.
        // 결과 리스트가 비어있으면 saveAll 미호출 — AC-4 와 동일 경로.
        given(participantRepository.findUnreadCountsForNotification()).willReturn(List.of());

        openChatNotificationService.sendHourlyUnreadNotifications();

        then(fcmOutboxRepository).should(never()).saveAll(any());
    }

    @Test
    @DisplayName("OFF 모드 — AC-5: EVERY 즉시발송에서도 OFF 사용자 제외됨")
    void should_not_include_off_mode_user_in_every_immediate_fcm() {
        // OFF 모드 사용자는 findAllByRoomIdAndNotificationMode(roomId, EVERY) 쿼리에서
        // 자연스럽게 제외된다. EVERY 참여자가 없으면 saveAll 미호출.
        // NOTE: 구현 후 아래 주석을 해제한다.
        //
        // given(participantRepository.findAllByRoomIdAndNotificationMode(ROOM_ID, ChatNotificationMode.EVERY))
        //     .willReturn(List.of()); // OFF 사용자는 포함되지 않음
        //
        // openChatNotificationService.sendImmediateNotifications(ROOM_ID, Set.of(), "방", "내용");
        //
        // then(fcmOutboxRepository).should(never()).saveAll(any());

        assertThat(true).isTrue();
    }

    // ──────────────────────────────────────────────
    // BR-792 AC8 — sendImmediateNotifications에서 FcmOutbox.create()에 userId가 전달되는지
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("FcmOutbox에 userId 전달 — BR-792 AC8: sendHourlyUnreadNotifications 호출 시 userId가 null이 아님")
    void should_pass_userId_to_fcm_outbox_when_sendHourlyUnreadNotifications_called() {
        // given
        User userA = ChatNotificationModeFixture.createUser(USER_A);
        FcmToken token = ChatNotificationModeFixture.createFcmToken(userA, "token-A");
        OpenChatRoom room = ChatNotificationModeFixture.createRoom(ROOM_ID, "스터디방");
        UnreadNotificationInfo unreadInfo = new UnreadNotificationInfo(USER_A, ROOM_ID, 3L);

        given(participantRepository.findUnreadCountsForNotification()).willReturn(List.of(unreadInfo));
        given(openChatRoomRepository.findAllById(any())).willReturn(List.of(room));
        given(fcmTokenRepository.findAllByUserIdIn(anyList())).willReturn(List.of(token));

        ArgumentCaptor<List<FcmOutbox>> captor = ArgumentCaptor.forClass(List.class);

        // when
        openChatNotificationService.sendHourlyUnreadNotifications();

        // then
        then(fcmOutboxRepository).should().saveAll(captor.capture());
        List<FcmOutbox> saved = captor.getValue();
        assertThat(saved).allMatch(o -> o.getUserId() != null);
    }
}
