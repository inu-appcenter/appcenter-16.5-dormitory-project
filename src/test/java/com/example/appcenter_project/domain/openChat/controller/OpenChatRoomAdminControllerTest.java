package com.example.appcenter_project.domain.openChat.controller;

import com.example.appcenter_project.domain.openChat.dto.request.RequestCreateDormOfficialRoomDto;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatDormOfficialRoomFixture;
import com.example.appcenter_project.domain.openChat.service.OpenChatDormOfficialRoomService;
import com.example.appcenter_project.domain.openChat.service.OpenChatMessageService;
import com.example.appcenter_project.domain.user.enums.DormType;
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

@WebMvcTest(OpenChatRoomAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class OpenChatRoomAdminControllerTest {

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

    @Test
    @DisplayName("201 반환 — AC-1 정상 요청 시 공식 방 생성 성공")
    void should_return_201_when_valid_request() throws Exception {
        // given
        RequestCreateDormOfficialRoomDto request = OpenChatDormOfficialRoomFixture.createRequest();
        given(openChatDormOfficialRoomService.createDormOfficialRoom(any(), any())).willReturn(42L);

        // when
        ResultActions result = mockMvc.perform(post("/admin/open-chat-rooms/dorm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isCreated());
    }

    @Test
    @DisplayName("roomId 반환 — AC-1 정상 요청 시 생성된 방 ID 응답")
    void should_return_roomId_when_valid_request() throws Exception {
        // given
        RequestCreateDormOfficialRoomDto request = OpenChatDormOfficialRoomFixture.createRequest();
        given(openChatDormOfficialRoomService.createDormOfficialRoom(any(), any())).willReturn(42L);

        // when
        ResultActions result = mockMvc.perform(post("/admin/open-chat-rooms/dorm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(jsonPath("$.roomId").value(42L));
    }

    @Test
    @DisplayName("400 반환 — Validation: name null 시 DTO 검증 실패")
    void should_return_400_when_name_is_null() throws Exception {
        // given
        RequestCreateDormOfficialRoomDto request = OpenChatDormOfficialRoomFixture.createRequestWithNullName();

        // when
        ResultActions result = mockMvc.perform(post("/admin/open-chat-rooms/dorm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 반환 — Validation: name 공백 시 DTO 검증 실패")
    void should_return_400_when_name_is_blank() throws Exception {
        // given
        RequestCreateDormOfficialRoomDto request = OpenChatDormOfficialRoomFixture.createRequestWithBlankName();

        // when
        ResultActions result = mockMvc.perform(post("/admin/open-chat-rooms/dorm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 반환 — Validation: name 30자 초과 시 DTO 검증 실패")
    void should_return_400_when_name_exceeds_30_chars() throws Exception {
        // given
        RequestCreateDormOfficialRoomDto request = OpenChatDormOfficialRoomFixture.createRequestWithLongName();

        // when
        ResultActions result = mockMvc.perform(post("/admin/open-chat-rooms/dorm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 반환 — Validation: description 100자 초과 시 DTO 검증 실패")
    void should_return_400_when_description_exceeds_100_chars() throws Exception {
        // given
        RequestCreateDormOfficialRoomDto request = OpenChatDormOfficialRoomFixture.createRequestWithLongDescription();

        // when
        ResultActions result = mockMvc.perform(post("/admin/open-chat-rooms/dorm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 반환 — Validation: dormType null 시 DTO 검증 실패")
    void should_return_400_when_dorm_type_is_null() throws Exception {
        // given
        RequestCreateDormOfficialRoomDto request = OpenChatDormOfficialRoomFixture.createRequestWithNullDormType();

        // when
        ResultActions result = mockMvc.perform(post("/admin/open-chat-rooms/dorm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 반환 — AC-4 dormType이 NONE 시 INVALID_DORM_TYPE")
    void should_return_400_when_dorm_type_is_none() throws Exception {
        // given
        RequestCreateDormOfficialRoomDto request = OpenChatDormOfficialRoomFixture.createRequestWithNoneDormType();
        given(openChatDormOfficialRoomService.createDormOfficialRoom(any(), any()))
                .willThrow(new CustomException(ErrorCode.INVALID_DORM_TYPE));

        // when
        ResultActions result = mockMvc.perform(post("/admin/open-chat-rooms/dorm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("409 반환 — AC-2 동일 기숙사 공식 방 중복 생성 시 OPEN_CHAT_DORM_OFFICIAL_ROOM_ALREADY_EXISTS")
    void should_return_409_when_dorm_official_room_already_exists() throws Exception {
        // given
        RequestCreateDormOfficialRoomDto request = OpenChatDormOfficialRoomFixture.createRequest();
        given(openChatDormOfficialRoomService.createDormOfficialRoom(any(), any()))
                .willThrow(new CustomException(ErrorCode.OPEN_CHAT_DORM_OFFICIAL_ROOM_ALREADY_EXISTS));

        // when
        ResultActions result = mockMvc.perform(post("/admin/open-chat-rooms/dorm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isConflict());
    }
}
