package com.example.appcenter_project.domain.openChat.dto.response;

import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomScope;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ResponseOpenChatRoomDetailDto {

    private Long roomId;
    private String name;
    private String description;
    private OpenChatRoomScope scope;
    private int currentParticipants;
    private int maxParticipants;
    private boolean isOfficial;
    private LocalDateTime createdAt;
    private boolean isBlockedByPartner;

    public void updateIsBlockedByPartner(boolean v) {
        this.isBlockedByPartner = v;
    }
}
