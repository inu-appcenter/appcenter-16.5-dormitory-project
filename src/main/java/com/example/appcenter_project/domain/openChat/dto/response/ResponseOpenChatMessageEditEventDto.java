package com.example.appcenter_project.domain.openChat.dto.response;

import com.example.appcenter_project.domain.openChat.entity.OpenChatMessage;
import com.example.appcenter_project.domain.openChat.enums.EventType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ResponseOpenChatMessageEditEventDto {

    private Long messageId;

    private Long roomId;

    private String content;

    private LocalDateTime editedAt;

    @Builder.Default
    private EventType eventType = EventType.MESSAGE_UPDATED;


    public static ResponseOpenChatMessageEditEventDto from(OpenChatMessage message) {
        return ResponseOpenChatMessageEditEventDto.builder()
                .messageId(message.getId())
                .roomId(message.getRoomId())
                .content(message.getContent())
                .editedAt(message.getEditedAt())
                .build();
    }
}
