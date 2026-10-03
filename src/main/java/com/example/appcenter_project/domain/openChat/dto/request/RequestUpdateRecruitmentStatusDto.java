package com.example.appcenter_project.domain.openChat.dto.request;

import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomRecruitmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RequestUpdateRecruitmentStatusDto {

    @NotNull
    private OpenChatRoomRecruitmentStatus status;
}
