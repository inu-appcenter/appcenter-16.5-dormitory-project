package com.example.appcenter_project.domain.roommate.dto.response;

import com.example.appcenter_project.domain.openChat.enums.EventType;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingChat;
import com.example.appcenter_project.shared.dto.ReplySourceDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ResponseRoommateChatCreateEventDto {

    @Builder.Default
    private EventType eventType = EventType.MESSAGE_CREATED;

    private Long roommateChatId;

    private Long userId;

    private String content;

    private LocalDateTime createAt;

    private String userImageUrl;

    private Long disclosureRequestId;

    @JsonProperty("isRead")
    private boolean isRead;

    @JsonProperty("isSystem")
    private boolean isSystem;

    private ReplySourceDto replySource;

    @Builder.Default
    private List<String> imageUrls = List.of();

    public static ResponseRoommateChatCreateEventDto from(RoommateChattingChat chat, String userImageUrl) {
        return from(chat, userImageUrl, null);
    }

    public static ResponseRoommateChatCreateEventDto from(RoommateChattingChat chat, String userImageUrl, ReplySourceDto replySource) {
        return from(chat, userImageUrl, replySource, List.of());
    }

    public static ResponseRoommateChatCreateEventDto from(RoommateChattingChat chat, String userImageUrl,
                                                          ReplySourceDto replySource, List<String> imageUrls) {
        return ResponseRoommateChatCreateEventDto.builder()
                .roommateChatId(chat.getId())
                .userId(chat.getMember() != null ? chat.getMember().getId() : null)
                .content(chat.getContent())
                .createAt(chat.getCreatedDate())
                .userImageUrl(userImageUrl)
                .disclosureRequestId(chat.getDisclosureRequestId())
                .isRead(chat.isReadByReceiver())
                .isSystem(chat.isSystem())
                .replySource(replySource)
                .imageUrls(imageUrls)
                .build();
    }

    public static ResponseRoommateChatCreateEventDto fromSystem(String content) {
        return ResponseRoommateChatCreateEventDto.builder()
                .content(content)
                .isSystem(true)
                .isRead(true)
                .createAt(LocalDateTime.now())
                .build();
    }
}
