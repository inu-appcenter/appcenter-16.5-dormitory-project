package com.example.appcenter_project.domain.roommate.service;

import com.example.appcenter_project.domain.roommate.entity.RoommateChattingChat;
import com.example.appcenter_project.domain.roommate.fixture.RoommateChattingReplyFixture;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingChatRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingRoomRepository;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.exception.ErrorCode;
import com.example.appcenter_project.shared.enums.ReplySourceStatus;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class RoommateChattingReplyServiceTest {

    @Mock
    RoommateChattingChatRepository roommateChattingChatRepository;

    @Mock
    RoommateChattingRoomRepository roommateChattingRoomRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    RoommateChattingChatService roommateChattingChatService;

    // ─── 답장 전송 — 성공 케이스 ───────────────────────────────────────────────

    @Test
    @DisplayName("replyToSenderId 저장 — BR-769-4 ROOMMATE 방 일반 메시지 답장")
    void should_save_reply_with_sender_id_when_replying_in_roommate_room() {
        // given
        Long roomId = 7L;
        Long originalMessageId = 180L;
        Long originalSenderId = 3L;
        RoommateChattingChat originalChat = RoommateChattingReplyFixture.createNormalChat(
                originalMessageId, roomId, originalSenderId);
        given(roommateChattingChatRepository.findById(originalMessageId))
                .willReturn(Optional.of(originalChat));

        // when
        roommateChattingChatService.sendChatWithReply(roomId, 5L, "답장 내용", originalMessageId);

        // then
        then(roommateChattingChatRepository).should().save(
                argThat(chat -> originalSenderId.equals(chat.getReplyToSenderId())));
    }

    @Test
    @DisplayName("replyToRoomId 저장 — BR-769-4 ROOMMATE 방 답장 시 방 ID 저장")
    void should_save_room_id_when_replying_in_roommate_room() {
        // given
        Long roomId = 7L;
        Long originalMessageId = 180L;
        RoommateChattingChat originalChat = RoommateChattingReplyFixture.createNormalChat(
                originalMessageId, roomId, 3L);
        given(roommateChattingChatRepository.findById(originalMessageId))
                .willReturn(Optional.of(originalChat));

        // when
        roommateChattingChatService.sendChatWithReply(roomId, 5L, "답장 내용", originalMessageId);

        // then
        then(roommateChattingChatRepository).should().save(
                argThat(chat -> roomId.equals(chat.getReplyToRoomId())));
    }

    // ─── 답장 전송 — 실패 케이스 ───────────────────────────────────────────────

    @Test
    @DisplayName("CustomException 발생 — BR-769-5 존재하지 않는 원본 ID로 답장 (룸메톡방)")
    void should_throw_CustomException_when_roommate_reply_target_not_found() {
        // given
        Long roomId = 7L;
        Long nonExistentMessageId = 9999L;
        given(roommateChattingChatRepository.findById(nonExistentMessageId))
                .willReturn(Optional.empty());

        // when
        ThrowingCallable action = () -> roommateChattingChatService.sendChatWithReply(
                roomId, 5L, "답장 내용", nonExistentMessageId);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOMMATE_CHAT_MESSAGE_NOT_FOUND);
    }

    @Test
    @DisplayName("CustomException 발생 — BR-769-6 삭제된 메시지에 답장 (룸메톡방)")
    void should_throw_CustomException_when_roommate_reply_target_is_deleted() {
        // given
        Long roomId = 7L;
        Long deletedMessageId = 180L;
        RoommateChattingChat deletedChat = RoommateChattingReplyFixture.createDeletedChat(
                deletedMessageId, roomId, 3L);
        given(roommateChattingChatRepository.findById(deletedMessageId))
                .willReturn(Optional.of(deletedChat));

        // when
        ThrowingCallable action = () -> roommateChattingChatService.sendChatWithReply(
                roomId, 5L, "답장 내용", deletedMessageId);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOMMATE_CHAT_MESSAGE_ALREADY_DELETED);
    }

    @Test
    @DisplayName("CustomException 발생 — BR-769-7 시스템 메시지에 답장 불가 (룸메톡방)")
    void should_throw_CustomException_when_roommate_reply_target_is_system_message() {
        // given
        Long roomId = 7L;
        Long systemMessageId = 180L;
        RoommateChattingChat systemChat = RoommateChattingReplyFixture.createSystemChat(
                systemMessageId, roomId, null);
        given(roommateChattingChatRepository.findById(systemMessageId))
                .willReturn(Optional.of(systemChat));

        // when
        ThrowingCallable action = () -> roommateChattingChatService.sendChatWithReply(
                roomId, 5L, "답장 내용", systemMessageId);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOMMATE_CHAT_REPLY_NOT_ALLOWED_FOR_TYPE);
    }

    @Test
    @DisplayName("CustomException 발생 — BR-769-8 다른 방의 메시지에 답장 (룸메톡방)")
    void should_throw_CustomException_when_roommate_reply_target_is_in_different_room() {
        // given
        Long currentRoomId = 7L;
        Long otherRoomId = 8L;
        Long messageInOtherRoom = 180L;
        RoommateChattingChat chatFromOtherRoom = RoommateChattingReplyFixture.createNormalChat(
                messageInOtherRoom, otherRoomId, 3L);
        given(roommateChattingChatRepository.findById(messageInOtherRoom))
                .willReturn(Optional.of(chatFromOtherRoom));

        // when
        ThrowingCallable action = () -> roommateChattingChatService.sendChatWithReply(
                currentRoomId, 5L, "답장 내용", messageInOtherRoom);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOMMATE_CHAT_REPLY_TARGET_NOT_IN_SAME_ROOM);
    }

    @Test
    @DisplayName("CustomException 발생 — BR-769-9 중첩 답장 시도 (룸메톡방)")
    void should_throw_CustomException_when_roommate_nested_reply_attempted() {
        // given
        Long roomId = 7L;
        Long replyMessageId = 180L;
        RoommateChattingChat existingReply = RoommateChattingReplyFixture.createNestedReplyChat(
                replyMessageId, roomId, 3L, 150L);
        given(roommateChattingChatRepository.findById(replyMessageId))
                .willReturn(Optional.of(existingReply));

        // when
        ThrowingCallable action = () -> roommateChattingChatService.sendChatWithReply(
                roomId, 5L, "중첩 답장 시도", replyMessageId);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOMMATE_CHAT_NESTED_REPLY_NOT_ALLOWED);
    }

    // ─── soft-delete ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("isDeleted=true 저장 — BR-769-15 발신자 본인이 soft-delete 성공 (룸메톡방)")
    void should_soft_delete_chat_when_requester_is_owner() {
        // given
        Long chatId = 200L;
        Long ownerId = 1L;
        RoommateChattingChat chat = RoommateChattingReplyFixture.createNormalChat(chatId, 7L, ownerId);
        given(roommateChattingChatRepository.findById(chatId))
                .willReturn(Optional.of(chat));

        // when
        roommateChattingChatService.deleteChat(chatId, ownerId);

        // then
        then(roommateChattingChatRepository).should().save(
                argThat(c -> c.isDeleted()));
    }

    @Test
    @DisplayName("CustomException 발생 — BR-769-16 타인의 룸메이트 채팅 삭제 시도")
    void should_throw_CustomException_when_deleting_others_roommate_chat() {
        // given
        Long chatId = 200L;
        Long ownerId = 1L;
        Long requesterId = 2L;
        RoommateChattingChat chat = RoommateChattingReplyFixture.createNormalChat(chatId, 7L, ownerId);
        given(roommateChattingChatRepository.findById(chatId))
                .willReturn(Optional.of(chat));

        // when
        ThrowingCallable action = () -> roommateChattingChatService.deleteChat(chatId, requesterId);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOMMATE_CHAT_MESSAGE_NOT_OWNED_BY_USER);
    }

    // ─── 답장 조회 — replySource (룸메톡방) ─────────────────────────────────────

    @Test
    @DisplayName("replySource.status=NORMAL 반환 — BR-769-10 룸메 채팅 정상 답장 조회")
    void should_return_normal_status_when_roommate_original_chat_exists() {
        // given
        Long roomId = 7L;
        Long originalChatId = 180L;
        Long replyChatId = 200L;
        RoommateChattingChat originalChat = RoommateChattingReplyFixture.createNormalChat(
                originalChatId, roomId, 3L);
        RoommateChattingChat replyChat = RoommateChattingReplyFixture.createNestedReplyChat(
                replyChatId, roomId, 5L, originalChatId);
        given(roommateChattingChatRepository.findAllById(List.of(originalChatId)))
                .willReturn(List.of(originalChat));
        given(userRepository.findAllById(anyList())).willReturn(List.of());

        // when
        var result = roommateChattingChatService.buildReplySources(List.of(replyChat));

        // then
        assertThat(result.get(replyChatId).getStatus()).isEqualTo(ReplySourceStatus.NORMAL);
    }

    @Test
    @DisplayName("replySource.status=NOT_FOUND 반환 — BR-769-12 룸메 채팅 원본 ID 없음")
    void should_return_not_found_status_when_roommate_original_chat_not_in_db() {
        // given
        Long roomId = 7L;
        Long nonExistentChatId = 9999L;
        Long replyChatId = 200L;
        RoommateChattingChat replyChat = RoommateChattingReplyFixture.createNestedReplyChat(
                replyChatId, roomId, 5L, nonExistentChatId);
        given(roommateChattingChatRepository.findAllById(List.of(nonExistentChatId)))
                .willReturn(List.of());

        // when
        var result = roommateChattingChatService.buildReplySources(List.of(replyChat));

        // then
        assertThat(result.get(replyChatId).getStatus()).isEqualTo(ReplySourceStatus.NOT_FOUND);
    }
}
