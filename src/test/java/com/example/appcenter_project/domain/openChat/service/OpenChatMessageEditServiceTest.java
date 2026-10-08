package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatMessageEditEventDto;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatMessageEditFixture;
import com.example.appcenter_project.domain.openChat.repository.OpenChatMessageQuerydslRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatMessageRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomRepository;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.exception.ErrorCode;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OpenChatMessageEditServiceTest {

    @Mock
    OpenChatMessageRepository openChatMessageRepository;

    @Mock
    OpenChatMessageQuerydslRepository openChatMessageQuerydslRepository;

    @Mock
    OpenChatRoomRepository openChatRoomRepository;

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    OpenChatMessageService openChatMessageService;

    @Test
    @DisplayName("CustomException 발생 — AC 존재하지 않는 messageId OPEN_CHAT_MESSAGE_NOT_FOUND")
    void should_throw_CustomException_when_message_not_found() {
        // given
        var dto = OpenChatMessageEditFixture.createEditRequest();
        given(openChatMessageRepository.findById(anyLong())).willReturn(Optional.empty());

        // when
        ThrowingCallable action = () -> openChatMessageService.editMessage(42L, 5L, 999L, dto);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_MESSAGE_NOT_FOUND);
    }

    @Test
    @DisplayName("CustomException 발생 — AC-2 타인 메시지 수정 OPEN_CHAT_MESSAGE_NOT_OWNED_BY_USER")
    void should_throw_CustomException_when_message_not_owned_by_requester() {
        // given
        Long requesterId = 42L;
        Long ownerId = 99L;
        var message = OpenChatMessageEditFixture.createTextMessage(ownerId, 5L);
        var dto = OpenChatMessageEditFixture.createEditRequest();
        given(openChatMessageRepository.findById(1L)).willReturn(Optional.of(message));

        // when
        ThrowingCallable action = () -> openChatMessageService.editMessage(requesterId, 5L, 1L, dto);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_MESSAGE_NOT_OWNED_BY_USER);
    }

    @Test
    @DisplayName("CustomException 발생 — AC-3 IMAGE 타입 메시지 수정 OPEN_CHAT_MESSAGE_EDIT_FORBIDDEN_TYPE")
    void should_throw_CustomException_when_message_type_is_image() {
        // given
        Long senderId = 42L;
        var message = OpenChatMessageEditFixture.createImageMessage(senderId, 5L);
        var dto = OpenChatMessageEditFixture.createEditRequest();
        given(openChatMessageRepository.findById(1L)).willReturn(Optional.of(message));

        // when
        ThrowingCallable action = () -> openChatMessageService.editMessage(senderId, 5L, 1L, dto);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_MESSAGE_EDIT_FORBIDDEN_TYPE);
    }

    @Test
    @DisplayName("CustomException 발생 — AC-3 SYSTEM 타입 메시지 수정 OPEN_CHAT_MESSAGE_EDIT_FORBIDDEN_TYPE")
    void should_throw_CustomException_when_message_type_is_system() {
        // given
        Long senderId = 42L;
        var message = OpenChatMessageEditFixture.createSystemMessage(senderId, 5L);
        var dto = OpenChatMessageEditFixture.createEditRequest();
        given(openChatMessageRepository.findById(1L)).willReturn(Optional.of(message));

        // when
        ThrowingCallable action = () -> openChatMessageService.editMessage(senderId, 5L, 1L, dto);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_MESSAGE_EDIT_FORBIDDEN_TYPE);
    }

    @Test
    @DisplayName("CustomException 발생 — AC-4 삭제된 메시지 수정 OPEN_CHAT_MESSAGE_ALREADY_DELETED")
    void should_throw_CustomException_when_message_is_deleted() {
        // given
        Long senderId = 42L;
        var message = OpenChatMessageEditFixture.createDeletedTextMessage(senderId, 5L);
        var dto = OpenChatMessageEditFixture.createEditRequest();
        given(openChatMessageRepository.findById(1L)).willReturn(Optional.of(message));

        // when
        ThrowingCallable action = () -> openChatMessageService.editMessage(senderId, 5L, 1L, dto);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_MESSAGE_ALREADY_DELETED);
    }

    @Test
    @DisplayName("CustomException 발생 — AC-5 기존 내용과 동일한 수정 OPEN_CHAT_MESSAGE_CONTENT_UNCHANGED")
    void should_throw_CustomException_when_content_is_unchanged() {
        // given
        Long senderId = 42L;
        String sameContent = "기존 메시지 내용";
        var message = OpenChatMessageEditFixture.createTextMessageWithContent(senderId, 5L, sameContent);
        var sameDtoContent = OpenChatMessageEditFixture.createEditRequestWithSameContent(sameContent);
        given(openChatMessageRepository.findById(1L)).willReturn(Optional.of(message));

        // when
        ThrowingCallable action = () -> openChatMessageService.editMessage(senderId, 5L, 1L, sameDtoContent);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_MESSAGE_CONTENT_UNCHANGED);
    }

    @Test
    @DisplayName("save 호출 — AC-1 정상 텍스트 메시지 수정 시 repository save 실행")
    void should_call_save_when_message_edit_succeeds() {
        // given
        Long senderId = 42L;
        var message = OpenChatMessageEditFixture.createTextMessage(senderId, 5L);
        var dto = OpenChatMessageEditFixture.createEditRequest();
        given(openChatMessageRepository.findById(1L)).willReturn(Optional.of(message));
        given(openChatMessageRepository.save(any())).willReturn(message);
        given(openChatMessageQuerydslRepository.findLatestMessageIdByRoomId(5L)).willReturn(Optional.of(2L));

        // when
        openChatMessageService.editMessage(senderId, 5L, 1L, dto);

        // then
        then(openChatMessageRepository).should().save(message);
    }

    @Test
    @DisplayName("WebSocket broadcast 호출 — AC-10 수정 성공 시 이벤트 전송")
    void should_broadcast_edit_event_when_message_edit_succeeds() {
        // given
        Long senderId = 42L;
        var message = OpenChatMessageEditFixture.createTextMessage(senderId, 5L);
        var dto = OpenChatMessageEditFixture.createEditRequest();
        given(openChatMessageRepository.findById(1L)).willReturn(Optional.of(message));
        given(openChatMessageRepository.save(any())).willReturn(message);
        given(openChatMessageQuerydslRepository.findLatestMessageIdByRoomId(5L)).willReturn(Optional.of(2L));

        // when
        openChatMessageService.editMessage(senderId, 5L, 1L, dto);

        // then
        then(messagingTemplate).should().convertAndSend(
                eq("/sub/openchat/5"),
                any(ResponseOpenChatMessageEditEventDto.class));
    }

    @Test
    @DisplayName("room.lastMessage 갱신 — AC-7 수정 대상이 채팅방 최신 메시지인 경우")
    void should_update_room_last_message_when_message_is_latest() {
        // given
        Long senderId = 42L;
        Long messageId = 1L;
        var message = OpenChatMessageEditFixture.createTextMessage(senderId, 5L);
        var dto = OpenChatMessageEditFixture.createEditRequest();
        var room = OpenChatMessageEditFixture.createOpenChatRoom(5L);
        given(openChatMessageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(openChatMessageRepository.save(any())).willReturn(message);
        given(openChatMessageQuerydslRepository.findLatestMessageIdByRoomId(5L)).willReturn(Optional.of(messageId));
        given(openChatRoomRepository.findById(5L)).willReturn(Optional.of(room));

        // when
        openChatMessageService.editMessage(senderId, 5L, messageId, dto);

        // then
        then(openChatRoomRepository).should().findById(5L);
    }

    @Test
    @DisplayName("room.lastMessage 미갱신 — AC-8 수정 대상이 채팅방 최신 메시지가 아닌 경우")
    void should_not_update_room_last_message_when_message_is_not_latest() {
        // given
        Long senderId = 42L;
        Long messageId = 1L;
        Long latestMessageId = 100L;
        var message = OpenChatMessageEditFixture.createTextMessage(senderId, 5L);
        var dto = OpenChatMessageEditFixture.createEditRequest();
        given(openChatMessageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(openChatMessageRepository.save(any())).willReturn(message);
        given(openChatMessageQuerydslRepository.findLatestMessageIdByRoomId(5L)).willReturn(Optional.of(latestMessageId));

        // when
        openChatMessageService.editMessage(senderId, 5L, messageId, dto);

        // then
        then(openChatRoomRepository).should(never()).findById(anyLong());
    }
}
