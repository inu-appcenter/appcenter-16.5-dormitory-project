package com.example.appcenter_project.domain.openChat.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RequestOpenChatMessageDto {
    private Long roomId;

    @NotBlank
    private String content;

    private Long replyToMessageId;
}
