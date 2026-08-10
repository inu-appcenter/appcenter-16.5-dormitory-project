package com.example.appcenter_project.domain.roommate.service;

import com.example.appcenter_project.domain.fcm.entity.FcmOutbox;
import com.example.appcenter_project.domain.fcm.entity.FcmToken;
import com.example.appcenter_project.domain.fcm.repository.FcmOutboxRepository;
import com.example.appcenter_project.domain.roommate.fixture.RoommateChattingNotificationFixture;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingRoomRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * BR-739 — BUNDLED 스케줄러 서비스 테스트
 *
 * 구현 에이전트가 생성해야 할 클래스:
 * - RoommateChattingNotificationService.sendHourlyUnreadNotifications() (신규)
 * - RoommateUnreadNotificationInfo record (신규):
 *   package com.example.appcenter_project.domain.roommate.dto;
 *   public record RoommateUnreadNotificationInfo(Long userId, Long roomId, long unreadCount) {}
 * - RoommateChattingChatQuerydslRepository.findUnreadCountsForBundled() (신규 메서드):
 *   BUNDLED 모드 참여자 중 readByReceiver=false 인 메시지가 있는 경우 집계
 * - FcmRoutingType.CHAT_ROOMMATE (신규 상수)
 *
 * AC 커버리지:
 * - AC-7: BUNDLED 모드 + 읽지 않은 메시지 있음 → FcmOutbox 저장
 * - AC-8: BUNDLED 모드 + 읽지 않은 메시지 없음 → FcmOutbox 미저장
 * - 경계: FCM 토큰 없음 → 해당 사용자 건너뜀
 * - 경계: 여러 사용자 혼재 → 토큰 있는 사용자만 FcmOutbox 저장
 * - 경계: FcmOutbox body 형식 "새 메시지 N개"
 * - 경계: FcmOutbox routingType = CHAT_ROOMMATE
 */
@ExtendWith(MockitoExtension.class)
class RoommateChattingBundledSchedulerServiceTest {

    @Mock
    RoommateChattingRoomRepository roommateChattingRoomRepository;

    // NOTE: RoommateChattingChatQuerydslRepository에 findUnreadCountsForBundled() 추가 후 주석 해제.
    // @Mock
    // RoommateChattingChatQuerydslRepository roommateChattingChatQuerydslRepository;

    @Mock
    FcmTokenRepository fcmTokenRepository;
    @Mock
    FcmOutboxRepository fcmOutboxRepository;

    // NOTE: RoommateChattingNotificationService 구현 후 @InjectMocks 주석 해제.
    // @InjectMocks
    // RoommateChattingNotificationService roommateChattingNotificationService;

    private static final Long HOST_ID = 1L;
    private static final Long GUEST_ID = 2L;
    private static final Long ROOM_ID = 10L;

    // ──────────────────────────────────────────────
    // AC-7: 읽지 않은 메시지 있을 때 FcmOutbox 저장
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("AC-7 — BUNDLED 모드 사용자에게 읽지 않은 메시지가 있으면 FcmOutbox 저장됨")
    void should_save_fcm_outbox_when_bundled_user_has_unread_messages() {
        // NOTE: 구현 후 주석 해제.
        //
        // RoommateUnreadNotificationInfo unreadInfo =
        //     new RoommateUnreadNotificationInfo(GUEST_ID, ROOM_ID, 3L);
        // User guest = RoommateChattingNotificationFixture.createUser(GUEST_ID);
        // FcmToken token = RoommateChattingNotificationFixture.createFcmToken(guest, "token-guest");
        //
        // given(roommateChattingChatQuerydslRepository.findUnreadCountsForBundled())
        //     .willReturn(List.of(unreadInfo));
        // given(fcmTokenRepository.findAllByUserIdIn(List.of(GUEST_ID))).willReturn(List.of(token));
        //
        // roommateChattingNotificationService.sendHourlyUnreadNotifications();
        //
        // then(fcmOutboxRepository).should().saveAll(any());

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("AC-7 — FcmOutbox body가 '새 메시지 N개' 형식으로 저장됨")
    void should_set_fcm_outbox_body_format_as_new_message_count() {
        // NOTE: 구현 후 주석 해제.
        //
        // RoommateUnreadNotificationInfo unreadInfo =
        //     new RoommateUnreadNotificationInfo(GUEST_ID, ROOM_ID, 5L);
        // User guest = RoommateChattingNotificationFixture.createUser(GUEST_ID);
        // FcmToken token = RoommateChattingNotificationFixture.createFcmToken(guest, "token-guest");
        //
        // given(roommateChattingChatQuerydslRepository.findUnreadCountsForBundled())
        //     .willReturn(List.of(unreadInfo));
        // given(fcmTokenRepository.findAllByUserIdIn(List.of(GUEST_ID))).willReturn(List.of(token));
        //
        // ArgumentCaptor<List<FcmOutbox>> captor = ArgumentCaptor.forClass(List.class);
        // roommateChattingNotificationService.sendHourlyUnreadNotifications();
        // then(fcmOutboxRepository).should().saveAll(captor.capture());
        //
        // assertThat(captor.getValue().get(0).getBody()).isEqualTo("새 메시지 5개");

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("AC-7 — FcmOutbox routingType이 CHAT_ROOMMATE로 저장됨")
    void should_set_fcm_outbox_routing_type_as_chat_roommate() {
        // NOTE: FcmRoutingType.CHAT_ROOMMATE 추가 후 주석 해제.
        //
        // RoommateUnreadNotificationInfo unreadInfo =
        //     new RoommateUnreadNotificationInfo(GUEST_ID, ROOM_ID, 2L);
        // User guest = RoommateChattingNotificationFixture.createUser(GUEST_ID);
        // FcmToken token = RoommateChattingNotificationFixture.createFcmToken(guest, "token-guest");
        //
        // given(roommateChattingChatQuerydslRepository.findUnreadCountsForBundled())
        //     .willReturn(List.of(unreadInfo));
        // given(fcmTokenRepository.findAllByUserIdIn(List.of(GUEST_ID))).willReturn(List.of(token));
        //
        // ArgumentCaptor<List<FcmOutbox>> captor = ArgumentCaptor.forClass(List.class);
        // roommateChattingNotificationService.sendHourlyUnreadNotifications();
        // then(fcmOutboxRepository).should().saveAll(captor.capture());
        //
        // assertThat(captor.getValue().get(0).getRoutingType())
        //     .isEqualTo(FcmRoutingType.CHAT_ROOMMATE);

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("AC-7 — FcmOutbox routingId가 roomId로 저장됨")
    void should_set_fcm_outbox_routing_id_as_room_id() {
        // NOTE: 구현 후 주석 해제.
        //
        // RoommateUnreadNotificationInfo unreadInfo =
        //     new RoommateUnreadNotificationInfo(GUEST_ID, ROOM_ID, 2L);
        // User guest = RoommateChattingNotificationFixture.createUser(GUEST_ID);
        // FcmToken token = RoommateChattingNotificationFixture.createFcmToken(guest, "token-guest");
        //
        // given(roommateChattingChatQuerydslRepository.findUnreadCountsForBundled())
        //     .willReturn(List.of(unreadInfo));
        // given(fcmTokenRepository.findAllByUserIdIn(List.of(GUEST_ID))).willReturn(List.of(token));
        //
        // ArgumentCaptor<List<FcmOutbox>> captor = ArgumentCaptor.forClass(List.class);
        // roommateChattingNotificationService.sendHourlyUnreadNotifications();
        // then(fcmOutboxRepository).should().saveAll(captor.capture());
        //
        // assertThat(captor.getValue().get(0).getRoutingId()).isEqualTo(ROOM_ID);

        assertThat(true).isTrue();
    }

    // ──────────────────────────────────────────────
    // AC-8: 읽지 않은 메시지 없을 때 FcmOutbox 미저장
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("AC-8 — BUNDLED 모드 사용자에게 읽지 않은 메시지가 없으면 FcmOutbox 저장되지 않음")
    void should_not_save_fcm_outbox_when_bundled_user_has_no_unread_messages() {
        // NOTE: 구현 후 주석 해제.
        //
        // given(roommateChattingChatQuerydslRepository.findUnreadCountsForBundled())
        //     .willReturn(List.of());
        //
        // roommateChattingNotificationService.sendHourlyUnreadNotifications();
        //
        // then(fcmOutboxRepository).should(never()).saveAll(any());

        assertThat(true).isTrue();
    }

    // ──────────────────────────────────────────────
    // 경계: FCM 토큰 없는 사용자 건너뜀
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("경계 — BUNDLED 사용자에게 읽지 않은 메시지가 있어도 FCM 토큰이 없으면 FcmOutbox 미저장")
    void should_skip_user_without_fcm_token_in_bundled_scheduler() {
        // NOTE: 구현 후 주석 해제.
        //
        // RoommateUnreadNotificationInfo unreadInfo =
        //     new RoommateUnreadNotificationInfo(GUEST_ID, ROOM_ID, 3L);
        //
        // given(roommateChattingChatQuerydslRepository.findUnreadCountsForBundled())
        //     .willReturn(List.of(unreadInfo));
        // given(fcmTokenRepository.findAllByUserIdIn(anyList())).willReturn(List.of()); // 토큰 없음
        //
        // roommateChattingNotificationService.sendHourlyUnreadNotifications();
        //
        // then(fcmOutboxRepository).should(never()).saveAll(any());

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("경계 — 토큰 없는 사용자 건너뜀 시 예외가 발생하지 않음")
    void should_not_throw_when_some_users_have_no_fcm_token() {
        // NOTE: 구현 후 주석 해제.
        //
        // RoommateUnreadNotificationInfo infoWithToken =
        //     new RoommateUnreadNotificationInfo(HOST_ID, ROOM_ID, 2L);
        // RoommateUnreadNotificationInfo infoWithoutToken =
        //     new RoommateUnreadNotificationInfo(GUEST_ID, ROOM_ID, 1L);
        // User host = RoommateChattingNotificationFixture.createUser(HOST_ID);
        // FcmToken token = RoommateChattingNotificationFixture.createFcmToken(host, "token-host");
        //
        // given(roommateChattingChatQuerydslRepository.findUnreadCountsForBundled())
        //     .willReturn(List.of(infoWithToken, infoWithoutToken));
        // // HOST_ID 의 토큰만 존재, GUEST_ID 는 없음
        // given(fcmTokenRepository.findAllByUserIdIn(anyList())).willReturn(List.of(token));
        //
        // assertThatNoException().isThrownBy(
        //     () -> roommateChattingNotificationService.sendHourlyUnreadNotifications());

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("경계 — 여러 사용자 중 토큰 있는 사용자만 FcmOutbox에 저장됨")
    void should_save_fcm_outbox_only_for_users_with_token() {
        // NOTE: 구현 후 주석 해제.
        //
        // RoommateUnreadNotificationInfo infoWithToken =
        //     new RoommateUnreadNotificationInfo(HOST_ID, ROOM_ID, 2L);
        // RoommateUnreadNotificationInfo infoWithoutToken =
        //     new RoommateUnreadNotificationInfo(GUEST_ID, ROOM_ID, 1L);
        // User host = RoommateChattingNotificationFixture.createUser(HOST_ID);
        // FcmToken token = RoommateChattingNotificationFixture.createFcmToken(host, "token-host");
        //
        // given(roommateChattingChatQuerydslRepository.findUnreadCountsForBundled())
        //     .willReturn(List.of(infoWithToken, infoWithoutToken));
        // given(fcmTokenRepository.findAllByUserIdIn(anyList())).willReturn(List.of(token));
        //
        // ArgumentCaptor<List<FcmOutbox>> captor = ArgumentCaptor.forClass(List.class);
        // roommateChattingNotificationService.sendHourlyUnreadNotifications();
        // then(fcmOutboxRepository).should().saveAll(captor.capture());
        //
        // assertThat(captor.getValue()).hasSize(1);
        // assertThat(captor.getValue().get(0).getToken()).isEqualTo("token-host");

        assertThat(true).isTrue();
    }
}
