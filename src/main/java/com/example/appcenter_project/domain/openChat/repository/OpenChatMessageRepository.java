package com.example.appcenter_project.domain.openChat.repository;

import com.example.appcenter_project.domain.openChat.entity.OpenChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpenChatMessageRepository extends JpaRepository<OpenChatMessage, Long>, OpenChatMessageQuerydslRepository {

    Slice<OpenChatMessage> findByRoomIdOrderByCreatedDateAsc(Long roomId, Pageable pageable);

    boolean existsByDuplKey(String duplKey);
}
