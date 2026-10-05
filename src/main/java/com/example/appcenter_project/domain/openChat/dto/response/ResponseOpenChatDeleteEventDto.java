package com.example.appcenter_project.domain.openChat.dto.response;

import com.example.appcenter_project.domain.openChat.enums.EventType;
import lombok.Getter;

@Getter
public class ResponseOpenChatDeleteEventDto {
    EventType eventType = EventType.MESSAGE_DELETED;
    Long messageId;
    Long roomId;
    String content = "삭제된 메시지입니다.";

    public ResponseOpenChatDeleteEventDto(Long messageId, Long roomId) {
        this.messageId = messageId;
        this.roomId = roomId;
    }
}