package com.example.appcenter_project.domain.openChat.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RequestEditOpenChatMessageDto {

    @NotBlank
    private String content;

    @Builder
    public RequestEditOpenChatMessageDto(String content) {
        this.content = content;
    }
}
