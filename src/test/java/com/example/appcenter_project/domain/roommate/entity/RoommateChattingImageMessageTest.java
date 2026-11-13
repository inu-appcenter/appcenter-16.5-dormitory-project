package com.example.appcenter_project.domain.roommate.entity;

import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatRoomDto;
import com.example.appcenter_project.domain.roommate.enums.RoommateChattingMessageType;
import com.example.appcenter_project.domain.roommate.fixture.RoommateChattingImageFixture;
import com.example.appcenter_project.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// BR-791
class RoommateChattingImageMessageTest {

    private final User host = User.createForTest(1L, "호스트");
    private final User guest = User.createForTest(2L, "게스트");
    private final RoommateChattingRoom room = RoommateChattingRoom.create(host, guest);

    @Test
    @DisplayName("BR-791 이미지 메시지 생성 — IMAGE 타입, content [이미지]")
    void should_create_image_message_with_image_type_and_placeholder() {
        RoommateChattingChat chat = RoommateChattingChat.createImageMessage(room, guest, false);

        assertThat(chat.getMessageType()).isEqualTo(RoommateChattingMessageType.IMAGE);
        assertThat(chat.getContent()).isEqualTo("[이미지]");
        assertThat(chat.isImage()).isTrue();
        assertThat(chat.isSystem()).isFalse();
    }

    @Test
    @DisplayName("BR-791 채팅 목록 MY 탭 — 마지막 메시지가 이미지면 [이미지]")
    void should_show_image_placeholder_as_last_message() {
        RoommateChattingChat chat = RoommateChattingImageFixture.createSavedImageChat(10L, room, guest, false);

        ResponseOpenChatRoomDto dto = ResponseOpenChatRoomDto.fromRoommate(room.getId(), "게스트", chat, 0, false);

        assertThat(dto.getLastMessage()).isEqualTo("[이미지]");
    }
}