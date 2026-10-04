package com.example.appcenter_project.domain.openChat.dto.response;

import com.example.appcenter_project.domain.openChat.entity.OpenChatMessage;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.*;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingChat;
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
    private OpenChatRoomRecruitmentStatus recruitmentStatus;
    private LocalDateTime lastStatusChangedAt;
    private Long lastStatusChangedBy;

    public void updateIsBlockedByPartner(boolean v) {
        this.isBlockedByPartner = v;
    }

    public static ResponseOpenChatRoomDto from(OpenChatRoom room, OpenChatMessage message, int currentParticipants, boolean joined) {
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
                .lastMessage(toResponseContent(message))
                .unreadCount(0)
                .isDormOfficial(room.getTargetDorm() != null)
                .recruitmentClosed(room.isRecruitmentClosed())
                .recruitmentStatus(room.isRecruitmentClosed()
                        ? OpenChatRoomRecruitmentStatus.CLOSED
                        : OpenChatRoomRecruitmentStatus.OPEN)
                .lastStatusChangedAt(room.getLastStatusChangedAt())
                .lastStatusChangedBy(room.getLastStatusChangedBy())
                .build();
    }

    public static ResponseOpenChatRoomDto from(OpenChatRoom room, OpenChatMessage message, int currentParticipants, boolean joined, int unreadCount) {
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
                .lastMessage(toResponseContent(message))
                .unreadCount(unreadCount)
                .isDormOfficial(room.getTargetDorm() != null)
                .recruitmentClosed(room.isRecruitmentClosed())
                .recruitmentStatus(room.isRecruitmentClosed()
                        ? OpenChatRoomRecruitmentStatus.CLOSED
                        : OpenChatRoomRecruitmentStatus.OPEN)
                .lastStatusChangedAt(room.getLastStatusChangedAt())
                .lastStatusChangedBy(room.getLastStatusChangedBy())
                .build();
    }

    public static ResponseOpenChatRoomDto fromRoommate(
            Long roomId, String partnerName,
            RoommateChattingChat roommateChat,
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
                .lastMessageAt(roommateChat != null ? roommateChat.getCreatedDate() : null)
                .lastMessage(toResponseContent(roommateChat))
                .unreadCount(unreadCount)
                .isMyRoommate(isMyRoommate)
                .recruitmentStatus(OpenChatRoomRecruitmentStatus.OPEN)
                .build();
    }

    private static String toResponseContent(OpenChatMessage message) {
        if (message == null) return null;

        if (message.getDeletedState().isDeleted()) {
            return "삭제된 메시지입니다.";
        }

        if(message.getType() == OpenChatMessageType.IMAGE) {
            return "[이미지]";
        }

        return message.getContent().length() > 500 ? message.getContent().substring(0, 500) : message.getContent();
    }

    private static String toResponseContent(RoommateChattingChat message) {
        if (message == null) return null;

        if (message.getDeletedState().isDeleted()) {
            return "삭제된 메시지입니다.";
        }

        return message.getContent().length() > 500 ? message.getContent().substring(0, 500) : message.getContent();
    }
}
