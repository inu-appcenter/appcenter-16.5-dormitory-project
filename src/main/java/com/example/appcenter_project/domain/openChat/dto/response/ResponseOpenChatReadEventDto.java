package com.example.appcenter_project.domain.openChat.dto.response;

import com.example.appcenter_project.domain.openChat.enums.EventType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResponseOpenChatReadEventDto {

    private Long messageId;

    private int unreadCount;

    @Builder.Default
    private EventType eventType = EventType.MESSAGE_READ;

    public static ResponseOpenChatReadEventDto of(Long messageId, int unreadCount) {
        return ResponseOpenChatReadEventDto.builder()
                .messageId(messageId)
                .unreadCount(unreadCount)
                .build();
    }
}
