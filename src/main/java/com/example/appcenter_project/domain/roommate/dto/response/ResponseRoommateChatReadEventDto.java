package com.example.appcenter_project.domain.roommate.dto.response;

import com.example.appcenter_project.domain.openChat.enums.EventType;
import java.util.List;
import lombok.Getter;

@Getter
public class ResponseRoommateChatReadEventDto {
    private final EventType eventType = EventType.MESSAGE_READ;
    private final Long roomId;
    private final Long readerId;
    private final List<Long> messageIds;

    public ResponseRoommateChatReadEventDto(Long roomId, Long readerId, List<Long> messageIds) {
        this.roomId = roomId;
        this.readerId = readerId;
        this.messageIds = List.copyOf(messageIds);
    }
}
