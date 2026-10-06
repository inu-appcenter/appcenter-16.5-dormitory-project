package com.example.appcenter_project.domain.openChat.controller;

import com.example.appcenter_project.domain.openChat.service.OpenChatMessageService;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.exception.ErrorCode;
import com.example.appcenter_project.global.exception.SlackErrorNotifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OpenChatMessageController.class)
@AutoConfigureMockMvc(addFilters = false)
class OpenChatReplyMetadataControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    OpenChatMessageService openChatMessageService;

    @MockBean
    SlackErrorNotifier slackErrorNotifier;

    @MockBean
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("204 반환 — BR-769-15 발신자 본인 메시지 soft-delete 성공")
    void should_return_204_when_owner_deletes_message() throws Exception {
        // given
        Long roomId = 1L;
        Long messageId = 100L;
        willDoNothing().given(openChatMessageService).deleteMessage(anyLong(), eq(roomId), eq(messageId));

        // when
        ResultActions result = mockMvc.perform(
                delete("/open-chat-rooms/{roomId}/messages/{messageId}", roomId, messageId));

        // then
        result.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("403 반환 — BR-769-16 타인이 메시지 삭제 시도")
    void should_return_403_when_non_owner_deletes_message() throws Exception {
        // given
        Long roomId = 1L;
        Long messageId = 100L;
        willThrow(new CustomException(ErrorCode.OPEN_CHAT_MESSAGE_NOT_OWNED_BY_USER))
                .given(openChatMessageService).deleteMessage(anyLong(), eq(roomId), eq(messageId));

        // when
        ResultActions result = mockMvc.perform(
                delete("/open-chat-rooms/{roomId}/messages/{messageId}", roomId, messageId));

        // then
        result.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("404 반환 — 존재하지 않는 메시지 삭제 시도")
    void should_return_404_when_deleting_non_existent_message() throws Exception {
        // given
        Long roomId = 1L;
        Long nonExistentMessageId = 9999L;
        willThrow(new CustomException(ErrorCode.OPEN_CHAT_MESSAGE_NOT_FOUND))
                .given(openChatMessageService).deleteMessage(anyLong(), eq(roomId), eq(nonExistentMessageId));

        // when
        ResultActions result = mockMvc.perform(
                delete("/open-chat-rooms/{roomId}/messages/{messageId}", roomId, nonExistentMessageId));

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("400 반환 — 이미 삭제된 메시지 재삭제 시도")
    void should_return_400_when_deleting_already_deleted_message() throws Exception {
        // given
        Long roomId = 1L;
        Long alreadyDeletedMessageId = 42L;
        willThrow(new CustomException(ErrorCode.OPEN_CHAT_MESSAGE_ALREADY_DELETED))
                .given(openChatMessageService).deleteMessage(anyLong(), eq(roomId), eq(alreadyDeletedMessageId));

        // when
        ResultActions result = mockMvc.perform(
                delete("/open-chat-rooms/{roomId}/messages/{messageId}", roomId, alreadyDeletedMessageId));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("200 반환 — BR-769-10 메시지 목록 조회 성공 (replySource 포함)")
    void should_return_200_when_fetching_messages_with_reply_source() throws Exception {
        // given
        Long roomId = 1L;
        given(openChatMessageService.getMessages(anyLong(), eq(roomId), isNull(), any(Integer.class), any()))
                .willReturn(null);

        // when
        ResultActions result = mockMvc.perform(
                get("/open-chat-rooms/{roomId}/messages", roomId)
                        .param("size", "30"));

        // then
        result.andExpect(status().isOk());
    }
}
