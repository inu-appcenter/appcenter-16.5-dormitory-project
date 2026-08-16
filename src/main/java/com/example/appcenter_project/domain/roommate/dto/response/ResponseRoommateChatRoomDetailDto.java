package com.example.appcenter_project.domain.roommate.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResponseRoommateChatRoomDetailDto {
    private Long chatRoomId;
    private String partnerName;
    private String partnerProfileImageUrl;
    private boolean isOpponentLeft;
    private boolean isBlockedByPartner;
    private String myBoardTitle;
    private String opponentBoardTitle;
}
