package com.example.appcenter_project.domain.roommate.fixture;

import com.example.appcenter_project.domain.roommate.entity.RoommateChattingChat;

public class RoommateChattingReplyFixture {

    public static RoommateChattingChat createNormalChat(Long id, Long roomId, Long senderId) {
        return RoommateChattingChat.createForTest(id, roomId, senderId, "일반 메시지 내용", false, false, null);
    }

    public static RoommateChattingChat createDeletedChat(Long id, Long roomId, Long senderId) {
        return RoommateChattingChat.createForTest(id, roomId, senderId, "삭제된 메시지", false, true, null);
    }

    public static RoommateChattingChat createSystemChat(Long id, Long roomId, Long senderId) {
        return RoommateChattingChat.createForTest(id, roomId, senderId, "시스템 메시지", true, false, null);
    }

    public static RoommateChattingChat createNestedReplyChat(Long id, Long roomId, Long senderId, Long originalMessageId) {
        return RoommateChattingChat.createForTest(id, roomId, senderId, "답장 메시지", false, false, originalMessageId);
    }
}
