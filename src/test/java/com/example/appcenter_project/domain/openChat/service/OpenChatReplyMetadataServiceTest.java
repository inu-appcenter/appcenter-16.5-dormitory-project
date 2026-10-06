package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.openChat.entity.OpenChatMessage;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatMessageType;
import com.example.appcenter_project.domain.openChat.fixture.ChatReplyMetadataFixture;
import com.example.appcenter_project.domain.openChat.repository.OpenChatMessageRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatParticipantRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OpenChatReplyMetadataServiceTest {

    @Mock
    OpenChatMessageRepository openChatMessageRepository;

    @Mock
    OpenChatRoomRepository openChatRoomRepository;

    @Mock
    OpenChatParticipantRepository openChatParticipantRepository;

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    OpenChatMessageService openChatMessageService;

    // ─── 답장 전송 — 실패 케이스 ───────────────────────────────────────────────

    @Test
    @DisplayName("CustomException 발생 — BR-769-5 존재하지 않는 원본 ID로 답장")
    void should_throw_CustomException_when_reply_target_message_not_found() {
        // given
        Long roomId = 1L;
        Long nonExistentMessageId = 9999L;
        given(openChatMessageRepository.findById(nonExistentMessageId))
                .willReturn(Optional.empty());

        // when
        ThrowingCallable action = () -> openChatMessageService.sendMessageWithReply(
                roomId, 10L, "답장 내용", nonExistentMessageId);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_MESSAGE_NOT_FOUND);
    }

    @Test
    @DisplayName("CustomException 발생 — BR-769-6 이미 삭제된 메시지에 답장")
    void should_throw_CustomException_when_reply_target_is_deleted() {
        // given
        Long roomId = 1L;
        Long deletedMessageId = 42L;
        OpenChatMessage deletedMessage = ChatReplyMetadataFixture.createDeletedMessage(deletedMessageId, roomId, 3L);
        given(openChatMessageRepository.findById(deletedMessageId))
                .willReturn(Optional.of(deletedMessage));

        // when
        ThrowingCallable action = () -> openChatMessageService.sendMessageWithReply(
                roomId, 10L, "답장 내용", deletedMessageId);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_MESSAGE_ALREADY_DELETED);
    }

    @Test
    @DisplayName("CustomException 발생 — BR-769-7 SYSTEM 타입 메시지에 답장 불가")
    void should_throw_CustomException_when_reply_target_is_system_type() {
        // given
        Long roomId = 1L;
        Long systemMessageId = 55L;
        OpenChatMessage systemMessage = ChatReplyMetadataFixture.createSystemMessage(systemMessageId, roomId, null);
        given(openChatMessageRepository.findById(systemMessageId))
                .willReturn(Optional.of(systemMessage));

        // when
        ThrowingCallable action = () -> openChatMessageService.sendMessageWithReply(
                roomId, 10L, "답장 내용", systemMessageId);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_REPLY_NOT_ALLOWED_FOR_TYPE);
    }

    @Test
    @DisplayName("CustomException 발생 — BR-769-8 다른 방의 메시지에 답장")
    void should_throw_CustomException_when_reply_target_is_in_different_room() {
        // given
        Long currentRoomId = 1L;
        Long otherRoomId = 2L;
        Long messageInOtherRoom = 77L;
        OpenChatMessage messageFromOtherRoom = ChatReplyMetadataFixture.createTextMessage(
                messageInOtherRoom, otherRoomId, 3L);
        given(openChatMessageRepository.findById(messageInOtherRoom))
                .willReturn(Optional.of(messageFromOtherRoom));

        // when
        ThrowingCallable action = () -> openChatMessageService.sendMessageWithReply(
                currentRoomId, 10L, "답장 내용", messageInOtherRoom);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_REPLY_TARGET_NOT_IN_SAME_ROOM);
    }

    @Test
    @DisplayName("CustomException 발생 — BR-769-9 이미 답장인 메시지에 중첩 답장")
    void should_throw_CustomException_when_nested_reply_attempted() {
        // given
        Long roomId = 1L;
        Long replyMessageId = 88L;
        OpenChatMessage existingReplyMessage = ChatReplyMetadataFixture.createNestedReplyMessage(
                replyMessageId, roomId, 3L, 50L);
        given(openChatMessageRepository.findById(replyMessageId))
                .willReturn(Optional.of(existingReplyMessage));

        // when
        ThrowingCallable action = () -> openChatMessageService.sendMessageWithReply(
                roomId, 10L, "중첩 답장 시도", replyMessageId);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_NESTED_REPLY_NOT_ALLOWED);
    }

    // ─── 답장 전송 — 성공 케이스 ───────────────────────────────────────────────

    @Test
    @DisplayName("replyToRoomType=OPEN 저장 — BR-769-1 OPEN 방 TEXT 메시지 답장")
    void should_save_reply_with_open_room_type_when_replying_in_open_room() {
        // given
        Long roomId = 1L;
        Long originalMessageId = 42L;
        Long senderId = 3L;
        OpenChatMessage originalMessage = ChatReplyMetadataFixture.createTextMessage(
                originalMessageId, roomId, senderId);
        given(openChatMessageRepository.findById(originalMessageId))
                .willReturn(Optional.of(originalMessage));

        // when
        openChatMessageService.sendMessageWithReply(roomId, 10L, "텍스트 답장", originalMessageId);

        // then
        then(openChatMessageRepository).should().save(
                argThat(msg -> com.example.appcenter_project.shared.enums.ChatRoomType.OPEN.equals(msg.getReplyToRoomType())));
    }

    @Test
    @DisplayName("replyToDerivedRoomId 저장 — BR-769-2 REOPEN_CARD 답장 시 파생 톡방 ID 파싱")
    void should_parse_derived_room_id_when_replying_to_reopen_card() {
        // given
        Long roomId = 1L;
        Long reopenCardMessageId = 60L;
        Long derivedRoomId = 5L;
        OpenChatMessage reopenCard = ChatReplyMetadataFixture.createReopenCardMessage(
                reopenCardMessageId, roomId, 3L, derivedRoomId);
        given(openChatMessageRepository.findById(reopenCardMessageId))
                .willReturn(Optional.of(reopenCard));

        // when
        openChatMessageService.sendMessageWithReply(roomId, 10L, "파생톡방 카드 답장", reopenCardMessageId);

        // then
        then(openChatMessageRepository).should().save(
                argThat(msg -> derivedRoomId.equals(msg.getReplyToDerivedRoomId())));
    }

    // ─── soft-delete ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("CustomException 발생 — BR-769-16 타인의 메시지 삭제 시도")
    void should_throw_CustomException_when_deleting_others_message() {
        // given
        Long roomId = 1L;
        Long messageId = 100L;
        Long ownerId = 1L;
        Long requesterId = 2L;
        OpenChatMessage message = ChatReplyMetadataFixture.createTextMessage(messageId, roomId, ownerId);
        OpenChatRoom room = mock(OpenChatRoom.class);
        given(room.getId()).willReturn(roomId);
        given(openChatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(openChatMessageRepository.findById(messageId))
                .willReturn(Optional.of(message));
        given(openChatParticipantRepository.existsByRoomIdAndUserId(roomId, requesterId)).willReturn(true);

        // when
        ThrowingCallable action = () -> openChatMessageService.deleteMessage(requesterId, roomId, messageId);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_MESSAGE_NOT_OWNED_BY_USER);
    }

    @Test
    @DisplayName("isDeleted=true 저장 — BR-769-15 발신자 본인이 soft-delete 성공")
    void should_soft_delete_message_when_requester_is_owner() {
        // given
        Long roomId = 1L;
        Long messageId = 100L;
        Long ownerId = 1L;
        OpenChatMessage message = ChatReplyMetadataFixture.createTextMessage(messageId, roomId, ownerId);
        OpenChatRoom room = mock(OpenChatRoom.class);
        given(room.getId()).willReturn(roomId);
        given(openChatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(openChatMessageRepository.findById(messageId))
                .willReturn(Optional.of(message));
        given(openChatParticipantRepository.existsByRoomIdAndUserId(roomId, ownerId)).willReturn(true);

        // when
        openChatMessageService.deleteMessage(ownerId, roomId, messageId);

        // then
        assertThat(message.isDeleted()).isTrue();
        then(messagingTemplate).should().convertAndSend(
                eq("/sub/openchat/" + roomId), any(Object.class));
    }
}
