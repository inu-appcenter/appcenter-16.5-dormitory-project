package com.example.appcenter_project.domain.openChat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ResponseOpenChatMessageListDto {
    private List<ResponseOpenChatMessageDto> messages;
    private boolean hasNext;
    private Long nextCursor;
    private boolean hasNewer;
}
