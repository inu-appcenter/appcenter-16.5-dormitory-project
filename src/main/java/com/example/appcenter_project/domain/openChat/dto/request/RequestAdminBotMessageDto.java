package com.example.appcenter_project.domain.openChat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RequestAdminBotMessageDto {

    @NotBlank
    @Size(max = 500)
    private String content;

    @Builder
    public RequestAdminBotMessageDto(String content) {
        this.content = content;
    }
}
