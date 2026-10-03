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
class OpenChatRecruitmentStatusControllerTest {

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
    @DisplayName("204 반환 — AC-01 방장이 OPEN → CLOSED 전환")
    void should_return_204_when_owner_closes_derived_room() throws Exception {
        // when
        ResultActions result = mockMvc.perform(
                patch("/open-chat-rooms/{roomId}/recruitment-status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"));

        // then
        result.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("204 반환 — AC-02 방장이 CLOSED → OPEN 재개")
    void should_return_204_when_owner_reopens_derived_room() throws Exception {
        // when
        ResultActions result = mockMvc.perform(
                patch("/open-chat-rooms/{roomId}/recruitment-status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"));

        // then
        result.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("204 반환 — AC-06 관리자가 임의의 DERIVED 방을 CLOSED로 전환")
    void should_return_204_when_admin_closes_derived_room() throws Exception {
        // when
        ResultActions result = mockMvc.perform(
                patch("/open-chat-rooms/{roomId}/recruitment-status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"));

        // then
        result.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("204 반환 — AC-07 관리자가 임의의 DERIVED 방을 OPEN으로 재개")
    void should_return_204_when_admin_reopens_derived_room() throws Exception {
        // when
        ResultActions result = mockMvc.perform(
                patch("/open-chat-rooms/{roomId}/recruitment-status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"));

        // then
        result.andExpect(status().isNoContent());
    }

    // ============================================================
    // Validation
    // ============================================================

    @Test
    @DisplayName("400 반환 — AC-13 status 필드 누락 (null)")
    void should_return_400_when_status_is_null() throws Exception {
        // when
        ResultActions result = mockMvc.perform(
                patch("/open-chat-rooms/{roomId}/recruitment-status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 반환 — AC-13 status 값이 OPEN/CLOSED 외 문자열")
    void should_return_400_when_status_is_invalid_enum_value() throws Exception {
        // when
        ResultActions result = mockMvc.perform(
                patch("/open-chat-rooms/{roomId}/recruitment-status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FOO\"}"));

        // then
        result.andExpect(status().isBadRequest());
    }

    // ============================================================
    // Business Rule — Controller 계층 검증 (service에서 예외 throw)
    // ============================================================

    @Test
    @DisplayName("409 반환 — BR-759-AC-03 이미 OPEN인 방을 OPEN으로 요청 → OPEN_CHAT_ROOM_ALREADY_OPEN")
    void should_return_409_when_already_open_and_request_open() throws Exception {
        // given
        willThrow(new CustomException(ErrorCode.OPEN_CHAT_ROOM_ALREADY_OPEN))
                .given(openChatRoomService).updateRecruitmentStatus(any(), anyLong(), any());

        // when
        ResultActions result = mockMvc.perform(
                patch("/open-chat-rooms/{roomId}/recruitment-status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"));

        // then
        result.andExpect(status().isConflict());
    }

    @Test
    @DisplayName("409 반환 — BR-759-AC-04 이미 CLOSED인 방을 CLOSED로 요청 → OPEN_CHAT_ROOM_ALREADY_CLOSED")
    void should_return_409_when_already_closed_and_request_closed() throws Exception {
        // given
        willThrow(new CustomException(ErrorCode.OPEN_CHAT_ROOM_ALREADY_CLOSED))
                .given(openChatRoomService).updateRecruitmentStatus(any(), anyLong(), any());

        // when
        ResultActions result = mockMvc.perform(
                patch("/open-chat-rooms/{roomId}/recruitment-status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"));

        // then
        result.andExpect(status().isConflict());
    }

    @Test
    @DisplayName("403 반환 — BR-759-AC-05 방장·관리자 아닌 참여자가 상태 변경 요청 → OPEN_CHAT_ROOM_FORBIDDEN")
    void should_return_403_when_non_owner_non_admin_requests_status_change() throws Exception {
        // given
        willThrow(new CustomException(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN))
                .given(openChatRoomService).updateRecruitmentStatus(any(), anyLong(), any());

        // when
        ResultActions result = mockMvc.perform(
                patch("/open-chat-rooms/{roomId}/recruitment-status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"));

        // then
        result.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("400 반환 — BR-759-AC-08 OPEN 타입 방에 상태 변경 요청 → OPEN_CHAT_ROOM_NOT_DERIVED")
    void should_return_400_when_room_is_not_derived_type() throws Exception {
        // given
        willThrow(new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_DERIVED))
                .given(openChatRoomService).updateRecruitmentStatus(any(), anyLong(), any());

        // when
        ResultActions result = mockMvc.perform(
                patch("/open-chat-rooms/{roomId}/recruitment-status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("404 반환 — BR-759-AC-09 존재하지 않는 roomId → OPEN_CHAT_ROOM_NOT_FOUND")
    void should_return_404_when_room_not_found() throws Exception {
        // given
        willThrow(new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND))
                .given(openChatRoomService).updateRecruitmentStatus(any(), anyLong(), any());

        // when
        ResultActions result = mockMvc.perform(
                patch("/open-chat-rooms/{roomId}/recruitment-status", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"));

        // then
        result.andExpect(status().isNotFound());
    }

    // ============================================================
    // Edge Case
    // ============================================================

    @Test
    @DisplayName("404 반환 — 요청자 계정이 DB에 없음 → USER_NOT_FOUND")
    void should_return_404_when_actor_user_not_found() throws Exception {
        // given
        willThrow(new CustomException(ErrorCode.USER_NOT_FOUND))
                .given(openChatRoomService).updateRecruitmentStatus(any(), anyLong(), any());

        // when
        ResultActions result = mockMvc.perform(
                patch("/open-chat-rooms/{roomId}/recruitment-status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"));

        // then
        result.andExpect(status().isNotFound());
    }
}
