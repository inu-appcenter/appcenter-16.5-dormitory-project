package com.example.appcenter_project.domain.openChat.repository;

import com.example.appcenter_project.domain.openChat.entity.OpenChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OpenChatMessageRepository extends JpaRepository<OpenChatMessage, Long> {

    Slice<OpenChatMessage> findByRoomIdOrderByCreatedDateAsc(Long roomId, Pageable pageable);

    boolean existsByDuplKey(String duplKey);

    @Query("SELECT m.id FROM OpenChatMessage m WHERE m.replyToMessageId = :replyToMessageId")
    List<Long> findIdsByReplyToMessageId(@Param("replyToMessageId") Long replyToMessageId);
}
