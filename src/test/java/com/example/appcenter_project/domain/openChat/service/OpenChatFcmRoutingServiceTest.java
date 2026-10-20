package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.fcm.entity.FcmOutbox;
import com.example.appcenter_project.domain.fcm.entity.FcmToken;
import com.example.appcenter_project.domain.fcm.enums.FcmRoutingType;
import com.example.appcenter_project.domain.fcm.repository.FcmOutboxRepository;
import com.example.appcenter_project.domain.openChat.dto.UnreadNotificationInfo;
import com.example.appcenter_project.domain.openChat.entity.OpenChatParticipant;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomType;
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
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OpenChatFcmRoutingServiceTest {

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

    @Test
    @DisplayName("FcmOutbox routingType = CHAT_PERSONAL 저장 — AC-7: PERSONAL roomType 즉시 알림 routing")
    void should_save_outbox_with_routingType_CHAT_PERSONAL_when_personal_room_immediate_notification() {
        // given
        Long roomId = 99L;
        Long userId = 1L;
        String title = "채팅방 이름";
        String body = "새 메시지가 도착했습니다.";

        OpenChatParticipant participant = OpenChatParticipant.create(roomId, userId, false);
        FcmToken fcmToken = buildFcmToken(userId, "token-user-1");

        given(participantRepository.findAllByRoomIdAndNotificationMode(eq(roomId), any()))
                .willReturn(List.of(participant));
        given(fcmTokenRepository.findAllByUserIdIn(anyList()))
                .willReturn(List.of(fcmToken));

        ArgumentCaptor<List<FcmOutbox>> captor = ArgumentCaptor.forClass(List.class);

        // when
        openChatNotificationService.sendImmediateNotifications(roomId, OpenChatRoomType.PERSONAL, Set.of(), title, body, 123L);

        // then
        then(fcmOutboxRepository).should().saveAll(captor.capture());
        List<FcmOutbox> saved = captor.getValue();
        assertThat(saved).allMatch(o -> o.getRoutingType() == FcmRoutingType.CHAT_PERSONAL);
    }

    @Test
    @DisplayName("FcmOutbox routingType = CHAT_OPEN 저장 — AC-8: OPEN roomType 즉시 알림 routing")
    void should_save_outbox_with_routingType_CHAT_OPEN_when_open_room_immediate_notification() {
        // given
        Long roomId = 1234L;
        Long userId = 1L;
        String title = "채팅방 이름";
        String body = "새 메시지가 도착했습니다.";

        OpenChatParticipant participant = OpenChatParticipant.create(roomId, userId, false);
        FcmToken fcmToken = buildFcmToken(userId, "token-user-1");

        given(participantRepository.findAllByRoomIdAndNotificationMode(eq(roomId), any()))
                .willReturn(List.of(participant));
        given(fcmTokenRepository.findAllByUserIdIn(anyList()))
                .willReturn(List.of(fcmToken));

        ArgumentCaptor<List<FcmOutbox>> captor = ArgumentCaptor.forClass(List.class);

        // when
        openChatNotificationService.sendImmediateNotifications(roomId, OpenChatRoomType.OPEN, Set.of(), title, body, 123L);

        // then
        then(fcmOutboxRepository).should().saveAll(captor.capture());
        List<FcmOutbox> saved = captor.getValue();
        assertThat(saved).allMatch(o -> o.getRoutingType() == FcmRoutingType.CHAT_OPEN);
    }

    @Test
    @DisplayName("FcmOutbox routingType = CHAT_OPEN 저장 — DERIVED roomType은 CHAT_OPEN과 동일")
    void should_save_outbox_with_routingType_CHAT_OPEN_when_derived_room_immediate_notification() {
        // given
        Long roomId = 500L;
        Long userId = 1L;
        String title = "파생 채팅방";
        String body = "새 메시지가 도착했습니다.";

        OpenChatParticipant participant = OpenChatParticipant.create(roomId, userId, false);
        FcmToken fcmToken = buildFcmToken(userId, "token-user-1");

        given(participantRepository.findAllByRoomIdAndNotificationMode(eq(roomId), any()))
                .willReturn(List.of(participant));
        given(fcmTokenRepository.findAllByUserIdIn(anyList()))
                .willReturn(List.of(fcmToken));

        ArgumentCaptor<List<FcmOutbox>> captor = ArgumentCaptor.forClass(List.class);

        // when
        openChatNotificationService.sendImmediateNotifications(roomId, OpenChatRoomType.DERIVED, Set.of(), title, body, 123L);

        // then
        then(fcmOutboxRepository).should().saveAll(captor.capture());
        List<FcmOutbox> saved = captor.getValue();
        assertThat(saved).allMatch(o -> o.getRoutingType() == FcmRoutingType.CHAT_OPEN);
    }

    @Test
    @DisplayName("FcmOutbox routingId = 99 저장 — AC-7: PERSONAL 즉시 알림 roomId 라우팅 저장")
    void should_save_outbox_with_routingId_roomId_when_personal_immediate_notification_sent() {
        // given
        Long roomId = 99L;
        Long userId = 1L;
        String title = "채팅방 이름";
        String body = "새 메시지가 도착했습니다.";

        OpenChatParticipant participant = OpenChatParticipant.create(roomId, userId, false);
        FcmToken fcmToken = buildFcmToken(userId, "token-user-1");

        given(participantRepository.findAllByRoomIdAndNotificationMode(eq(roomId), any()))
                .willReturn(List.of(participant));
        given(fcmTokenRepository.findAllByUserIdIn(anyList()))
                .willReturn(List.of(fcmToken));

        ArgumentCaptor<List<FcmOutbox>> captor = ArgumentCaptor.forClass(List.class);

        // when
        openChatNotificationService.sendImmediateNotifications(roomId, OpenChatRoomType.PERSONAL, Set.of(), title, body, 123L);

        // then
        then(fcmOutboxRepository).should().saveAll(captor.capture());
        List<FcmOutbox> saved = captor.getValue();
        assertThat(saved).allMatch(o -> o.getRoutingId().equals(99L));
    }

    @Test
    @DisplayName("FcmOutbox routingType = CHAT_PERSONAL 저장 — PERSONAL roomType 묶음 알림 routing")
    void should_save_outbox_with_routingType_CHAT_PERSONAL_when_hourly_personal_notification() {
        // given
        Long roomId = 99L;
        Long userId = 1L;
        UnreadNotificationInfo info = new UnreadNotificationInfo(userId, roomId, 3);

        OpenChatRoom room = OpenChatRoom.createForTest(roomId, "개인 채팅방", OpenChatRoomType.PERSONAL);
        FcmToken fcmToken = buildFcmToken(userId, "token-user-1");

        given(participantRepository.findUnreadCountsForNotification()).willReturn(List.of(info));
        given(openChatRoomRepository.findAllById(any())).willReturn(List.of(room));
        given(fcmTokenRepository.findAllByUserIdIn(anyList())).willReturn(List.of(fcmToken));

        ArgumentCaptor<List<FcmOutbox>> captor = ArgumentCaptor.forClass(List.class);

        // when
        openChatNotificationService.sendHourlyUnreadNotifications();

        // then
        then(fcmOutboxRepository).should().saveAll(captor.capture());
        List<FcmOutbox> saved = captor.getValue();
        assertThat(saved).allMatch(o -> o.getRoutingType() == FcmRoutingType.CHAT_PERSONAL);
    }

    @Test
    @DisplayName("FcmOutbox routingType = CHAT_OPEN 저장 — OPEN roomType 묶음 알림 routing")
    void should_save_outbox_with_routingType_CHAT_OPEN_when_hourly_open_notification() {
        // given
        Long roomId = 1234L;
        Long userId = 1L;
        UnreadNotificationInfo info = new UnreadNotificationInfo(userId, roomId, 5);

        OpenChatRoom room = OpenChatRoom.createForTest(roomId, "오픈 채팅방", OpenChatRoomType.OPEN);
        FcmToken fcmToken = buildFcmToken(userId, "token-user-1");

        given(participantRepository.findUnreadCountsForNotification()).willReturn(List.of(info));
        given(openChatRoomRepository.findAllById(any())).willReturn(List.of(room));
        given(fcmTokenRepository.findAllByUserIdIn(anyList())).willReturn(List.of(fcmToken));

        ArgumentCaptor<List<FcmOutbox>> captor = ArgumentCaptor.forClass(List.class);

        // when
        openChatNotificationService.sendHourlyUnreadNotifications();

        // then
        then(fcmOutboxRepository).should().saveAll(captor.capture());
        List<FcmOutbox> saved = captor.getValue();
        assertThat(saved).allMatch(o -> o.getRoutingType() == FcmRoutingType.CHAT_OPEN);
    }

    @Test
    @DisplayName("FcmOutbox routingType = CHAT_OPEN 저장 — roomId 조회 실패 시 기본값 CHAT_OPEN")
    void should_save_outbox_with_routingType_CHAT_OPEN_as_default_when_room_not_found_in_hourly() {
        // given
        Long roomId = 9999L;
        Long userId = 1L;
        UnreadNotificationInfo info = new UnreadNotificationInfo(userId, roomId, 2);

        FcmToken fcmToken = buildFcmToken(userId, "token-user-1");

        given(participantRepository.findUnreadCountsForNotification()).willReturn(List.of(info));
        given(openChatRoomRepository.findAllById(any())).willReturn(List.of());
        given(fcmTokenRepository.findAllByUserIdIn(anyList())).willReturn(List.of(fcmToken));

        ArgumentCaptor<List<FcmOutbox>> captor = ArgumentCaptor.forClass(List.class);

        // when
        openChatNotificationService.sendHourlyUnreadNotifications();

        // then
        then(fcmOutboxRepository).should().saveAll(captor.capture());
        List<FcmOutbox> saved = captor.getValue();
        assertThat(saved).allMatch(o -> o.getRoutingType() == FcmRoutingType.CHAT_OPEN);
    }

    private FcmToken buildFcmToken(Long userId, String token) {
        User user = User.createForTest(userId, "user-" + userId);
        return FcmToken.builder().user(user).token(token).build();
    }
}
