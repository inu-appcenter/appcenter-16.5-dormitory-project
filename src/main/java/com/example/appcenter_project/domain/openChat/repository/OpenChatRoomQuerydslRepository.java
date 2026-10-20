package com.example.appcenter_project.domain.openChat.repository;

import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OpenChatRoomQuerydslRepository {
    List<OpenChatRoom> findMyRooms(Long userId, String keyword);
    Page<OpenChatRoom> findByDormitory(String dormType, String keyword, Pageable pageable);
    Page<OpenChatRoom> findAllPublicRooms(String keyword, Pageable pageable);
}
