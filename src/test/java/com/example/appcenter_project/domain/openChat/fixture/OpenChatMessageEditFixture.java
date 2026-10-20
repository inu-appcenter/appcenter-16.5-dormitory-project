package com.example.appcenter_project.domain.openChat.fixture;

import com.example.appcenter_project.domain.openChat.dto.request.RequestEditOpenChatMessageDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatMessageDto;
import com.example.appcenter_project.domain.openChat.entity.OpenChatMessage;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatMessageType;

import java.time.LocalDateTime;

public class OpenChatMessageEditFixture {

    public static RequestEditOpenChatMessageDto createEditRequest() {
        return RequestEditOpenChatMessageDto.builder()
                .content("수정된 메시지 내용입니다.")
                .build();
    }

    public static RequestEditOpenChatMessageDto createEditRequestWithBlankContent() {
        return RequestEditOpenChatMessageDto.builder()
                .content("   ")
                .build();
    }

    public static RequestEditOpenChatMessageDto createEditRequestWithNullContent() {
        return RequestEditOpenChatMessageDto.builder()
                .build();
    }

    public static RequestEditOpenChatMessageDto createEditRequestWithSameContent(String content) {
        return RequestEditOpenChatMessageDto.builder()
                .content(content)
                .build();
    }

    public static OpenChatMessage createTextMessage(Long senderId, Long roomId) {
        return OpenChatMessage.createForTest(1L, roomId, senderId, "기존 메시지 내용", OpenChatMessageType.TEXT, false, null);
    }

    public static OpenChatMessage createTextMessageWithContent(Long senderId, Long roomId, String content) {
        return OpenChatMessage.createForTest(1L, roomId, senderId, content, OpenChatMessageType.TEXT, false, null);
    }

    public static OpenChatMessage createDeletedTextMessage(Long senderId, Long roomId) {
        return OpenChatMessage.createForTest(1L, roomId, senderId, "삭제된 메시지", OpenChatMessageType.TEXT, true, null);
    }

    public static OpenChatMessage createImageMessage(Long senderId, Long roomId) {
        return OpenChatMessage.createForTest(1L, roomId, senderId, null, OpenChatMessageType.IMAGE, false, null);
    }

    public static OpenChatMessage createSystemMessage(Long senderId, Long roomId) {
        return OpenChatMessage.createForTest(1L, roomId, senderId, "시스템 메시지", OpenChatMessageType.SYSTEM, false, null);
    }

    public static OpenChatRoom createOpenChatRoom(Long roomId) {
        return OpenChatRoom.createForTest(roomId, "테스트 채팅방");
    }

    public static ResponseOpenChatMessageDto createEditedResponse() {
        return ResponseOpenChatMessageDto.builder()
                .messageId(1L)
                .roomId(5L)
                .senderId(42L)
                .senderNickname("홍길동")
                .content("수정된 메시지 내용입니다.")
                .type(OpenChatMessageType.TEXT)
                .isEdited(true)
                .editedAt(LocalDateTime.of(2026, 10, 5, 10, 15, 0))
                .isDeleted(false)
                .isBot(false)
                .build();
    }
}
