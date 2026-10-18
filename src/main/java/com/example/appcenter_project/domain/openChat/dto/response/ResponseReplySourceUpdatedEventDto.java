package com.example.appcenter_project.domain.openChat.dto.response;

import com.example.appcenter_project.domain.openChat.enums.EventType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResponseReplySourceUpdatedEventDto {

    private Long messageId;

    private Long roomId;

    private Long replyToMessageId;

    private String contentPreview;

    @Builder.Default
    private EventType eventType = EventType.REPLY_SOURCE_UPDATED;
}
