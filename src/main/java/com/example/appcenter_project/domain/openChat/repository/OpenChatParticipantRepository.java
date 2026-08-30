package com.example.appcenter_project.domain.openChat.repository;

import com.example.appcenter_project.domain.openChat.entity.OpenChatParticipant;
import com.example.appcenter_project.domain.openChat.enums.ChatNotificationMode;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OpenChatParticipantRepository extends JpaRepository<OpenChatParticipant, Long>, OpenChatParticipantQuerydslRepository {

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    boolean existsByRoomIdAndUserIdAndIsHost(Long roomId, Long userId, boolean isHost);

    long countByRoomId(Long roomId);

    long countByRoomIdAndIsHost(Long roomId, boolean isHost);

    Optional<OpenChatParticipant> findByRoomIdAndUserId(Long roomId, Long userId);

    List<OpenChatParticipant> findAllByRoomId(Long roomId);

    List<OpenChatParticipant> findAllByRoomIdIn(Collection<Long> roomIds);

    List<OpenChatParticipant> findAllByRoomIdAndNotificationMode(Long roomId, ChatNotificationMode mode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM OpenChatParticipant p WHERE p.roomId = :roomId")
    List<OpenChatParticipant> findAllByRoomIdWithLock(@Param("roomId") Long roomId);

    @Modifying
    @Transactional
    @Query("DELETE FROM OpenChatParticipant p WHERE p.roomId = :roomId")
    void deleteAllByRoomId(@Param("roomId") Long roomId);

    @Modifying
    @Transactional
    @Query("UPDATE OpenChatParticipant p SET p.lastReadMessageId = :messageId WHERE p.roomId = :roomId AND p.userId = :userId AND (p.lastReadMessageId IS NULL OR p.lastReadMessageId < :messageId)")
    void updateLastReadMessageId(@Param("roomId") Long roomId, @Param("userId") Long userId, @Param("messageId") Long messageId);

    @Modifying
    @Transactional
    @Query("UPDATE OpenChatParticipant p SET p.lastReadMessageId = :messageId WHERE p.roomId = :roomId AND p.userId IN :userIds")
    void updateLastReadMessageIdByRoomIdAndUserIdIn(@Param("roomId") Long roomId, @Param("userIds") Set<Long> userIds, @Param("messageId") Long messageId);
}
