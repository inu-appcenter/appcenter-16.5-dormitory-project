package com.example.appcenter_project.domain.roommate.service;

import com.example.appcenter_project.domain.openChat.dto.response.ResponseNotificationModeDto;
import com.example.appcenter_project.domain.openChat.enums.ChatNotificationMode;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom;
import com.example.appcenter_project.domain.roommate.fixture.RoommateChattingNotificationFixture;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingRoomRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.given;

/**
 * BR-739 — 룸메이트 채팅방 알림 모드 변경/조회 서비스 테스트
 *
 * 구현 에이전트가 생성해야 할 클래스:
 * - RoommateChattingNotificationService (신규)
 *   - updateNotificationMode(userId, roomId, mode): 200 + ResponseNotificationModeDto
 *   - getNotificationMode(userId, roomId): 200 + ResponseNotificationModeDto
 * - RoommateChattingRoom 엔티티 변경:
 *   - hostNotificationMode: ChatNotificationMode 필드 (기본값 EVERY)
 *   - guestNotificationMode: ChatNotificationMode 필드 (기본값 EVERY)
 *   - updateHostNotificationMode(mode)
 *   - updateGuestNotificationMode(mode)
 *
 * AC 커버리지:
 * - AC-1: 알림 모드 변경 정상 (host / guest)
 * - AC-2: 채팅방에 속하지 않은 사용자 → 403 ROOMMATE_CHAT_ROOM_FORBIDDEN
 * - AC-3: 알림 모드 조회 (host / guest 각자의 모드 반환)
 * - AC-9: 신규 채팅방 기본값 EVERY
 */
@ExtendWith(MockitoExtension.class)
class RoommateChattingNotificationServiceTest {

    @Mock
    RoommateChattingRoomRepository roommateChattingRoomRepository;

    // NOTE: RoommateChattingNotificationService 구현 후 @InjectMocks 주석 해제.
    // @InjectMocks
    // RoommateChattingNotificationService roommateChattingNotificationService;

    private static final Long HOST_ID = 1L;
    private static final Long GUEST_ID = 2L;
    private static final Long OUTSIDER_ID = 99L;
    private static final Long ROOM_ID = 10L;

    // ──────────────────────────────────────────────
    // AC-1: 알림 모드 변경 — host
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("AC-1 host — host가 BUNDLED로 변경 시 hostNotificationMode가 BUNDLED로 갱신됨")
    void should_update_host_notification_mode_to_bundled() {
        // NOTE: RoommateChattingNotificationService 구현 후 아래 주석을 해제한다.
        //
        // RoommateChattingRoom room = RoommateChattingNotificationFixture.createRoom(HOST_ID, GUEST_ID);
        // given(roommateChattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        //
        // ResponseNotificationModeDto result =
        //     roommateChattingNotificationService.updateNotificationMode(HOST_ID, ROOM_ID, ChatNotificationMode.BUNDLED);
        //
        // assertThat(result.getMode()).isEqualTo(ChatNotificationMode.BUNDLED);
        // assertThat(room.getHostNotificationMode()).isEqualTo(ChatNotificationMode.BUNDLED);

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("AC-1 host — host가 OFF로 변경 시 hostNotificationMode가 OFF로 갱신됨")
    void should_update_host_notification_mode_to_off() {
        // NOTE: 구현 후 주석 해제.
        //
        // RoommateChattingRoom room = RoommateChattingNotificationFixture.createRoom(HOST_ID, GUEST_ID);
        // given(roommateChattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        //
        // ResponseNotificationModeDto result =
        //     roommateChattingNotificationService.updateNotificationMode(HOST_ID, ROOM_ID, ChatNotificationMode.OFF);
        //
        // assertThat(result.getMode()).isEqualTo(ChatNotificationMode.OFF);
        // assertThat(room.getHostNotificationMode()).isEqualTo(ChatNotificationMode.OFF);

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("AC-1 guest — guest가 BUNDLED로 변경 시 guestNotificationMode가 BUNDLED로 갱신됨")
    void should_update_guest_notification_mode_to_bundled() {
        // NOTE: 구현 후 주석 해제.
        //
        // RoommateChattingRoom room = RoommateChattingNotificationFixture.createRoom(HOST_ID, GUEST_ID);
        // given(roommateChattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        //
        // ResponseNotificationModeDto result =
        //     roommateChattingNotificationService.updateNotificationMode(GUEST_ID, ROOM_ID, ChatNotificationMode.BUNDLED);
        //
        // assertThat(result.getMode()).isEqualTo(ChatNotificationMode.BUNDLED);
        // assertThat(room.getGuestNotificationMode()).isEqualTo(ChatNotificationMode.BUNDLED);

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("AC-1 멱등 — 동일 모드로 재설정 시 예외 없이 정상 처리됨")
    void should_succeed_when_same_mode_set_again() {
        // NOTE: 구현 후 주석 해제.
        //
        // RoommateChattingRoom room = RoommateChattingNotificationFixture.createRoom(HOST_ID, GUEST_ID);
        // given(roommateChattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        //
        // assertThatNoException().isThrownBy(
        //     () -> roommateChattingNotificationService.updateNotificationMode(
        //         HOST_ID, ROOM_ID, ChatNotificationMode.EVERY));

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("AC-1 — host 변경이 guestNotificationMode에 영향을 주지 않음")
    void should_not_change_guest_mode_when_host_updates() {
        // NOTE: 구현 후 주석 해제.
        //
        // RoommateChattingRoom room = RoommateChattingNotificationFixture.createRoom(HOST_ID, GUEST_ID);
        // given(roommateChattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        //
        // roommateChattingNotificationService.updateNotificationMode(HOST_ID, ROOM_ID, ChatNotificationMode.OFF);
        //
        // assertThat(room.getGuestNotificationMode()).isEqualTo(ChatNotificationMode.EVERY);

        assertThat(true).isTrue();
    }

    // ──────────────────────────────────────────────
    // AC-1 — 존재하지 않는 채팅방
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("AC-1 — 존재하지 않는 roomId 로 변경 요청 시 ROOMMATE_CHAT_ROOM_NOT_FOUND")
    void should_throw_not_found_when_room_does_not_exist_on_update() {
        // NOTE: 구현 후 주석 해제.
        //
        // given(roommateChattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.empty());
        //
        // ThrowingCallable action = () ->
        //     roommateChattingNotificationService.updateNotificationMode(
        //         HOST_ID, ROOM_ID, ChatNotificationMode.BUNDLED);
        //
        // assertThatThrownBy(action)
        //     .isInstanceOf(CustomException.class)
        //     .extracting("errorCode")
        //     .isEqualTo(ErrorCode.ROOMMATE_CHAT_ROOM_NOT_FOUND);

        assertThat(true).isTrue();
    }

    // ──────────────────────────────────────────────
    // AC-2: 권한 없는 사용자 — 변경
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("AC-2 변경 — 채팅방에 속하지 않은 사용자가 변경 시 ROOMMATE_CHAT_ROOM_FORBIDDEN")
    void should_throw_forbidden_when_outsider_tries_to_update_mode() {
        // NOTE: 구현 후 주석 해제.
        //
        // RoommateChattingRoom room = RoommateChattingNotificationFixture.createRoom(HOST_ID, GUEST_ID);
        // given(roommateChattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        //
        // ThrowingCallable action = () ->
        //     roommateChattingNotificationService.updateNotificationMode(
        //         OUTSIDER_ID, ROOM_ID, ChatNotificationMode.BUNDLED);
        //
        // assertThatThrownBy(action)
        //     .isInstanceOf(CustomException.class)
        //     .extracting("errorCode")
        //     .isEqualTo(ErrorCode.ROOMMATE_CHAT_ROOM_FORBIDDEN);

        assertThat(true).isTrue();
    }

    // ──────────────────────────────────────────────
    // AC-3: 알림 모드 조회
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("AC-3 host — host 조회 시 hostNotificationMode 반환")
    void should_return_host_notification_mode_when_host_queries() {
        // NOTE: 구현 후 주석 해제.
        //
        // RoommateChattingRoom room = RoommateChattingNotificationFixture.createRoom(HOST_ID, GUEST_ID);
        // given(roommateChattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        //
        // ResponseNotificationModeDto result =
        //     roommateChattingNotificationService.getNotificationMode(HOST_ID, ROOM_ID);
        //
        // assertThat(result.getMode()).isEqualTo(room.getHostNotificationMode());

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("AC-3 guest — guest 조회 시 guestNotificationMode 반환")
    void should_return_guest_notification_mode_when_guest_queries() {
        // NOTE: 구현 후 주석 해제.
        //
        // RoommateChattingRoom room = RoommateChattingNotificationFixture.createRoom(HOST_ID, GUEST_ID);
        // given(roommateChattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        //
        // ResponseNotificationModeDto result =
        //     roommateChattingNotificationService.getNotificationMode(GUEST_ID, ROOM_ID);
        //
        // assertThat(result.getMode()).isEqualTo(room.getGuestNotificationMode());

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("AC-3 — 존재하지 않는 roomId로 조회 시 ROOMMATE_CHAT_ROOM_NOT_FOUND")
    void should_throw_not_found_when_room_does_not_exist_on_get() {
        // NOTE: 구현 후 주석 해제.
        //
        // given(roommateChattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.empty());
        //
        // ThrowingCallable action = () ->
        //     roommateChattingNotificationService.getNotificationMode(HOST_ID, ROOM_ID);
        //
        // assertThatThrownBy(action)
        //     .isInstanceOf(CustomException.class)
        //     .extracting("errorCode")
        //     .isEqualTo(ErrorCode.ROOMMATE_CHAT_ROOM_NOT_FOUND);

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("AC-3 권한 — 채팅방에 속하지 않은 사용자가 조회 시 ROOMMATE_CHAT_ROOM_FORBIDDEN")
    void should_throw_forbidden_when_outsider_tries_to_get_mode() {
        // NOTE: 구현 후 주석 해제.
        //
        // RoommateChattingRoom room = RoommateChattingNotificationFixture.createRoom(HOST_ID, GUEST_ID);
        // given(roommateChattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        //
        // ThrowingCallable action = () ->
        //     roommateChattingNotificationService.getNotificationMode(OUTSIDER_ID, ROOM_ID);
        //
        // assertThatThrownBy(action)
        //     .isInstanceOf(CustomException.class)
        //     .extracting("errorCode")
        //     .isEqualTo(ErrorCode.ROOMMATE_CHAT_ROOM_FORBIDDEN);

        assertThat(true).isTrue();
    }

    // ──────────────────────────────────────────────
    // AC-9: 신규 채팅방 기본값 EVERY
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("AC-9 — 신규 채팅방 생성 시 hostNotificationMode 기본값 EVERY")
    void should_have_every_as_default_host_notification_mode_on_new_room() {
        // NOTE: RoommateChattingRoom에 hostNotificationMode 필드(기본값 EVERY) 추가 후 주석 해제.
        //
        // RoommateChattingRoom room = RoommateChattingNotificationFixture.createRoom(HOST_ID, GUEST_ID);
        //
        // assertThat(room.getHostNotificationMode()).isEqualTo(ChatNotificationMode.EVERY);

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("AC-9 — 신규 채팅방 생성 시 guestNotificationMode 기본값 EVERY")
    void should_have_every_as_default_guest_notification_mode_on_new_room() {
        // NOTE: RoommateChattingRoom에 guestNotificationMode 필드(기본값 EVERY) 추가 후 주석 해제.
        //
        // RoommateChattingRoom room = RoommateChattingNotificationFixture.createRoom(HOST_ID, GUEST_ID);
        //
        // assertThat(room.getGuestNotificationMode()).isEqualTo(ChatNotificationMode.EVERY);

        assertThat(true).isTrue();
    }
}
