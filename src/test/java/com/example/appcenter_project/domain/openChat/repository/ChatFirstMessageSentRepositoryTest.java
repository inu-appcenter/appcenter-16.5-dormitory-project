package com.example.appcenter_project.domain.openChat.repository;

import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatRoomFixture;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingRoomRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class ChatFirstMessageSentRepositoryTest {

    @Autowired OpenChatRoomRepository openChatRoomRepository;
    @Autowired RoommateChattingRoomRepository roommateChattingRoomRepository;
    @Autowired EntityManager entityManager;

    @Test
    @DisplayName("[BR-TBD] 같은 방에 두 번 시도해도 첫 번째만 영향 행 1, 두 번째는 0")
    void onlyFirstCallWins() {
        OpenChatRoom room = openChatRoomRepository.saveAndFlush(OpenChatRoomFixture.createRoom());
        LocalDateTime now = LocalDateTime.now();

        int first = openChatRoomRepository.markFirstMessageSent(room.getId(), now);
        int second = openChatRoomRepository.markFirstMessageSent(room.getId(), now.plusSeconds(1));

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
    }

    @Test
    @DisplayName("[BR-TBD] 이후 같은 영속 엔티티가 dirty checking 으로 flush 돼도 first_message_sent_at 이 NULL 로 덮이지 않는다")
    void staleEntityDoesNotOverwriteColumn() {
        OpenChatRoom room = openChatRoomRepository.saveAndFlush(OpenChatRoomFixture.createRoom());
        openChatRoomRepository.markFirstMessageSent(room.getId(), LocalDateTime.now());

        room.updateLastMessage(5L, LocalDateTime.now());
        entityManager.flush();
        entityManager.clear();

        assertThat(openChatRoomRepository.findById(room.getId()).orElseThrow().getFirstMessageSentAt()).isNotNull();
    }

    @Test
    @DisplayName("[BR-TBD] 룸메이트 방 쿼리가 실제 테이블·컬럼에 대해 유효하다 (없는 방은 0행)")
    void roommateQueryIsValid() {
        assertThat(roommateChattingRoomRepository.markFirstMessageSent(999_999L, LocalDateTime.now())).isZero();
    }
}
