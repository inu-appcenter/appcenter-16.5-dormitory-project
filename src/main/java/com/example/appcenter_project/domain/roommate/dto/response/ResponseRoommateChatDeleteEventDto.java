package com.example.appcenter_project.domain.roommate.dto.response;

import com.example.appcenter_project.domain.openChat.enums.EventType;
import lombok.Getter;

@Getter
public class ResponseRoommateChatDeleteEventDto {
    EventType eventType = EventType.MESSAGE_DELETED;
    Long messageId;
    Long roomId;
    String content = "삭제된 메시지입니다.";

    public ResponseRoommateChatDeleteEventDto(Long messageId, Long roomId) {
        this.messageId = messageId;
        this.roomId = roomId;
    }
}