package com.example.appcenter_project.domain.fcm.enums;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED,
    DEAD_PERMANENT,
    DEAD_EXHAUSTED,
    EXPIRED,
    DELETE,
}
