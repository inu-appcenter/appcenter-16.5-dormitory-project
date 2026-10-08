package com.example.appcenter_project.domain.openChat.dto.response;

import com.example.appcenter_project.domain.openChat.entity.OpenChatMessage;
import com.example.appcenter_project.domain.openChat.enums.EventType;
import com.example.appcenter_project.domain.openChat.enums.OpenChatMessageType;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomRecruitmentStatus;
import com.example.appcenter_project.shared.dto.ReplySourceDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ResponseOpenChatMessageCreateEventDto {

    @Builder.Default
    private final EventType eventType = EventType.MESSAGE_CREATED;

    private final Long messageId;

    private final Long senderId;

    private final String senderNickname;

    private final String content;

    private final OpenChatMessageType type;

    private final LocalDateTime createdAt;

    private final List<String> imageUrls;

    private final Long disclosureRequestId;

    private final Long linkedRoomId;

    private final String linkedRoomName;

    private final String linkedRoomDescription;

    private final Integer linkedRoomMaxParticipants;

    private final boolean linkedRoomRecruitmentClosed;

    private final OpenChatRoomRecruitmentStatus linkedRoomRecruitmentStatus;

    @JsonProperty("isBot")
    private boolean isBot;

    private ReplySourceDto replySource;

    public static ResponseOpenChatMessageCreateEventDto from(OpenChatMessage message, String senderNickname) {
        return from(message, senderNickname, List.of(), null);
    }

    public static ResponseOpenChatMessageCreateEventDto from(OpenChatMessage message, String senderNickname, List<String> imageUrls) {
        return from(message, senderNickname, imageUrls, null);
    }

    public static ResponseOpenChatMessageCreateEventDto from(OpenChatMessage message, String senderNickname, List<String> imageUrls, ReplySourceDto replySource) {
        return ResponseOpenChatMessageCreateEventDto.builder()
                .eventType(EventType.MESSAGE_CREATED)
                .messageId(message.getId())
                .senderId(message.getSenderId())
                .senderNickname(senderNickname)
                .content(message.getContent())
                .type(message.getType())
                .createdAt(message.getCreatedDate())
                .imageUrls(imageUrls != null ? imageUrls : List.of())
                .isBot(message.getType() == OpenChatMessageType.BOT)
                .replySource(replySource)
                .build();
    }

    public static ResponseOpenChatMessageCreateEventDto fromStudentIdRequest(OpenChatMessage message, String senderNickname, Long disclosureRequestId) {
        return ResponseOpenChatMessageCreateEventDto.builder()
                .eventType(EventType.STUDENT_ID_REQUEST)
                .messageId(message.getId())
                .senderId(message.getSenderId())
                .senderNickname(senderNickname)
                .content(message.getContent())
                .type(message.getType())
                .createdAt(message.getCreatedDate())
                .disclosureRequestId(disclosureRequestId)
                .imageUrls(List.of())
                .isBot(false)
                .build();
    }

    public static ResponseOpenChatMessageCreateEventDto fromRoomLink(OpenChatMessage message, String senderNickname, Long linkedRoomId, String linkedRoomName, String linkedRoomDescription, Integer linkedRoomMaxParticipants) {
        return fromRoomLink(message, senderNickname,
                linkedRoomId, linkedRoomName, linkedRoomDescription, linkedRoomMaxParticipants, false);
    }

    public static ResponseOpenChatMessageCreateEventDto fromRoomLink(OpenChatMessage message, String senderNickname, Long linkedRoomId, String linkedRoomName, String linkedRoomDescription, Integer linkedRoomMaxParticipants, boolean linkedRoomRecruitmentClosed) {
        return fromRoomLink(message, senderNickname,
                linkedRoomId, linkedRoomName, linkedRoomDescription, linkedRoomMaxParticipants,
                linkedRoomRecruitmentClosed,
                linkedRoomRecruitmentClosed ? OpenChatRoomRecruitmentStatus.CLOSED : OpenChatRoomRecruitmentStatus.OPEN);
    }

    public static ResponseOpenChatMessageCreateEventDto fromRoomLink(OpenChatMessage message, String senderNickname, Long linkedRoomId, String linkedRoomName, String linkedRoomDescription, Integer linkedRoomMaxParticipants, boolean linkedRoomRecruitmentClosed, OpenChatRoomRecruitmentStatus recruitmentStatus) {
        return ResponseOpenChatMessageCreateEventDto.builder()
                .eventType(EventType.ROOM_LINK_CREATED)
                .messageId(message.getId())
                .senderId(message.getSenderId())
                .senderNickname(senderNickname)
                .content(message.getContent())
                .type(message.getType())
                .imageUrls(List.of())
                .createdAt(message.getCreatedDate())
                .linkedRoomId(linkedRoomId)
                .linkedRoomName(linkedRoomName)
                .linkedRoomDescription(linkedRoomDescription)
                .linkedRoomMaxParticipants(linkedRoomMaxParticipants)
                .linkedRoomRecruitmentClosed(linkedRoomRecruitmentClosed)
                .linkedRoomRecruitmentStatus(recruitmentStatus)
                .isBot(false)
                .build();
    }

}
