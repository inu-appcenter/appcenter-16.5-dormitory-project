package com.example.appcenter_project.domain.openChat.dto.response;

import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResponseAdminChatRoomDto {
    private Long roomId;
    private String roomName;

    public static ResponseAdminChatRoomDto of(Long roomId, String roomName) {
        return ResponseAdminChatRoomDto.builder()
                .roomId(roomId)
                .roomName(roomName)
                .build();
    }

    public static ResponseAdminChatRoomDto from(OpenChatRoom room) {
        return ResponseAdminChatRoomDto.builder()
                .roomId(room.getId())
                .roomName(room.getName())
                .build();
    }
}
