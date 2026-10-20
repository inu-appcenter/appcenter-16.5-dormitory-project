package com.example.appcenter_project.domain.openChat.controller;

import com.example.appcenter_project.domain.openChat.service.OpenChatRoomService;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.exception.ErrorCode;
import com.example.appcenter_project.global.exception.SlackErrorNotifier;
import com.example.appcenter_project.global.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OpenChatRoomController.class)
@AutoConfigureMockMvc(addFilters = false)
class OpenChatJoinRoomClosedBlockingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    OpenChatRoomService openChatRoomService;

    @MockBean
    SlackErrorNotifier slackErrorNotifier;

    @MockBean
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void setUp() {
        CustomUserDetails mockUser = new CustomUserDetails();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ============================================================
    // Happy Path — Controller
    // ============================================================

    @Test
    @DisplayName("200 반환 — AC-04 BR-761 OPEN 상태 DERIVED 방에 비참여자 joinRoom 성공")
    void should_return_200_when_joining_open_derived_room() throws Exception {
        // given
        given(openChatRoomService.joinRoom(any(), anyLong(), any()))
                .willReturn(null);

        // when
        ResultActions result = mockMvc.perform(
                post("/open-chat-rooms/{roomId}/participants/me", 1L)
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk());
    }

    // ============================================================
    // Business Rule — Controller 계층 검증 (service에서 예외 throw)
    // ============================================================

    @Test
    @DisplayName("409 반환 — AC-01 BR-761 CLOSED 상태 방에 비참여자 joinRoom → OPEN_CHAT_ROOM_CLOSED_FOR_JOIN")
    void should_return_409_when_joining_closed_derived_room() throws Exception {
        // given
        willThrow(new CustomException(ErrorCode.OPEN_CHAT_ROOM_CLOSED_FOR_JOIN))
                .given(openChatRoomService).joinRoom(any(), anyLong(), any());

        // when
        ResultActions result = mockMvc.perform(
                post("/open-chat-rooms/{roomId}/participants/me", 1L)
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isConflict());
    }

    @Test
    @DisplayName("409 응답 바디 name 필드 — AC-10 BR-761 CLOSED 방 입장 실패 시 name=OPEN_CHAT_ROOM_CLOSED_FOR_JOIN")
    void should_return_error_name_OPEN_CHAT_ROOM_CLOSED_FOR_JOIN_in_body_when_joining_closed_room() throws Exception {
        // given
        willThrow(new CustomException(ErrorCode.OPEN_CHAT_ROOM_CLOSED_FOR_JOIN))
                .given(openChatRoomService).joinRoom(any(), anyLong(), any());

        // when
        ResultActions result = mockMvc.perform(
                post("/open-chat-rooms/{roomId}/participants/me", 1L)
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(jsonPath("$.name").value("OPEN_CHAT_ROOM_CLOSED_FOR_JOIN"));
    }

    @Test
    @DisplayName("404 반환 — 존재하지 않는 roomId로 joinRoom → OPEN_CHAT_ROOM_NOT_FOUND")
    void should_return_404_when_room_not_found_on_join() throws Exception {
        // given
        willThrow(new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND))
                .given(openChatRoomService).joinRoom(any(), anyLong(), any());

        // when
        ResultActions result = mockMvc.perform(
                post("/open-chat-rooms/{roomId}/participants/me", 999999L)
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("400 반환 — OPEN/PERSONAL/공식 방에 joinRoom → OPEN_CHAT_ROOM_NOT_DERIVED")
    void should_return_400_when_room_is_not_derived_type_on_join() throws Exception {
        // given
        willThrow(new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_DERIVED))
                .given(openChatRoomService).joinRoom(any(), anyLong(), any());

        // when
        ResultActions result = mockMvc.perform(
                post("/open-chat-rooms/{roomId}/participants/me", 1L)
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 반환 — 정원 초과 방에 joinRoom → OPEN_CHAT_ROOM_FULL")
    void should_return_400_when_room_is_full_on_join() throws Exception {
        // given
        willThrow(new CustomException(ErrorCode.OPEN_CHAT_ROOM_FULL))
                .given(openChatRoomService).joinRoom(any(), anyLong(), any());

        // when
        ResultActions result = mockMvc.perform(
                post("/open-chat-rooms/{roomId}/participants/me", 1L)
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("403 반환 — 비밀번호 불일치로 joinRoom → OPEN_CHAT_ROOM_FORBIDDEN")
    void should_return_403_when_password_mismatch_on_join() throws Exception {
        // given
        willThrow(new CustomException(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN))
                .given(openChatRoomService).joinRoom(any(), anyLong(), any());

        // when
        ResultActions result = mockMvc.perform(
                post("/open-chat-rooms/{roomId}/participants/me", 1L)
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isForbidden());
    }
}
