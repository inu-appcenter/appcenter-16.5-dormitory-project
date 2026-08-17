package com.example.appcenter_project.domain.fcm.service;

import com.example.appcenter_project.domain.fcm.entity.FcmOutbox;
import com.example.appcenter_project.domain.fcm.entity.FcmToken;
import com.example.appcenter_project.domain.fcm.enums.FcmRoutingType;
import com.example.appcenter_project.domain.fcm.enums.OutboxStatus;
import com.example.appcenter_project.domain.fcm.repository.FcmOutboxRepository;
import com.example.appcenter_project.domain.user.repository.FcmTokenRepository;
import com.example.appcenter_project.global.mixpanel.MixpanelService;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.json.JSONObject;

/**
 * FCM 전송을 fcmExecutor 스레드 풀에서 비동기로 처리하는 컴포넌트.
 * <p>
 * - sendBatch(): 최대 500개 토큰을 sendEachForMulticast()로 HTTP 1회에 전송 (전체 알림용)
 * - sendOne(): 단일 토큰 전송 (개별 사용자 알림용)
 * - sendOutboxBatch(): Outbox 패턴 배치 전송 + DB 상태 업데이트
 * <p>
 * FcmMessageService 내부에서 @Async를 직접 호출하면 Spring 프록시를 우회해
 * 비동기가 동작하지 않으므로 별도 빈으로 분리했다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FcmAsyncSender {

    private static final int MULTICAST_LIMIT = 500;
    private static final String FCM_SUCCESS_KEY_PREFIX = "fcm:success:";
    private static final String FCM_FAIL_KEY_PREFIX    = "fcm:fail:";
    private static final String FCM_DEAD_KEY_PREFIX    = "fcm:dead:";
    private static final List<String> PERMANENT_ERROR_CODES = List.of("UNREGISTERED", "INVALID_ARGUMENT");

    private final FcmTokenRepository fcmTokenRepository;
    private final FcmOutboxRepository fcmOutboxRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final FirebaseMessaging firebaseMessaging;
    private final MixpanelService mixpanelService;

    public record RetryKey(int retryCount, String errorCode) {}

    /**
     * 토큰 목록(최대 500개)을 MulticastMessage로 묶어 HTTP 1회에 전송.
     * FcmMessageService에서 500개 단위 청크로 분할 후 호출한다.
     */
    @Async("fcmExecutor")
    @Transactional
    public CompletableFuture<Void> sendBatch(List<String> tokens, String title, String body) {
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(notification)
                .build();

        try {
            BatchResponse batchResponse = firebaseMessaging.sendEachForMulticast(message);
            log.info("FCM 배치 전송: 성공={}, 실패={}", batchResponse.getSuccessCount(), batchResponse.getFailureCount());

            List<SendResponse> responses = batchResponse.getResponses();
            List<String> failedTokens = new ArrayList<>();
            int successCount = 0;

            for (int i = 0; i < responses.size(); i++) {
                if (responses.get(i).isSuccessful()) {
                    successCount++;
                } else {
                    String failedToken = tokens.get(i);
                    log.warn("FCM 전송 실패 (token: {}...): {}",
                            failedToken.substring(0, Math.min(20, failedToken.length())),
                            responses.get(i).getException().getMessage());
                    failedTokens.add(failedToken);
                }
            }

            if (!failedTokens.isEmpty()) {
                fcmTokenRepository.deleteAllByTokenIn(failedTokens);
            }
            recordBatch(successCount, failedTokens.size());
        } catch (FirebaseMessagingException e) {
            log.error("FCM 배치 전송 오류: {}", e.getMessage());
            recordFail();
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 단일 토큰 전송. 개별 사용자 알림(sendNotification, sendGroupOrderNotification 등)에서 사용.
     */
    @Async("fcmExecutor")
    @Transactional
    public CompletableFuture<Void> sendOne(String token, String title, String body) {
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .build();

        try {
            String response = firebaseMessaging.send(message);
            log.info("FCM 전송 성공: {}", response);
            recordSuccess();
        } catch (Exception e) {
            log.error("FCM 전송 실패 (token: {}...): {}", token.substring(0, Math.min(20, token.length())), e.getMessage());
            fcmTokenRepository.deleteByToken(token);
            recordFail();
        }

        return CompletableFuture.completedFuture(null);
    }

    @Async("fcmExecutor")
    public CompletableFuture<int[]> sendOutboxBatch(List<FcmOutbox> batch, String title, String body) {
        List<String> tokens = batch.stream().map(FcmOutbox::getToken).toList();

        FcmOutbox rep = batch.get(0);
        FcmRoutingType rt = rep.getRoutingType();
        Long rid = rep.getRoutingId();

        MulticastMessage.Builder builder = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build());

        log.info("======== sendoutboxbatch start ============");
        if (rt != null && rid != null) {
            String groupKey = rt.threadId(rid);
            String fcmPayloadLog = String.format(
                    "{\"notification\":{\"title\":\"%s\",\"body\":\"%s\"}," +
                            "\"apns\":{\"payload\":{\"aps\":{\"sound\":\"default\",\"thread-id\":\"%s\"}}}," +
                            "\"android\":{\"notification\":{\"sound\":\"default\",\"tag\":\"%s\"}}," +
                            "\"data\":{\"path\":\"%s\",\"type\":\"%s\",\"%s\":\"%s\"}}",
                    title, body, groupKey, groupKey, rt.path(rid), rt.dataType(), rt.dataKey(), rid);
            log.info("FCM Outbox 전송 payload: {}", fcmPayloadLog);
          builder
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder().setSound("default").setThreadId(groupKey).build())
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default").setTag(groupKey).build())
                            .build())
                    .putData("path", rt.path(rid))
                    .putData("type", rt.dataType())
                    .putData(rt.dataKey(), String.valueOf(rid));
        }
        log.info("======== sendoutboxbatch end ============");

        MulticastMessage message = builder.build();

        try {
            BatchResponse batchResponse = firebaseMessaging.sendEachForMulticast(message);
            if (batchResponse == null) {
                return CompletableFuture.completedFuture(new int[]{0, 0, 0});
            }
            List<SendResponse> responses = batchResponse.getResponses();

            List<Long> sentIds = new ArrayList<>();
            Map<String, List<Long>> deadPermMap = new HashMap<>();
            Map<String, List<Long>> exhaustedMap = new HashMap<>();
            Map<RetryKey, List<Long>> failedMap = new HashMap<>();
            List<String> permanentFailTokens = new ArrayList<>();
            int deadCount = 0;

            for (int i = 0; i < responses.size(); i++) {
                SendResponse response = responses.get(i);
                FcmOutbox outbox = batch.get(i);

                if (response.isSuccessful()) {
                    sentIds.add(outbox.getId());
                } else {
                    FirebaseMessagingException e = response.getException();
                    String errorCode = e.getMessagingErrorCode() != null
                            ? e.getMessagingErrorCode().name()
                            : "UNKNOWN";

                    if (PERMANENT_ERROR_CODES.contains(errorCode)) {
                        deadPermMap.computeIfAbsent(errorCode, k -> new ArrayList<>()).add(outbox.getId());
                        permanentFailTokens.add(outbox.getToken());
                        deadCount++;
                        log.warn("FCM 영구 오류 (id={}): {}", outbox.getId(), errorCode);
                    } else {
                        boolean exhausted = outbox.getRetryCount() + 1 >= outbox.getMaxRetry();
                        if (exhausted) {
                            exhaustedMap.computeIfAbsent(errorCode, k -> new ArrayList<>()).add(outbox.getId());
                            deadCount++;
                            log.warn("FCM 최대 재시도 초과 (id={}): {}", outbox.getId(), errorCode);
                        } else {
                            long backoffMinutes = 5L * (1L << outbox.getRetryCount());
                            failedMap.computeIfAbsent(new RetryKey(outbox.getRetryCount(), errorCode), k -> new ArrayList<>()).add(outbox.getId());
                            log.warn("FCM 전송 실패, {}분 후 재시도 (id={}, retry={}/{}): {}",
                                    backoffMinutes, outbox.getId(), outbox.getRetryCount() + 1, outbox.getMaxRetry(), errorCode);
                        }
                    }
                }
            }

            if (!sentIds.isEmpty()) {
                fcmOutboxRepository.bulkUpdateStatus(sentIds, OutboxStatus.SENT);

                List<String> sentTokens = batch.stream()
                        .filter(o -> sentIds.contains(o.getId()))
                        .map(FcmOutbox::getToken)
                        .toList();
                List<FcmToken> fcmTokens = fcmTokenRepository.findAllByTokenIn(sentTokens);
                Map<String, Long> tokenToUserId = fcmTokens.stream()
                        .collect(Collectors.toMap(FcmToken::getToken, t -> t.getUser().getId(), (a, b) -> a));
                for (FcmOutbox outbox : batch) {
                    if (sentIds.contains(outbox.getId())) {
                        Long ownerId = tokenToUserId.get(outbox.getToken());
                        if (ownerId != null) {
                            try {
                                JSONObject props = new JSONObject();
                                if (outbox.getRoutingType() != null) {
                                    props.put("routing_type", outbox.getRoutingType().name());
                                    props.put("routing_id", outbox.getRoutingId());
                                }
                                mixpanelService.trackEvent(ownerId.toString(), "push_sent", props);
                            } catch (Exception ex) {
                                log.warn("Mixpanel push_sent 이벤트 추적 실패 - userId: {}", ownerId);
                            }
                        }
                    }
                }
            }
            deadPermMap.forEach((ec, ids) -> fcmOutboxRepository.bulkUpdateStatusWithError(ids, OutboxStatus.DEAD_PERMANENT, ec));
            exhaustedMap.forEach((ec, ids) -> fcmOutboxRepository.bulkUpdateStatusWithError(ids, OutboxStatus.DEAD_EXHAUSTED, ec));
            failedMap.forEach((key, ids) -> {
                long backoffMinutes = 5L * (1L << key.retryCount());
                fcmOutboxRepository.bulkMarkFailed(ids, key.errorCode(), LocalDateTime.now().plusMinutes(backoffMinutes));
            });
            if (!permanentFailTokens.isEmpty()) {
                fcmTokenRepository.deleteAllByTokenIn(permanentFailTokens);
                log.info("FCM 오류 토큰 삭제: {}건", permanentFailTokens.size());
            }

            int failCount = responses.size() - sentIds.size();
            recordOutboxStats(sentIds.size(), failCount, deadCount);
            log.info("FCM Outbox 배치 전송: 성공={}, 실패={} ({}건 중)", sentIds.size(), failCount, batch.size());
            return CompletableFuture.completedFuture(new int[]{sentIds.size(), failCount, deadCount});

        } catch (FirebaseMessagingException e) {
            log.error("FCM Outbox 배치 전송 오류: {}", e.getMessage());
            Map<String, List<Long>> exhaustedMap = new HashMap<>();
            Map<RetryKey, List<Long>> failedMap = new HashMap<>();
            int deadCount = 0;
            for (FcmOutbox outbox : batch) {
                if (outbox.getRetryCount() + 1 >= outbox.getMaxRetry()) {
                    exhaustedMap.computeIfAbsent("BATCH_ERROR", k -> new ArrayList<>()).add(outbox.getId());
                    deadCount++;
                } else {
                    failedMap.computeIfAbsent(new RetryKey(outbox.getRetryCount(), "BATCH_ERROR"), k -> new ArrayList<>()).add(outbox.getId());
                }
            }
            exhaustedMap.forEach((ec, ids) -> fcmOutboxRepository.bulkUpdateStatusWithError(ids, OutboxStatus.DEAD_EXHAUSTED, ec));
            failedMap.forEach((key, ids) -> {
                long backoffMinutes = 5L * (1L << key.retryCount());
                fcmOutboxRepository.bulkMarkFailed(ids, key.errorCode(), LocalDateTime.now().plusMinutes(backoffMinutes));
            });
            recordOutboxStats(0, batch.size(), deadCount);
            return CompletableFuture.completedFuture(new int[]{0, batch.size(), deadCount});
        }
    }

    private void recordBatch(int successCount, int failCount) {
        String today = LocalDate.now().toString();
        if (successCount > 0) {
            String key = FCM_SUCCESS_KEY_PREFIX + today;
            redisTemplate.opsForValue().increment(key, successCount);
            redisTemplate.expire(key, Duration.ofHours(24));
        }
        if (failCount > 0) {
            String key = FCM_FAIL_KEY_PREFIX + today;
            redisTemplate.opsForValue().increment(key, failCount);
            redisTemplate.expire(key, Duration.ofHours(24));
        }
    }

    private void recordOutboxStats(int successCount, int failCount, int deadCount) {
        String today = LocalDate.now().toString();
        if (successCount > 0) {
            String key = FCM_SUCCESS_KEY_PREFIX + today;
            redisTemplate.opsForValue().increment(key, successCount);
            redisTemplate.expire(key, Duration.ofHours(24));
        }
        if (failCount > 0) {
            String key = FCM_FAIL_KEY_PREFIX + today;
            redisTemplate.opsForValue().increment(key, failCount);
            redisTemplate.expire(key, Duration.ofHours(24));
        }
        if (deadCount > 0) {
            String key = FCM_DEAD_KEY_PREFIX + today;
            redisTemplate.opsForValue().increment(key, deadCount);
            redisTemplate.expire(key, Duration.ofHours(24));
        }
    }

    private void recordSuccess() {
        String key = FCM_SUCCESS_KEY_PREFIX + LocalDate.now();
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofHours(24));
    }

    private void recordFail() {
        String key = FCM_FAIL_KEY_PREFIX + LocalDate.now();
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofHours(24));
    }
}
