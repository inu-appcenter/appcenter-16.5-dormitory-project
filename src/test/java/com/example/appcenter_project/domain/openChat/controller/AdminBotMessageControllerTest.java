package com.example.appcenter_project.domain.openChat.controller;

import com.example.appcenter_project.domain.openChat.dto.response.ResponseAdminChatRoomDto;
import com.example.appcenter_project.domain.openChat.fixture.AdminBotMessageFixture;
import com.example.appcenter_project.domain.openChat.service.OpenChatDormOfficialRoomService;
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

import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OpenChatRoomAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminBotMessageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    OpenChatDormOfficialRoomService openChatDormOfficialRoomService;

    @MockBean
    OpenChatMessageService openChatMessageService;

    @MockBean
    SlackErrorNotifier slackErrorNotifier;

    @MockBean
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    // =====================================================================
    // GET /admin/open-chat-rooms — 오픈채팅방 목록 조회
    // =====================================================================

    @Test
    @DisplayName("200 반환 — AC-1 오픈채팅방 목록 조회 성공")
    void should_return_200_when_get_chat_rooms() throws Exception {
        // given
        List<ResponseAdminChatRoomDto> rooms = List.of(
                ResponseAdminChatRoomDto.of(1L, "1기숙사 자유채팅"),
                ResponseAdminChatRoomDto.of(2L, "기숙사 공지방"),
                ResponseAdminChatRoomDto.of(3L, "자유게시판 채팅")
        );
        given(openChatMessageService.getAdminChatRooms()).willReturn(rooms);

        // when
        ResultActions result = mockMvc.perform(get("/admin/open-chat-rooms")
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk());
    }

    @Test
    @DisplayName("3개 반환 — AC-1 PERSONAL 제외한 3개 채팅방 응답")
    void should_return_3_rooms_when_get_chat_rooms() throws Exception {
        // given
        List<ResponseAdminChatRoomDto> rooms = List.of(
                ResponseAdminChatRoomDto.of(1L, "1기숙사 자유채팅"),
                ResponseAdminChatRoomDto.of(2L, "기숙사 공지방"),
                ResponseAdminChatRoomDto.of(3L, "자유게시판 채팅")
        );
        given(openChatMessageService.getAdminChatRooms()).willReturn(rooms);

        // when
        ResultActions result = mockMvc.perform(get("/admin/open-chat-rooms")
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("roomId 포함 — AC-1 응답 항목에 roomId 존재")
    void should_include_roomId_in_response() throws Exception {
        // given
        List<ResponseAdminChatRoomDto> rooms = List.of(ResponseAdminChatRoomDto.of(1L, "1기숙사 자유채팅"));
        given(openChatMessageService.getAdminChatRooms()).willReturn(rooms);

        // when
        ResultActions result = mockMvc.perform(get("/admin/open-chat-rooms")
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(jsonPath("$[0].roomId").value(1L));
    }

    @Test
    @DisplayName("roomName 포함 — AC-1 응답 항목에 roomName 존재")
    void should_include_roomName_in_response() throws Exception {
        // given
        List<ResponseAdminChatRoomDto> rooms = List.of(ResponseAdminChatRoomDto.of(1L, "1기숙사 자유채팅"));
        given(openChatMessageService.getAdminChatRooms()).willReturn(rooms);

        // when
        ResultActions result = mockMvc.perform(get("/admin/open-chat-rooms")
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(jsonPath("$[0].roomName").value("1기숙사 자유채팅"));
    }

    // =====================================================================
    // POST /admin/open-chat-rooms/{roomId}/bot — 챗봇 메시지 전송
    // =====================================================================

    @Test
    @DisplayName("201 반환 — AC-2 정상 챗봇 메시지 전송 성공")
    void should_return_201_when_send_bot_message() throws Exception {
        // given
        Map<String, String> request = Map.of("content", AdminBotMessageFixture.BOT_CONTENT);
        willDoNothing().given(openChatMessageService).sendBotMessage(any(), any(), any());

        // when
        ResultActions result = mockMvc.perform(post("/admin/open-chat-rooms/" + AdminBotMessageFixture.OPEN_ROOM_ID + "/bot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isCreated());
    }

    @Test
    @DisplayName("body 없음 — AC-2 성공 시 응답 body 없음")
    void should_return_no_body_when_send_bot_message_succeeds() throws Exception {
        // given
        Map<String, String> request = Map.of("content", AdminBotMessageFixture.BOT_CONTENT);
        willDoNothing().given(openChatMessageService).sendBotMessage(any(), any(), any());

        // when
        ResultActions result = mockMvc.perform(post("/admin/open-chat-rooms/" + AdminBotMessageFixture.OPEN_ROOM_ID + "/bot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(content().string(""));
    }

    @Test
    @DisplayName("400 반환 — Validation: content 공백 시 DTO 검증 실패")
    void should_return_400_when_content_is_blank() throws Exception {
        // given
        Map<String, String> request = Map.of("content", "   ");

        // when
        ResultActions result = mockMvc.perform(post("/admin/open-chat-rooms/" + AdminBotMessageFixture.OPEN_ROOM_ID + "/bot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 반환 — Validation: content 누락 시 DTO 검증 실패")
    void should_return_400_when_content_is_missing() throws Exception {
        // given
        Map<String, String> request = Map.of();

        // when
        ResultActions result = mockMvc.perform(post("/admin/open-chat-rooms/" + AdminBotMessageFixture.OPEN_ROOM_ID + "/bot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 반환 — Validation: content 500자 초과 시 DTO 검증 실패")
    void should_return_400_when_content_exceeds_500_chars() throws Exception {
        // given
        Map<String, String> request = Map.of("content", "a".repeat(501));

        // when
        ResultActions result = mockMvc.perform(post("/admin/open-chat-rooms/" + AdminBotMessageFixture.OPEN_ROOM_ID + "/bot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 반환 — AC-4 PERSONAL 방 전송 시 OPEN_CHAT_BOT_TARGET_PERSONAL")
    void should_return_400_when_room_is_personal() throws Exception {
        // given
        Map<String, String> request = Map.of("content", AdminBotMessageFixture.BOT_CONTENT);
        willThrow(new CustomException(ErrorCode.OPEN_CHAT_BOT_TARGET_PERSONAL))
                .given(openChatMessageService).sendBotMessage(any(), any(), any());

        // when
        ResultActions result = mockMvc.perform(post("/admin/open-chat-rooms/" + AdminBotMessageFixture.PERSONAL_ROOM_ID + "/bot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("404 반환 — AC-5 존재하지 않는 roomId 시 OPEN_CHAT_ROOM_NOT_FOUND")
    void should_return_404_when_room_not_found() throws Exception {
        // given
        Map<String, String> request = Map.of("content", AdminBotMessageFixture.BOT_CONTENT);
        willThrow(new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND))
                .given(openChatMessageService).sendBotMessage(any(), any(), any());

        // when
        ResultActions result = mockMvc.perform(post("/admin/open-chat-rooms/999/bot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isNotFound());
    }
}
