package com.example.appcenter_project.domain.openChat.dto.response;

import com.example.appcenter_project.domain.openChat.enums.EventType;
import com.example.appcenter_project.shared.enums.ReplySourceStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResponseReplySourceUpdatedEventDto {

    private Long messageId;

    private Long roomId;

    private Long replyToMessageId;

    private ReplySourceStatus status;

    private String contentPreview;

    @Builder.Default
    private EventType eventType = EventType.REPLY_SOURCE_UPDATED;
}
