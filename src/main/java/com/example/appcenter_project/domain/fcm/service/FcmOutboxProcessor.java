package com.example.appcenter_project.domain.fcm.service;

import com.example.appcenter_project.domain.fcm.entity.FcmOutbox;
import com.example.appcenter_project.domain.fcm.enums.OutboxStatus;
import com.example.appcenter_project.domain.fcm.repository.FcmOutboxRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class FcmOutboxProcessor {

    private static final int DB_CHUNK_SIZE = 500;
    private static final int FCM_BATCH_SIZE = 30;
    private static final String FCM_SUCCESS_KEY_PREFIX = "fcm:success:";
    private static final String FCM_FAIL_KEY_PREFIX = "fcm:fail:";
    private static final String FCM_DEAD_KEY_PREFIX = "fcm:dead:";
    private static final String FCM_DLQ_ALERT_KEY = "fcm:dlq:alert";
    private static final int STUCK_THRESHOLD_MINUTES = 5;
    private static final double DLQ_ERROR_RATE_THRESHOLD = 0.05;
    private static final int DLQ_MIN_SAMPLE_SIZE = 100;

    @Value("${slack.webhook-url:}")
    private String slackWebhookUrl;

    private final FcmOutboxRepository fcmOutboxRepository;
    private final FcmAsyncSender fcmAsyncSender;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TransactionTemplate transactionTemplate;

    private static final int CLEANUP_RETENTION_DAYS = 7;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanup() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(CLEANUP_RETENTION_DAYS);
        int deleted = fcmOutboxRepository.deleteOldOutboxes(
                List.of(OutboxStatus.SENT, OutboxStatus.DEAD_PERMANENT, OutboxStatus.DEAD_EXHAUSTED, OutboxStatus.EXPIRED, OutboxStatus.DELETE),
                threshold
        );
        if (deleted > 0) {
            log.info("FCM Outbox 정리 완료: {}건 삭제 ({}일 경과)", deleted, CLEANUP_RETENTION_DAYS);
        }
    }

    @PreDestroy
    public void shutdown() {
        transactionTemplate.executeWithoutResult(status -> {
            int recovered = fcmOutboxRepository.recoverAllProcessing(LocalDateTime.now());
            if (recovered > 0) {
                log.info("Graceful Shutdown: PROCESSING → PENDING 복구 {}건", recovered);
            }
        });
    }

    @Scheduled(fixedDelay = 5_000)
    public void process() {
        LocalDateTime now = LocalDateTime.now();

        int stuck = fcmOutboxRepository.recoverStuckProcessing(now, now.minusMinutes(STUCK_THRESHOLD_MINUTES));
        if (stuck > 0) {
            log.warn("FCM stuck PROCESSING 복구: {}건 ({}분 초과)", stuck, STUCK_THRESHOLD_MINUTES);
        }

        int expired = fcmOutboxRepository.bulkMarkExpired(
                List.of(OutboxStatus.PENDING, OutboxStatus.FAILED), now
        );
        if (expired > 0) {
            log.info("FCM 만료 알림 폐기: {}건", expired);
        }

        while (true) {
            List<FcmOutbox> chunk = fetchAndMarkProcessing(now);
            if (chunk.isEmpty()) break;

            Map<String, List<FcmOutbox>> groups = chunk.stream()
                    .collect(Collectors.groupingBy(o -> o.getTitle() + "\0" + o.getBody()
                            + "\0" + o.getRoutingType() + "\0" + o.getRoutingId()
                            + "\0" + o.getMessageId() + "\0" + o.getAction()));

            List<CompletableFuture<int[]>> futures = new ArrayList<>();
            for (List<FcmOutbox> group : groups.values()) {
                String title = group.get(0).getTitle();
                String body = group.get(0).getBody();
                for (int i = 0; i < group.size(); i += FCM_BATCH_SIZE) {
                    List<FcmOutbox> batch = group.subList(i, Math.min(i + FCM_BATCH_SIZE, group.size()));
                    futures.add(fcmAsyncSender.sendOutboxBatch(batch, title, body));
                }
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        checkDlqAlert();
    }

    private List<FcmOutbox> fetchAndMarkProcessing(LocalDateTime now) {
        List<FcmOutbox> result = transactionTemplate.execute(status -> {
            List<FcmOutbox> chunk = fcmOutboxRepository.findChunk(
                    List.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
                    now,
                    PageRequest.of(0, DB_CHUNK_SIZE)
            );
            if (!chunk.isEmpty()) {
                fcmOutboxRepository.bulkMarkProcessing(
                        chunk.stream().map(FcmOutbox::getId).toList(),
                        LocalDateTime.now()
                );
            }
            return chunk;
        });
        return result != null ? result : List.of();
    }

    private void checkDlqAlert() {
        String today = LocalDate.now().toString();
        long success = getRedisLong(FCM_SUCCESS_KEY_PREFIX + today);
        long fail = getRedisLong(FCM_FAIL_KEY_PREFIX + today);
        long dead = getRedisLong(FCM_DEAD_KEY_PREFIX + today);
        long total = success + fail;

        if (total < DLQ_MIN_SAMPLE_SIZE) return;

        double errorRate = (double) dead / total;
        if (errorRate > DLQ_ERROR_RATE_THRESHOLD && !slackWebhookUrl.isBlank()) {
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(FCM_DLQ_ALERT_KEY, "1", Duration.ofHours(1));
            if (Boolean.TRUE.equals(isNew)) {
                sendSlackAlert(dead, total, errorRate);
            }
        }
    }

    private long getRedisLong(String key) {
        var ops = redisTemplate.opsForValue();
        if (ops == null) return 0L;
        Object val = ops.get(key);
        if (val == null) return 0L;
        return Long.parseLong(val.toString());
    }

    private void sendSlackAlert(long deadCount, long totalCount, double errorRate) {
        String payload = String.format(
                "{\"text\": \"[FCM DLQ 경고] 오늘 에러율 %.1f%% 초과 (DEAD %d건 / 전체 %d건). 즉시 확인이 필요합니다.\"}",
                errorRate * 100, deadCount, totalCount
        );
        try {
            HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(slackWebhookUrl))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(payload))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            log.warn("FCM DLQ 경고 Slack 알림 전송: 에러율 {}% (DEAD {}건 / 전체 {}건)",
                    String.format("%.1f", errorRate * 100), deadCount, totalCount);
        } catch (Exception e) {
            log.error("Slack 알림 전송 실패: {}", e.getMessage());
        }
    }
}
