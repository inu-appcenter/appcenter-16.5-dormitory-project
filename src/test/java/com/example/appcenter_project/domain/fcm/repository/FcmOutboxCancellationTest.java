package com.example.appcenter_project.domain.fcm.repository;

import com.example.appcenter_project.domain.fcm.entity.FcmOutbox;
import com.example.appcenter_project.domain.fcm.enums.FcmRoutingType;
import com.example.appcenter_project.domain.fcm.enums.OutboxStatus;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomType;
import com.example.appcenter_project.domain.openChat.service.OpenChatNotificationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class FcmOutboxCancellationTest {
    @Autowired FcmOutboxRepository repository;
    @Autowired EntityManager entityManager;

    @ParameterizedTest
    @EnumSource(OpenChatRoomType.class)
    void cancelsUnsentAndQueuesSentNotificationsForTheSelectedMessage(OpenChatRoomType roomType) {
        FcmRoutingType routing = roomType == OpenChatRoomType.PERSONAL
                ? FcmRoutingType.CHAT_PERSONAL : FcmRoutingType.CHAT_OPEN;
        List<FcmOutbox> matching = new ArrayList<>();
        for (OutboxStatus status : OutboxStatus.values()) {
            matching.add(save(routing, 10L, 123L, status));
        }
        FcmOutbox otherRecipient = save(routing, 10L, 123L, OutboxStatus.PENDING);
        FcmOutbox secondSent = save(routing, 10L, 123L, OutboxStatus.SENT);
        ReflectionTestUtils.setField(secondSent, "token", "second-device");
        List<FcmOutbox> unrelated = List.of(
                save(routing, 10L, 124L, OutboxStatus.PENDING),
                save(routing, 11L, 123L, OutboxStatus.PENDING),
                save(FcmRoutingType.CHAT_ROOMMATE, 10L, 123L, OutboxStatus.PENDING),
                save(routing, 10L, null, OutboxStatus.PENDING));
        repository.flush();
        entityManager.clear();
        OpenChatNotificationService service = new OpenChatNotificationService(null, null, null, repository);

        service.cancelNotificationsForMessage(10L, 123L, roomType);
        entityManager.flush();
        entityManager.clear();

        for (FcmOutbox before : matching) {
            OutboxStatus expected = before.getStatus() == OutboxStatus.PENDING || before.getStatus() == OutboxStatus.FAILED || before.getStatus() == OutboxStatus.SENT
                    ? OutboxStatus.DELETE : before.getStatus();
            assertThat(repository.findById(before.getId()).orElseThrow().getStatus()).isEqualTo(expected);
        }
        assertThat(repository.findById(otherRecipient.getId()).orElseThrow().getStatus()).isEqualTo(OutboxStatus.DELETE);
        for (FcmOutbox other : unrelated) {
            assertThat(repository.findById(other.getId()).orElseThrow().getStatus()).isEqualTo(OutboxStatus.PENDING);
        }
        service.cancelNotificationsForMessage(10L, 123L, roomType);
        entityManager.flush();
        entityManager.clear();
        assertThat(repository.bulkMarkDelete(
                10L, 123L, routing, List.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
                java.time.LocalDateTime.now())).isZero();
        List<FcmOutbox> cancellations = repository.findAll().stream()
                .filter(o -> o.getAction() == com.example.appcenter_project.domain.fcm.enums.FcmAction.DELETE)
                .filter(o -> matching.stream().noneMatch(original -> original.getId().equals(o.getId()))).toList();
        assertThat(cancellations).hasSize(2);
        assertThat(cancellations).extracting(FcmOutbox::getToken).containsExactlyInAnyOrder("token", "second-device");
        assertThat(cancellations).allSatisfy(o -> {
            assertThat(o.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(o.getMessageId()).isEqualTo(123L);
            assertThat(o.getRoutingId()).isEqualTo(10L);
            assertThat(o.getRoutingType()).isEqualTo(routing);
        });
    }

    @org.junit.jupiter.api.Test
    void cancellationKeepsItsTypeAcrossProcessingFailureRecoveryAndSuccess() {
        FcmOutbox cancellation = repository.save(FcmOutbox.create("device", "room", "text",
                FcmRoutingType.CHAT_OPEN, 10L, 123L).createCancellation());
        repository.flush();
        Long id = cancellation.getId();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        repository.bulkMarkProcessing(List.of(id), now.minusMinutes(10));
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(OutboxStatus.PROCESSING);
        repository.recoverStuckProcessing(now, now.minusMinutes(5));
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(OutboxStatus.PENDING);
        repository.bulkMarkProcessing(List.of(id), now);
        repository.bulkMarkFailed(List.of(id), "UNAVAILABLE", now);
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(OutboxStatus.FAILED);
        repository.bulkMarkProcessing(List.of(id), now);
        repository.recoverAllProcessing(now);
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(OutboxStatus.PENDING);
        repository.bulkMarkProcessing(List.of(id), now);
        repository.bulkUpdateStatus(List.of(id), OutboxStatus.SENT);
        assertThat(repository.findById(id).orElseThrow().getAction()).isEqualTo(com.example.appcenter_project.domain.fcm.enums.FcmAction.DELETE);
        assertThat(repository.findSentMessageNotificationsForUpdate(10L, 123L, FcmRoutingType.CHAT_OPEN)).isEmpty();
    }

    private FcmOutbox save(FcmRoutingType routing, Long roomId, Long messageId, OutboxStatus status) {
        FcmOutbox outbox = FcmOutbox.create("token", "title", "body", routing, roomId, messageId);
        ReflectionTestUtils.setField(outbox, "messageId", messageId);
        ReflectionTestUtils.setField(outbox, "status", status);
        return repository.save(outbox);
    }
}
