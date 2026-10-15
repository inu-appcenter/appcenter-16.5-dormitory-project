package com.example.appcenter_project.domain.fcm.entity;

import com.example.appcenter_project.common.BaseTimeEntity;
import com.example.appcenter_project.domain.fcm.converter.FcmRoutingTypeConverter;
import com.example.appcenter_project.domain.fcm.enums.FcmRoutingType;
import com.example.appcenter_project.domain.fcm.enums.FcmAction;
import com.example.appcenter_project.domain.fcm.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "fcm_outbox", indexes = {
        @Index(name = "idx_fcm_outbox_status_next_retry", columnList = "status, next_retry_at")
})
public class FcmOutbox extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fcm_outbox_seq")
    @SequenceGenerator(name = "fcm_outbox_seq", sequenceName = "fcm_outbox_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, length = 512)
    private String token;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private int maxRetry;

    private LocalDateTime nextRetryAt;

    @Column(length = 100)
    private String lastErrorCode;

    private LocalDateTime expiredAt;

    @Convert(converter = FcmRoutingTypeConverter.class)
    @Column(length = 20)
    private FcmRoutingType routingType;

    private Long routingId;

    private Long messageId;

    @Enumerated(EnumType.STRING)
    private FcmAction action;


    private static final int TTL_HOURS = 24;

    public static FcmOutbox create(String token, String title, String body) {
        return create(token, title, body, null, null, null);
    }

    public static FcmOutbox create(String token, String title, String body,
                                  FcmRoutingType routingType, Long routingId) {
        return create(token, title, body, routingType, routingId, null);
    }

    public static FcmOutbox create(String token, String title, String body,
                                   FcmRoutingType routingType, Long routingId, Long messageId) {
        FcmOutbox outbox = new FcmOutbox();
        outbox.token = token;
        outbox.title = title;
        outbox.body = body;
        outbox.routingType = routingType;
        outbox.routingId = routingId;
        outbox.messageId = messageId;
        outbox.action = FcmAction.CREATE;
        outbox.status = OutboxStatus.PENDING;
        outbox.retryCount = 0;
        outbox.maxRetry = 3;
        outbox.nextRetryAt = LocalDateTime.now();
        outbox.expiredAt = LocalDateTime.now().plusHours(TTL_HOURS);
        return outbox;
    }

    public void markProcessing() {
        this.status = OutboxStatus.PROCESSING;
    }

    public FcmOutbox createCancellation() {
        FcmOutbox cancel = create(token, "", "", routingType, routingId, messageId);
        cancel.action = FcmAction.DELETE;
        return cancel;
    }

    public FcmOutbox createUpdate(String title, String body) {
        FcmOutbox update = create(token, title, body, routingType, routingId, messageId);
        update.action = FcmAction.UPDATE;
        return update;
    }

    public void markSent() {
        this.status = OutboxStatus.SENT;
    }

    public void markFailed(String errorCode, LocalDateTime nextRetryAt) {
        this.status = OutboxStatus.FAILED;
        this.retryCount++;
        this.lastErrorCode = errorCode;
        this.nextRetryAt = nextRetryAt;
    }

    public void markDeadPermanent(String errorCode) {
        this.status = OutboxStatus.DEAD_PERMANENT;
        this.lastErrorCode = errorCode;
    }

    public void markDeadExhausted(String errorCode) {
        this.status = OutboxStatus.DEAD_EXHAUSTED;
        this.lastErrorCode = errorCode;
    }

    public void resetToPending() {
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
        this.nextRetryAt = LocalDateTime.now();
    }

    public void markExpired() {
        this.status = OutboxStatus.EXPIRED;
    }

    public boolean isExhausted() {
        return retryCount >= maxRetry;
    }

    public FcmAction getAction() {
        return action == null ? FcmAction.CREATE : action;
    }
}
