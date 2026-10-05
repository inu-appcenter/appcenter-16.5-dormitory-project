package com.example.appcenter_project.domain.openChat.controller;

import com.example.appcenter_project.domain.openChat.fixture.OpenChatMessageEditFixture;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OpenChatMessageController.class)
@AutoConfigureMockMvc(addFilters = false)
class OpenChatMessageEditControllerTest {

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
    @DisplayName("200 반환 — AC-1 정상 텍스트 메시지 수정")
    void should_return_200_when_valid_text_message_edit() throws Exception {
        // given
        var request = OpenChatMessageEditFixture.createEditRequest();
        given(openChatMessageService.editMessage(any(), anyLong(), anyLong(), any()))
                .willReturn(OpenChatMessageEditFixture.createEditedResponse());

        // when
        ResultActions result = mockMvc.perform(patch("/open-chat-rooms/{roomId}/messages/{messageId}", 5L, 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isOk());
    }

    @Test
    @DisplayName("응답 isEdited true 반환 — AC-1 정상 수정 후 수정 여부 플래그")
    void should_return_isEdited_true_when_message_edited() throws Exception {
        // given
        var request = OpenChatMessageEditFixture.createEditRequest();
        given(openChatMessageService.editMessage(any(), anyLong(), anyLong(), any()))
                .willReturn(OpenChatMessageEditFixture.createEditedResponse());

        // when
        ResultActions result = mockMvc.perform(patch("/open-chat-rooms/{roomId}/messages/{messageId}", 5L, 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(jsonPath("$.isEdited").value(true));
    }

    @Test
    @DisplayName("400 반환 — AC-6 content 공백만 포함")
    void should_return_400_when_content_is_blank() throws Exception {
        // given
        var request = OpenChatMessageEditFixture.createEditRequestWithBlankContent();

        // when
        ResultActions result = mockMvc.perform(patch("/open-chat-rooms/{roomId}/messages/{messageId}", 5L, 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 반환 — AC-6 content null")
    void should_return_400_when_content_is_null() throws Exception {
        // given
        var request = OpenChatMessageEditFixture.createEditRequestWithNullContent();

        // when
        ResultActions result = mockMvc.perform(patch("/open-chat-rooms/{roomId}/messages/{messageId}", 5L, 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("403 반환 — AC-2 타인 메시지 수정 시도")
    void should_return_403_when_message_not_owned_by_user() throws Exception {
        // given
        var request = OpenChatMessageEditFixture.createEditRequest();
        given(openChatMessageService.editMessage(any(), anyLong(), anyLong(), any()))
                .willThrow(new CustomException(ErrorCode.OPEN_CHAT_MESSAGE_NOT_OWNED_BY_USER));

        // when
        ResultActions result = mockMvc.perform(patch("/open-chat-rooms/{roomId}/messages/{messageId}", 5L, 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("404 반환 — AC 존재하지 않는 messageId")
    void should_return_404_when_message_not_found() throws Exception {
        // given
        var request = OpenChatMessageEditFixture.createEditRequest();
        given(openChatMessageService.editMessage(any(), anyLong(), anyLong(), any()))
                .willThrow(new CustomException(ErrorCode.OPEN_CHAT_MESSAGE_NOT_FOUND));

        // when
        ResultActions result = mockMvc.perform(patch("/open-chat-rooms/{roomId}/messages/{messageId}", 5L, 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("400 반환 — AC-3 IMAGE 타입 메시지 수정 시도")
    void should_return_400_when_message_type_is_image() throws Exception {
        // given
        var request = OpenChatMessageEditFixture.createEditRequest();
        given(openChatMessageService.editMessage(any(), anyLong(), anyLong(), any()))
                .willThrow(new CustomException(ErrorCode.OPEN_CHAT_MESSAGE_EDIT_FORBIDDEN_TYPE));

        // when
        ResultActions result = mockMvc.perform(patch("/open-chat-rooms/{roomId}/messages/{messageId}", 5L, 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 반환 — AC-4 삭제된 메시지 수정 시도")
    void should_return_400_when_message_already_deleted() throws Exception {
        // given
        var request = OpenChatMessageEditFixture.createEditRequest();
        given(openChatMessageService.editMessage(any(), anyLong(), anyLong(), any()))
                .willThrow(new CustomException(ErrorCode.OPEN_CHAT_MESSAGE_ALREADY_DELETED));

        // when
        ResultActions result = mockMvc.perform(patch("/open-chat-rooms/{roomId}/messages/{messageId}", 5L, 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 반환 — AC-5 기존 내용과 동일한 내용으로 수정 시도")
    void should_return_400_when_content_is_unchanged() throws Exception {
        // given
        var request = OpenChatMessageEditFixture.createEditRequest();
        given(openChatMessageService.editMessage(any(), anyLong(), anyLong(), any()))
                .willThrow(new CustomException(ErrorCode.OPEN_CHAT_MESSAGE_CONTENT_UNCHANGED));

        // when
        ResultActions result = mockMvc.perform(patch("/open-chat-rooms/{roomId}/messages/{messageId}", 5L, 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }
}
