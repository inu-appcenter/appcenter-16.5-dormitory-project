package com.example.appcenter_project.domain.openChat.dto.response;

import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomRecruitmentStatus;
import lombok.Getter;

@Getter
public class ResponseRecruitmentStatusEventDto {
    private final Long derivedRoomId;
    private final OpenChatRoomRecruitmentStatus recruitmentStatus;

    private ResponseRecruitmentStatusEventDto(Long derivedRoomId, OpenChatRoomRecruitmentStatus recruitmentStatus) {
        this.derivedRoomId = derivedRoomId;
        this.recruitmentStatus = recruitmentStatus;
    }

    public static ResponseRecruitmentStatusEventDto of(Long derivedRoomId, OpenChatRoomRecruitmentStatus status) {
        return new ResponseRecruitmentStatusEventDto(derivedRoomId, status);
    }
}
