package com.example.appcenter_project.domain.openChat.dto.response;

import com.example.appcenter_project.domain.openChat.enums.EventType;
import lombok.Getter;

@Getter
public class ResponseOpenChatMessageDeleteEventDto {

    private final Long messageId;

    private final Long roomId;

    private final String content = "삭제된 메시지입니다.";

    private final EventType eventType = EventType.MESSAGE_DELETED;

    public ResponseOpenChatMessageDeleteEventDto(Long messageId, Long roomId) {
        this.messageId = messageId;
        this.roomId = roomId;
    }
}