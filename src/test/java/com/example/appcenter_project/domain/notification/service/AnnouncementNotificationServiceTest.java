package com.example.appcenter_project.domain.notification.service;

import com.example.appcenter_project.domain.announcement.entity.Announcement;
import com.example.appcenter_project.domain.fcm.entity.FcmOutbox;
import com.example.appcenter_project.domain.fcm.enums.FcmRoutingType;
import com.example.appcenter_project.domain.fcm.repository.FcmOutboxRepository;
import com.example.appcenter_project.domain.notification.entity.Notification;
import com.example.appcenter_project.domain.notification.repository.NotificationRepository;
import com.example.appcenter_project.domain.notification.repository.UserNotificationRepository;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.repository.FcmTokenRepository;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.domain.fcm.entity.FcmToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AnnouncementNotificationServiceTest {

    @Mock
    FcmOutboxRepository fcmOutboxRepository;

    @Mock
    FcmTokenRepository fcmTokenRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    NotificationRepository notificationRepository;

    @Mock
    UserNotificationRepository userNotificationRepository;

    @InjectMocks
    AnnouncementNotificationService announcementNotificationService;

    @Test
    @DisplayName("FcmOutbox routingType = ANNOUNCEMENT 저장 — AC-1: 공지사항 bulkEnqueueOutbox routing 저장")
    void should_save_outbox_with_routingType_ANNOUNCEMENT_when_announcement_notification_sent() {
        // given
        Announcement announcement = mock(Announcement.class);
        given(announcement.getId()).willReturn(5678L);
        given(announcement.getTitle()).willReturn("공지 제목");

        User user = User.createForTest(1L, "user-1");
        FcmToken fcmToken = FcmToken.builder().user(user).token("fcm-token-001").build();
        Notification notification = mock(Notification.class);
        given(notification.getTitle()).willReturn("새로운 공지사항이 올라왔어요!");
        given(notification.getBody()).willReturn("공지 제목");

        given(userRepository.findByReceiveNotificationTypesContainsAndRoleNotIn(any(), any()))
                .willReturn(List.of(user));
        given(notificationRepository.save(any())).willReturn(notification);
        given(fcmTokenRepository.findAllByUserInWithUser(anyList())).willReturn(List.of(fcmToken));

        ArgumentCaptor<List<FcmOutbox>> captor = ArgumentCaptor.forClass(List.class);

        // when
        announcementNotificationService.sendDormitoryNotifications(announcement);

        // then
        then(fcmOutboxRepository).should().saveAll(captor.capture());
        List<FcmOutbox> saved = captor.getValue();
        assertThat(saved).allMatch(o -> o.getRoutingType() == FcmRoutingType.ANNOUNCEMENT);
    }

    @Test
    @DisplayName("FcmOutbox routingId = 5678 저장 — AC-1: 공지사항 announcementId 라우팅 저장")
    void should_save_outbox_with_routingId_5678_when_announcement_notification_sent() {
        // given
        Announcement announcement = mock(Announcement.class);
        given(announcement.getId()).willReturn(5678L);
        given(announcement.getTitle()).willReturn("공지 제목");

        User user = User.createForTest(1L, "user-1");
        FcmToken fcmToken = FcmToken.builder().user(user).token("fcm-token-001").build();
        Notification notification = mock(Notification.class);
        given(notification.getTitle()).willReturn("새로운 공지사항이 올라왔어요!");
        given(notification.getBody()).willReturn("공지 제목");

        given(userRepository.findByReceiveNotificationTypesContainsAndRoleNotIn(any(), any()))
                .willReturn(List.of(user));
        given(notificationRepository.save(any())).willReturn(notification);
        given(fcmTokenRepository.findAllByUserInWithUser(anyList())).willReturn(List.of(fcmToken));

        ArgumentCaptor<List<FcmOutbox>> captor = ArgumentCaptor.forClass(List.class);

        // when
        announcementNotificationService.sendDormitoryNotifications(announcement);

        // then
        then(fcmOutboxRepository).should().saveAll(captor.capture());
        List<FcmOutbox> saved = captor.getValue();
        assertThat(saved).allMatch(o -> o.getRoutingId().equals(5678L));
    }

    // ──────────────────────────────────────────────
    // BR-792 AC8 — sendDormitoryNotifications 호출 시 FcmOutbox.create()에 userId가 전달되는지
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("FcmOutbox에 userId 전달 — BR-792 AC8: sendDormitoryNotifications 호출 시 userId가 null이 아님")
    void should_pass_userId_to_fcm_outbox_when_sendDormitoryNotifications_called() {
        // given
        Announcement announcement = mock(Announcement.class);
        given(announcement.getId()).willReturn(5678L);
        given(announcement.getTitle()).willReturn("공지 제목");

        User user = User.createForTest(1L, "user-1");
        FcmToken fcmToken = FcmToken.builder().user(user).token("fcm-token-001").build();
        Notification notification = mock(Notification.class);
        given(notification.getTitle()).willReturn("새로운 공지사항이 올라왔어요!");
        given(notification.getBody()).willReturn("공지 제목");

        given(userRepository.findByReceiveNotificationTypesContainsAndRoleNotIn(any(), any()))
                .willReturn(List.of(user));
        given(notificationRepository.save(any())).willReturn(notification);
        given(fcmTokenRepository.findAllByUserInWithUser(anyList())).willReturn(List.of(fcmToken));

        ArgumentCaptor<List<FcmOutbox>> captor = ArgumentCaptor.forClass(List.class);

        // when
        announcementNotificationService.sendDormitoryNotifications(announcement);

        // then
        then(fcmOutboxRepository).should().saveAll(captor.capture());
        List<FcmOutbox> saved = captor.getValue();
        assertThat(saved).allMatch(o -> o.getUserId() != null);
    }
}
