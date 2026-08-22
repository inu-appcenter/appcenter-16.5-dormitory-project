package com.example.appcenter_project.domain.openChat.entity;

import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatMessageDto;
import com.example.appcenter_project.domain.openChat.entity.OpenChatMessage;
import com.example.appcenter_project.domain.openChat.enums.OpenChatMessageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseOpenChatMessageDtoBotTest {

    @Test
    @DisplayName("isBot=true — AC-6 type=BOT 메시지의 isBot 필드는 true")
    void should_set_isBot_true_when_type_is_BOT() {
        // given
        OpenChatMessage message = OpenChatMessage.create(1L, 10L, "공지입니다.", OpenChatMessageType.BOT);

        // when
        ResponseOpenChatMessageDto dto = ResponseOpenChatMessageDto.from(message, "관리자", 0);

        // then
        assertThat(dto.isBot()).isTrue();
    }

    @Test
    @DisplayName("isBot=false — AC-6 type=TEXT 메시지의 isBot 필드는 false")
    void should_set_isBot_false_when_type_is_TEXT() {
        // given
        OpenChatMessage message = OpenChatMessage.create(1L, 10L, "안녕하세요.", OpenChatMessageType.TEXT);

        // when
        ResponseOpenChatMessageDto dto = ResponseOpenChatMessageDto.from(message, "사용자", 0);

        // then
        assertThat(dto.isBot()).isFalse();
    }

    @Test
    @DisplayName("isBot=false — type=IMAGE 메시지의 isBot 필드는 false")
    void should_set_isBot_false_when_type_is_IMAGE() {
        // given
        OpenChatMessage message = OpenChatMessage.create(1L, 10L, "", OpenChatMessageType.IMAGE);

        // when
        ResponseOpenChatMessageDto dto = ResponseOpenChatMessageDto.from(message, "사용자", 0);

        // then
        assertThat(dto.isBot()).isFalse();
    }

    @Test
    @DisplayName("isBot=false — type=SYSTEM 메시지의 isBot 필드는 false")
    void should_set_isBot_false_when_type_is_SYSTEM() {
        // given
        OpenChatMessage message = OpenChatMessage.create(1L, 0L, "시스템 메시지", OpenChatMessageType.SYSTEM);

        // when
        ResponseOpenChatMessageDto dto = ResponseOpenChatMessageDto.from(message, null, 0);

        // then
        assertThat(dto.isBot()).isFalse();
    }

    @Test
    @DisplayName("isBot=false — type=ROOM_LINK 메시지의 isBot 필드는 false")
    void should_set_isBot_false_when_type_is_ROOM_LINK() {
        // given
        OpenChatMessage message = OpenChatMessage.create(1L, 10L, "{}", OpenChatMessageType.ROOM_LINK);

        // when
        ResponseOpenChatMessageDto dto = ResponseOpenChatMessageDto.fromRoomLink(
                message, "사용자", 0, 2L, "파생 방", "설명", 10);

        // then
        assertThat(dto.isBot()).isFalse();
    }

    @Test
    @DisplayName("isBot=false — type=STUDENT_ID_REQUEST 메시지의 isBot 필드는 false")
    void should_set_isBot_false_when_type_is_STUDENT_ID_REQUEST() {
        // given
        OpenChatMessage message = OpenChatMessage.create(1L, 10L, "{}", OpenChatMessageType.STUDENT_ID_REQUEST);

        // when
        ResponseOpenChatMessageDto dto = ResponseOpenChatMessageDto.fromStudentIdRequest(
                message, "사용자", 0, 100L);

        // then
        assertThat(dto.isBot()).isFalse();
    }
}
