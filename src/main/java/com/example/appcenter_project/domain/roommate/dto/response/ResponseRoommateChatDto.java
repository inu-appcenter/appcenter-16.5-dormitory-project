package com.example.appcenter_project.domain.roommate.dto.response;

import com.example.appcenter_project.domain.roommate.entity.RoommateChattingChat;
import com.example.appcenter_project.shared.dto.ReplySourceDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class ResponseRoommateChatDto {

    private Long roommateChattingRoomId;
    private Long roommateChatId;
    private Long userId;
    private String content;
    private boolean read;
    @JsonProperty("isSystem")
    private boolean isSystem;
    private String createdDate;
    private String userImageUrl;
    private Long disclosureRequestId;
    @JsonProperty("isDeleted")
    private boolean isDeleted;
    private ReplySourceDto replySource;

    public static ResponseRoommateChatDto entityToDto(RoommateChattingChat chat, String userImageUrl) {
        return entityToDto(chat, userImageUrl, null);
    }

    public static ResponseRoommateChatDto entityToDto(RoommateChattingChat chat, String userImageUrl, ReplySourceDto replySource) {
        return ResponseRoommateChatDto.builder()
                .roommateChattingRoomId(chat.getRoommateChattingRoom().getId())
                .roommateChatId(chat.getId())
                .userId(chat.getMember() != null ? chat.getMember().getId() : null)
                .content(toResponseContent(chat))
                .read(chat.isReadByReceiver())
                .isSystem(chat.isSystem())
                .createdDate(chat.getCreatedDate().toString())
                .userImageUrl(userImageUrl)
                .disclosureRequestId(chat.getDisclosureRequestId())
                .isDeleted(chat.isDeleted())
                .replySource(replySource)
                .build();
    }

    public static ResponseRoommateChatDto systemDto(Long roomId, String content) {
        return ResponseRoommateChatDto.builder()
                .roommateChattingRoomId(roomId)
                .content(content)
                .isSystem(true)
                .read(true)
                .createdDate(java.time.LocalDateTime.now().toString())
                .build();
    }

    private static String toResponseContent(RoommateChattingChat chat) {
        if(chat == null) return null;

        if(chat.getDeletedState().isDeleted()) {
            return "삭제된 메시지입니다.";
        }

        return chat.getContent();
    }
}