package com.example.appcenter_project.domain.openChat.repository;

import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomType;
import com.example.appcenter_project.domain.user.enums.DormType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OpenChatRoomRepository extends JpaRepository<OpenChatRoom, Long>, OpenChatRoomQuerydslRepository {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM OpenChatRoom r WHERE r.id = :roomId")
    Optional<OpenChatRoom> findByIdWithLock(@Param("roomId") Long roomId);

    @Modifying
    @Query(value = "UPDATE open_chat_room SET first_message_sent_at = :now WHERE id = :roomId AND first_message_sent_at IS NULL", nativeQuery = true)
    int markFirstMessageSent(@Param("roomId") Long roomId, @Param("now") LocalDateTime now);

    Optional<OpenChatRoom> findByTargetDorm(DormType targetDorm);

    List<OpenChatRoom> findByRoomTypeNot(OpenChatRoomType roomType);
}
