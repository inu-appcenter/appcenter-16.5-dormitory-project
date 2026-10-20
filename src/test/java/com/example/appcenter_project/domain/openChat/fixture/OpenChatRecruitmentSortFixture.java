package com.example.appcenter_project.domain.openChat.fixture;

import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatRoomDto;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomRecruitmentStatus;

import java.time.LocalDateTime;

public class OpenChatRecruitmentSortFixture {

    public static ResponseOpenChatRoomDto createOpenRoomDto(Long roomId, LocalDateTime lastMessageAt) {
        return ResponseOpenChatRoomDto.builder()
                .roomId(roomId)
                .name("OPEN_ROOM_" + roomId)
                .recruitmentClosed(false)
                .recruitmentStatus(OpenChatRoomRecruitmentStatus.OPEN)
                .isJoinable(true)
                .isMyRoommate(false)
                .currentParticipants(2)
                .maxParticipants(10)
                .lastMessageAt(lastMessageAt)
                .build();
    }

    public static ResponseOpenChatRoomDto createClosedRoomDto(Long roomId, LocalDateTime lastMessageAt) {
        return ResponseOpenChatRoomDto.builder()
                .roomId(roomId)
                .name("CLOSED_ROOM_" + roomId)
                .recruitmentClosed(true)
                .recruitmentStatus(OpenChatRoomRecruitmentStatus.CLOSED)
                .isJoinable(false)
                .isMyRoommate(false)
                .currentParticipants(5)
                .maxParticipants(5)
                .lastMessageAt(lastMessageAt)
                .build();
    }

    public static ResponseOpenChatRoomDto createOpenRoomDtoIsMyRoommate(Long roomId, LocalDateTime lastMessageAt) {
        return ResponseOpenChatRoomDto.builder()
                .roomId(roomId)
                .name("ROOMMATE_ROOM_" + roomId)
                .recruitmentClosed(false)
                .recruitmentStatus(OpenChatRoomRecruitmentStatus.OPEN)
                .isJoinable(false)
                .isMyRoommate(true)
                .currentParticipants(2)
                .maxParticipants(2)
                .lastMessageAt(lastMessageAt)
                .build();
    }

    public static ResponseOpenChatRoomDto createJoinableOpenRoomDto(Long roomId) {
        return ResponseOpenChatRoomDto.builder()
                .roomId(roomId)
                .name("JOINABLE_OPEN_" + roomId)
                .recruitmentClosed(false)
                .recruitmentStatus(OpenChatRoomRecruitmentStatus.OPEN)
                .isJoinable(true)
                .isMyRoommate(false)
                .currentParticipants(2)
                .maxParticipants(10)
                .lastMessageAt(null)
                .build();
    }

    public static ResponseOpenChatRoomDto createFullRoomDto(Long roomId) {
        return ResponseOpenChatRoomDto.builder()
                .roomId(roomId)
                .name("FULL_OPEN_" + roomId)
                .recruitmentClosed(false)
                .recruitmentStatus(OpenChatRoomRecruitmentStatus.OPEN)
                .isJoinable(false)
                .isMyRoommate(false)
                .currentParticipants(10)
                .maxParticipants(10)
                .lastMessageAt(null)
                .build();
    }

    public static ResponseOpenChatRoomDto createJoinedClosedRoomDto(Long roomId) {
        return ResponseOpenChatRoomDto.builder()
                .roomId(roomId)
                .name("JOINED_CLOSED_" + roomId)
                .recruitmentClosed(true)
                .recruitmentStatus(OpenChatRoomRecruitmentStatus.CLOSED)
                .isJoinable(false)
                .isJoined(true)
                .isMyRoommate(false)
                .currentParticipants(5)
                .maxParticipants(5)
                .lastMessageAt(LocalDateTime.now())
                .build();
    }
}
