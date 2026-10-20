package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.openChat.entity.OpenChatMessage;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.fixture.ChatReplyMetadataFixture;
import com.example.appcenter_project.domain.openChat.repository.OpenChatMessageRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomRepository;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.shared.enums.ReplySourceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OpenChatReplySourceQueryTest {

    @Mock
    OpenChatMessageRepository openChatMessageRepository;

    @Mock
    OpenChatRoomRepository openChatRoomRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    OpenChatMessageService openChatMessageService;

    @Test
    @DisplayName("replySource.status=NORMAL 반환 — BR-769-10 정상 TEXT 메시지 답장 조회")
    void should_return_normal_status_when_original_message_exists_and_not_deleted() {
        // given
        Long roomId = 1L;
        Long originalMessageId = 42L;
        Long replyMessageId = 100L;
        OpenChatMessage originalMessage = ChatReplyMetadataFixture.createTextMessage(
                originalMessageId, roomId, 3L);
        OpenChatMessage replyMessage = ChatReplyMetadataFixture.createNestedReplyMessage(
                replyMessageId, roomId, 5L, originalMessageId);
        given(openChatMessageRepository.findAllById(List.of(originalMessageId)))
                .willReturn(List.of(originalMessage));
        given(userRepository.findAllById(anyList())).willReturn(List.of());

        // when
        var result = openChatMessageService.buildReplySources(List.of(replyMessage));

        // then
        assertThat(result.get(replyMessageId).getStatus()).isEqualTo(ReplySourceStatus.NORMAL);
    }

    @Test
    @DisplayName("replySource.contentPreview 포함 — BR-769-10 NORMAL 상태일 때 원본 내용 미리보기")
    void should_include_content_preview_when_reply_source_status_is_normal() {
        // given
        Long roomId = 1L;
        Long originalMessageId = 42L;
        Long replyMessageId = 100L;
        OpenChatMessage originalMessage = ChatReplyMetadataFixture.createTextMessage(
                originalMessageId, roomId, 3L);
        OpenChatMessage replyMessage = ChatReplyMetadataFixture.createNestedReplyMessage(
                replyMessageId, roomId, 5L, originalMessageId);
        given(openChatMessageRepository.findAllById(List.of(originalMessageId)))
                .willReturn(List.of(originalMessage));
        given(userRepository.findAllById(anyList())).willReturn(List.of());

        // when
        var result = openChatMessageService.buildReplySources(List.of(replyMessage));

        // then
        assertThat(result.get(replyMessageId).getContentPreview()).isNotNull();
    }

    @Test
    @DisplayName("replySource.status=DELETED 반환 — BR-769-11 원본 메시지가 isDeleted=true")
    void should_return_deleted_status_when_original_message_is_deleted() {
        // given
        Long roomId = 1L;
        Long originalMessageId = 42L;
        Long replyMessageId = 100L;
        OpenChatMessage deletedOriginal = ChatReplyMetadataFixture.createDeletedMessage(
                originalMessageId, roomId, 3L);
        OpenChatMessage replyMessage = ChatReplyMetadataFixture.createNestedReplyMessage(
                replyMessageId, roomId, 5L, originalMessageId);
        given(openChatMessageRepository.findAllById(List.of(originalMessageId)))
                .willReturn(List.of(deletedOriginal));

        // when
        var result = openChatMessageService.buildReplySources(List.of(replyMessage));

        // then
        assertThat(result.get(replyMessageId).getStatus()).isEqualTo(ReplySourceStatus.DELETED);
    }

    @Test
    @DisplayName("replySource.contentPreview=null 반환 — BR-769-11 DELETED 상태일 때 미리보기 없음")
    void should_return_null_content_preview_when_reply_source_status_is_deleted() {
        // given
        Long roomId = 1L;
        Long originalMessageId = 42L;
        Long replyMessageId = 100L;
        OpenChatMessage deletedOriginal = ChatReplyMetadataFixture.createDeletedMessage(
                originalMessageId, roomId, 3L);
        OpenChatMessage replyMessage = ChatReplyMetadataFixture.createNestedReplyMessage(
                replyMessageId, roomId, 5L, originalMessageId);
        given(openChatMessageRepository.findAllById(List.of(originalMessageId)))
                .willReturn(List.of(deletedOriginal));

        // when
        var result = openChatMessageService.buildReplySources(List.of(replyMessage));

        // then
        assertThat(result.get(replyMessageId).getContentPreview()).isNull();
    }

    @Test
    @DisplayName("replySource.status=NOT_FOUND 반환 — BR-769-12 원본 메시지 ID가 DB에 없음")
    void should_return_not_found_status_when_original_message_id_not_in_db() {
        // given
        Long roomId = 1L;
        Long nonExistentMessageId = 9999L;
        Long replyMessageId = 100L;
        OpenChatMessage replyMessage = ChatReplyMetadataFixture.createNestedReplyMessage(
                replyMessageId, roomId, 5L, nonExistentMessageId);
        given(openChatMessageRepository.findAllById(List.of(nonExistentMessageId)))
                .willReturn(List.of());

        // when
        var result = openChatMessageService.buildReplySources(List.of(replyMessage));

        // then
        assertThat(result.get(replyMessageId).getStatus()).isEqualTo(ReplySourceStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("replySource.status=RECRUITING 반환 — BR-769-13 REOPEN_CARD 원본 파생 톡방 모집 중")
    void should_return_recruiting_status_when_derived_room_is_recruiting() {
        // given
        Long roomId = 1L;
        Long derivedRoomId = 5L;
        Long originalMessageId = 42L;
        Long replyMessageId = 100L;
        OpenChatMessage reopenCard = ChatReplyMetadataFixture.createReopenCardMessage(
                originalMessageId, roomId, 3L, derivedRoomId);
        OpenChatMessage replyMessage = ChatReplyMetadataFixture.createNestedReplyMessage(
                replyMessageId, roomId, 5L, originalMessageId);
        OpenChatRoom derivedRoom = OpenChatRoom.createForTest(derivedRoomId, false);
        given(openChatMessageRepository.findAllById(List.of(originalMessageId)))
                .willReturn(List.of(reopenCard));
        given(openChatRoomRepository.findAllById(List.of(derivedRoomId)))
                .willReturn(List.of(derivedRoom));
        given(userRepository.findAllById(anyList())).willReturn(List.of());

        // when
        var result = openChatMessageService.buildReplySources(List.of(replyMessage));

        // then
        assertThat(result.get(replyMessageId).getStatus()).isEqualTo(ReplySourceStatus.RECRUITING);
    }

    @Test
    @DisplayName("replyToDerivedRoomId 포함 — BR-769-13 RECRUITING 상태일 때 파생 톡방 ID 반환")
    void should_include_derived_room_id_when_reply_source_status_is_recruiting() {
        // given
        Long roomId = 1L;
        Long derivedRoomId = 5L;
        Long originalMessageId = 42L;
        Long replyMessageId = 100L;
        OpenChatMessage reopenCard = ChatReplyMetadataFixture.createReopenCardMessage(
                originalMessageId, roomId, 3L, derivedRoomId);
        OpenChatMessage replyMessage = ChatReplyMetadataFixture.createNestedReplyMessage(
                replyMessageId, roomId, 5L, originalMessageId);
        OpenChatRoom derivedRoom = OpenChatRoom.createForTest(derivedRoomId, false);
        given(openChatMessageRepository.findAllById(List.of(originalMessageId)))
                .willReturn(List.of(reopenCard));
        given(openChatRoomRepository.findAllById(List.of(derivedRoomId)))
                .willReturn(List.of(derivedRoom));
        given(userRepository.findAllById(anyList())).willReturn(List.of());

        // when
        var result = openChatMessageService.buildReplySources(List.of(replyMessage));

        // then
        assertThat(result.get(replyMessageId).getReplyToDerivedRoomId()).isEqualTo(derivedRoomId);
    }

    @Test
    @DisplayName("replySource.status=RECRUITMENT_CLOSED 반환 — BR-769-14 REOPEN_CARD 원본 파생 톡방 모집 마감")
    void should_return_recruitment_closed_status_when_derived_room_is_closed() {
        // given
        Long roomId = 1L;
        Long derivedRoomId = 5L;
        Long originalMessageId = 42L;
        Long replyMessageId = 100L;
        OpenChatMessage reopenCard = ChatReplyMetadataFixture.createReopenCardMessage(
                originalMessageId, roomId, 3L, derivedRoomId);
        OpenChatMessage replyMessage = ChatReplyMetadataFixture.createNestedReplyMessage(
                replyMessageId, roomId, 5L, originalMessageId);
        OpenChatRoom closedDerivedRoom = OpenChatRoom.createForTest(derivedRoomId, true);
        given(openChatMessageRepository.findAllById(List.of(originalMessageId)))
                .willReturn(List.of(reopenCard));
        given(openChatRoomRepository.findAllById(List.of(derivedRoomId)))
                .willReturn(List.of(closedDerivedRoom));
        given(userRepository.findAllById(anyList())).willReturn(List.of());

        // when
        var result = openChatMessageService.buildReplySources(List.of(replyMessage));

        // then
        assertThat(result.get(replyMessageId).getStatus()).isEqualTo(ReplySourceStatus.RECRUITMENT_CLOSED);
    }
}
