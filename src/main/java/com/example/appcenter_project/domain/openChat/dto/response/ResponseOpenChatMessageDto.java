package com.example.appcenter_project.domain.openChat.dto.response;

import com.example.appcenter_project.domain.openChat.entity.OpenChatMessage;
import com.example.appcenter_project.domain.openChat.enums.OpenChatMessageType;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomRecruitmentStatus;
import com.example.appcenter_project.shared.dto.ReplySourceDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ResponseOpenChatMessageDto {
    private Long messageId;
    private Long roomId;
    private Long senderId;
    private String senderNickname;
    private String content;
    private OpenChatMessageType type;
    private List<String> imageUrls;
    private int unreadCount;
    private LocalDateTime createdAt;
    private Long linkedRoomId;
    private String linkedRoomName;
    private String linkedRoomDescription;
    private Integer linkedRoomMaxParticipants;
    private Boolean linkedRoomRecruitmentClosed;
    private OpenChatRoomRecruitmentStatus linkedRoomRecruitmentStatus;
    private Long disclosureRequestId;
    private boolean isBot;
    private boolean isDeleted;
    private ReplySourceDto replySource;

    public static ResponseOpenChatMessageDto from(OpenChatMessage message, String senderNickname, int unreadCount) {
        return from(message, senderNickname, unreadCount, List.of(), null);
    }

    public static ResponseOpenChatMessageDto from(OpenChatMessage message, String senderNickname, int unreadCount, List<String> imageUrls) {
        return from(message, senderNickname, unreadCount, imageUrls, null);
    }

    public static ResponseOpenChatMessageDto from(OpenChatMessage message, String senderNickname, int unreadCount, List<String> imageUrls, ReplySourceDto replySource) {
        return ResponseOpenChatMessageDto.builder()
                .messageId(message.getId())
                .roomId(message.getRoomId())
                .senderId(message.getSenderId())
                .senderNickname(senderNickname)
                .content(message.getContent())
                .type(message.getType())
                .imageUrls(imageUrls != null ? imageUrls : List.of())
                .unreadCount(unreadCount)
                .createdAt(message.getCreatedDate())
                .isBot(message.getType() == OpenChatMessageType.BOT)
                .isDeleted(message.isDeleted())
                .replySource(replySource)
                .build();
    }

    public static ResponseOpenChatMessageDto fromStudentIdRequest(
            OpenChatMessage message, String senderNickname, int unreadCount, Long disclosureRequestId) {
        return ResponseOpenChatMessageDto.builder()
                .messageId(message.getId())
                .roomId(message.getRoomId())
                .senderId(message.getSenderId())
                .senderNickname(senderNickname)
                .content(message.getContent())
                .type(message.getType())
                .imageUrls(List.of())
                .unreadCount(unreadCount)
                .createdAt(message.getCreatedDate())
                .disclosureRequestId(disclosureRequestId)
                .isBot(false)
                .isDeleted(message.isDeleted())
                .build();
    }

    public static ResponseOpenChatMessageDto fromRoomLink(
            OpenChatMessage message, String senderNickname, int unreadCount,
            Long linkedRoomId, String linkedRoomName, String linkedRoomDescription, Integer linkedRoomMaxParticipants) {
        return fromRoomLink(message, senderNickname, unreadCount,
                linkedRoomId, linkedRoomName, linkedRoomDescription, linkedRoomMaxParticipants, false);
    }

    public static ResponseOpenChatMessageDto fromRoomLink(
            OpenChatMessage message, String senderNickname, int unreadCount,
            Long linkedRoomId, String linkedRoomName, String linkedRoomDescription, Integer linkedRoomMaxParticipants,
            boolean linkedRoomRecruitmentClosed) {
        return fromRoomLink(message, senderNickname, unreadCount,
                linkedRoomId, linkedRoomName, linkedRoomDescription, linkedRoomMaxParticipants,
                linkedRoomRecruitmentClosed,
                linkedRoomRecruitmentClosed ? OpenChatRoomRecruitmentStatus.CLOSED : OpenChatRoomRecruitmentStatus.OPEN);
    }

    public static ResponseOpenChatMessageDto fromRoomLink(
            OpenChatMessage message, String senderNickname, int unreadCount,
            Long linkedRoomId, String linkedRoomName, String linkedRoomDescription, Integer linkedRoomMaxParticipants,
            boolean linkedRoomRecruitmentClosed, OpenChatRoomRecruitmentStatus recruitmentStatus) {
        return ResponseOpenChatMessageDto.builder()
                .messageId(message.getId())
                .roomId(message.getRoomId())
                .senderId(message.getSenderId())
                .senderNickname(senderNickname)
                .content(message.getContent())
                .type(message.getType())
                .imageUrls(List.of())
                .unreadCount(unreadCount)
                .createdAt(message.getCreatedDate())
                .linkedRoomId(linkedRoomId)
                .linkedRoomName(linkedRoomName)
                .linkedRoomDescription(linkedRoomDescription)
                .linkedRoomMaxParticipants(linkedRoomMaxParticipants)
                .linkedRoomRecruitmentClosed(linkedRoomRecruitmentClosed)
                .linkedRoomRecruitmentStatus(recruitmentStatus)
                .isBot(false)
                .isDeleted(message.isDeleted())
                .build();
    }
}
