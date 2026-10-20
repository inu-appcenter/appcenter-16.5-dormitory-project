package com.example.appcenter_project.domain.openChat.fixture;

import com.example.appcenter_project.domain.openChat.entity.OpenChatMessage;
import com.example.appcenter_project.domain.openChat.enums.OpenChatMessageType;

public class ChatReplyMetadataFixture {

    public static OpenChatMessage createTextMessage(Long id, Long roomId, Long senderId) {
        return OpenChatMessage.createForTest(id, roomId, senderId, "텍스트 메시지 내용", OpenChatMessageType.TEXT, false, null);
    }

    public static OpenChatMessage createDeletedMessage(Long id, Long roomId, Long senderId) {
        return OpenChatMessage.createForTest(id, roomId, senderId, "삭제된 메시지", OpenChatMessageType.TEXT, true, null);
    }

    public static OpenChatMessage createSystemMessage(Long id, Long roomId, Long senderId) {
        return OpenChatMessage.createForTest(id, roomId, senderId, "시스템 메시지", OpenChatMessageType.SYSTEM, false, null);
    }

    public static OpenChatMessage createReopenCardMessage(Long id, Long roomId, Long senderId, Long derivedRoomId) {
        String content = "{\"derivedRoomId\":" + derivedRoomId + ",\"roomName\":\"파생톡방\"}";
        return OpenChatMessage.createForTest(id, roomId, senderId, content, OpenChatMessageType.REOPEN_CARD, false, null);
    }

    public static OpenChatMessage createNestedReplyMessage(Long id, Long roomId, Long senderId, Long originalMessageId) {
        return OpenChatMessage.createForTest(id, roomId, senderId, "답장 메시지", OpenChatMessageType.TEXT, false, originalMessageId);
    }
}
