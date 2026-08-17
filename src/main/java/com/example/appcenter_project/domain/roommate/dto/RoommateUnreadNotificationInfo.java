package com.example.appcenter_project.domain.roommate.dto;

public record RoommateUnreadNotificationInfo(Long userId, Long roomId, long unreadCount, boolean isHost) {}
