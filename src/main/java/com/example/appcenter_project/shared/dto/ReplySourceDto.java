package com.example.appcenter_project.shared.dto;

import com.example.appcenter_project.shared.enums.ChatRoomType;
import com.example.appcenter_project.shared.enums.ReplySourceStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReplySourceDto {
    private Long replyToMessageId;
    private ReplySourceStatus status;
    private Long replyToSenderId;
    private String replyToSenderNickname;
    private String contentPreview;
    private ChatRoomType replyToRoomType;
    private Long replyToRoomId;
    private Long replyToDerivedRoomId;
}
