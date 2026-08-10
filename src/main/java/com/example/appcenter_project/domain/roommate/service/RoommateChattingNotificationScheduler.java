package com.example.appcenter_project.domain.roommate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoommateChattingNotificationScheduler {

    private final RoommateChattingNotificationService roommateChattingNotificationService;

    @Scheduled(cron = "0 0 * * * *")
    public void sendHourlyNotifications() {
        roommateChattingNotificationService.sendHourlyUnreadNotifications();
    }
}
