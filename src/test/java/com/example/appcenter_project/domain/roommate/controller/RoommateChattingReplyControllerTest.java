package com.example.appcenter_project.domain.roommate.controller;

import com.example.appcenter_project.domain.roommate.service.RoommateChattingChatService;
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

@WebMvcTest(RoommateChattingChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoommateChattingReplyControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    RoommateChattingChatService roommateChattingChatService;

    @MockBean
    SlackErrorNotifier slackErrorNotifier;

    @MockBean
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("400 반환 — 답장 불가 메시지 유형으로 전송 시도 (룸메톡방)")
    void should_return_400_when_roommate_chat_reply_target_is_system_message() throws Exception {
        // given
        String requestBody = "{\"roommateChattingRoomId\":7,\"content\":\"시스템 메시지 답장\",\"replyToMessageId\":55}";
        given(roommateChattingChatService.sendChat(any(), anyLong()))
                .willThrow(new CustomException(ErrorCode.ROOMMATE_CHAT_REPLY_NOT_ALLOWED_FOR_TYPE));

        // when
        ResultActions result = mockMvc.perform(post("/roommate/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 반환 — 다른 방의 메시지에 답장 (룸메톡방)")
    void should_return_400_when_roommate_chat_reply_target_is_in_different_room() throws Exception {
        // given
        String requestBody = "{\"roommateChattingRoomId\":7,\"content\":\"다른 방 메시지 답장\",\"replyToMessageId\":77}";
        given(roommateChattingChatService.sendChat(any(), anyLong()))
                .willThrow(new CustomException(ErrorCode.ROOMMATE_CHAT_REPLY_TARGET_NOT_IN_SAME_ROOM));

        // when
        ResultActions result = mockMvc.perform(post("/roommate/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 반환 — 중첩 답장 시도 (룸메톡방)")
    void should_return_400_when_roommate_nested_reply_attempted() throws Exception {
        // given
        String requestBody = "{\"roommateChattingRoomId\":7,\"content\":\"중첩 답장\",\"replyToMessageId\":88}";
        given(roommateChattingChatService.sendChat(any(), anyLong()))
                .willThrow(new CustomException(ErrorCode.ROOMMATE_CHAT_NESTED_REPLY_NOT_ALLOWED));

        // when
        ResultActions result = mockMvc.perform(post("/roommate/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("204 반환 — BR-769-15 발신자 본인 룸메이트 채팅 soft-delete 성공")
    void should_return_204_when_owner_deletes_roommate_chat() throws Exception {
        // given
        Long chatId = 200L;
        willDoNothing().given(roommateChattingChatService).deleteChat(eq(chatId), anyLong());

        // when
        ResultActions result = mockMvc.perform(
                delete("/roommate/chat/messages/{chatId}", chatId));

        // then
        result.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("403 반환 — BR-769-16 타인이 룸메이트 채팅 삭제 시도")
    void should_return_403_when_non_owner_deletes_roommate_chat() throws Exception {
        // given
        Long chatId = 200L;
        willThrow(new CustomException(ErrorCode.ROOMMATE_CHAT_MESSAGE_NOT_OWNED_BY_USER))
                .given(roommateChattingChatService).deleteChat(eq(chatId), anyLong());

        // when
        ResultActions result = mockMvc.perform(
                delete("/roommate/chat/messages/{chatId}", chatId));

        // then
        result.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("400 반환 — 이미 삭제된 룸메이트 채팅 재삭제 시도")
    void should_return_400_when_deleting_already_deleted_roommate_chat() throws Exception {
        // given
        Long alreadyDeletedChatId = 200L;
        willThrow(new CustomException(ErrorCode.ROOMMATE_CHAT_MESSAGE_ALREADY_DELETED))
                .given(roommateChattingChatService).deleteChat(eq(alreadyDeletedChatId), anyLong());

        // when
        ResultActions result = mockMvc.perform(
                delete("/roommate/chat/messages/{chatId}", alreadyDeletedChatId));

        // then
        result.andExpect(status().isBadRequest());
    }
}
