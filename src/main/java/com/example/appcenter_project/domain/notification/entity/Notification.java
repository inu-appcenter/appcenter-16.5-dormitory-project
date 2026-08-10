package com.example.appcenter_project.domain.notification.entity;

import com.example.appcenter_project.domain.notification.dto.request.RequestNotificationDto;
import com.example.appcenter_project.common.BaseTimeEntity;
import com.example.appcenter_project.shared.enums.ApiType;
import com.example.appcenter_project.domain.user.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long boardId;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    private ApiType apiType;

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL)
    private List<UserNotification> userNotifications = new ArrayList<>();

    @Builder
    public Notification(Long boardId, String title, String body, NotificationType notificationType, ApiType apiType, List<UserNotification> userNotifications) {
        this.boardId = boardId;
        this.title = title;
        this.body = body;
        this.notificationType = notificationType;
        this.apiType = apiType;
        this.userNotifications = userNotifications;
    }

    public static Notification from(RequestNotificationDto requestDto) {
        return Notification.builder()
                .title(requestDto.getTitle())
                .body(requestDto.getBody())
                .notificationType(NotificationType.from(requestDto.getNotificationType()))
                .apiType(ApiType.NOTIFICATION)
                .build();
    }

    public static Notification of(String title, String body, NotificationType notificationType, ApiType apiType, Long boardId) {
        return Notification.builder()
                .title(title)
                .body(body)
                .notificationType(notificationType)
                .apiType(apiType)
                .boardId(boardId)
                .build();

    }

    public static Notification createChatNotification(String title, String body, Long chatRoomId) {
        return Notification.builder()
                .title(title)
                .body(body)
                .notificationType(NotificationType.CHAT)
                .apiType(ApiType.CHAT)
                .boardId(chatRoomId)
                .build();
    }

    public static Notification createRoommateMatchingNotification(String title, String body, Long matchingId) {
        return Notification.builder()
                .title(title)
                .body(body)
                .notificationType(NotificationType.ROOMMATE)
                .apiType(ApiType.ROOMMATE_MATCHING)
                .boardId(matchingId)
                .build();
    }

    public static Notification createRoommateBoardNotification(String title, String body, Long boardId) {
        return Notification.builder()
                .title(title)
                .body(body)
                .notificationType(NotificationType.ROOMMATE)
                .apiType(ApiType.ROOMMATE)
                .boardId(boardId)
                .build();
    }

    public static Notification createRoommateBoardUpdateNotification(String title, String body, Long boardId) {
        return Notification.builder()
                .title(title)
                .body(body)
                .notificationType(NotificationType.ROOMMATE)
                .apiType(ApiType.ROOMMATE_UPDATE)
                .boardId(boardId)
                .build();
    }

    public void update(RequestNotificationDto requestNotificationDto) {
        title = requestNotificationDto.getTitle();
        body = requestNotificationDto.getBody();
        notificationType = NotificationType.from(requestNotificationDto.getNotificationType());
    }
}