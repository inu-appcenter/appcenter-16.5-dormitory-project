package com.example.appcenter_project.domain.openChat.dto.response;

import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.ChatCategory;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomScope;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ResponseOpenChatRoomDto {

    private Long roomId;
    private String name;
    private String description;
    private OpenChatRoomScope scope;
    private OpenChatRoomType roomType;
    private ChatCategory chatCategory;
    private Boolean isPublic;
    private boolean hasPassword;
    private int currentParticipants;
    private int maxParticipants;
    private boolean isJoined;
    private LocalDateTime lastMessageAt;
    private String lastMessage;
    private int unreadCount;
    private boolean isMyRoommate;
    private boolean isBlockedByPartner;
    private boolean isDormOfficial;
    private boolean recruitmentClosed;

    public void updateIsBlockedByPartner(boolean v) {
        this.isBlockedByPartner = v;
    }

    public static ResponseOpenChatRoomDto from(OpenChatRoom room, int currentParticipants, boolean joined) {
        return ResponseOpenChatRoomDto.builder()
                .roomId(room.getId())
                .name(room.getName())
                .description(room.getDescription())
                .scope(room.getScope())
                .roomType(room.getRoomType())
                .chatCategory(ChatCategory.OPEN_CHAT)
                .isPublic(room.isPublic())
                .hasPassword(room.getPassword() != null)
                .currentParticipants(currentParticipants)
                .maxParticipants(room.getMaxParticipants())
                .isJoined(joined)
                .lastMessageAt(room.getLastMessageAt())
                .lastMessage(room.getLastMessage())
                .unreadCount(0)
                .isDormOfficial(room.getTargetDorm() != null)
                .recruitmentClosed(room.isRecruitmentClosed())
                .build();
    }

    public static ResponseOpenChatRoomDto from(OpenChatRoom room, int currentParticipants, boolean joined, int unreadCount) {
        return ResponseOpenChatRoomDto.builder()
                .roomId(room.getId())
                .name(room.getName())
                .description(room.getDescription())
                .scope(room.getScope())
                .roomType(room.getRoomType())
                .chatCategory(ChatCategory.OPEN_CHAT)
                .isPublic(room.isPublic())
                .hasPassword(room.getPassword() != null)
                .currentParticipants(currentParticipants)
                .maxParticipants(room.getMaxParticipants())
                .isJoined(joined)
                .lastMessageAt(room.getLastMessageAt())
                .lastMessage(room.getLastMessage())
                .unreadCount(unreadCount)
                .isDormOfficial(room.getTargetDorm() != null)
                .recruitmentClosed(room.isRecruitmentClosed())
                .build();
    }

    public static ResponseOpenChatRoomDto fromRoommate(
            Long roomId, String partnerName,
            LocalDateTime lastMessageAt, String lastMessage,
            int unreadCount, boolean isMyRoommate) {
        return ResponseOpenChatRoomDto.builder()
                .roomId(roomId)
                .name(partnerName)
                .description(null)
                .scope(null)
                .roomType(null)
                .chatCategory(ChatCategory.ROOMMATE)
                .isPublic(false)
                .hasPassword(false)
                .currentParticipants(2)
                .maxParticipants(2)
                .isJoined(true)
                .lastMessageAt(lastMessageAt)
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .isMyRoommate(isMyRoommate)
                .build();
    }
}
