package com.example.appcenter_project.domain.openChat.controller;

import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatMessageDto;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatMultiImageFixture;
import com.example.appcenter_project.domain.openChat.service.OpenChatMessageService;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.exception.ErrorCode;
import com.example.appcenter_project.global.exception.SlackErrorNotifier;
import com.example.appcenter_project.global.security.CustomUserDetails;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OpenChatMessageController.class)
@AutoConfigureMockMvc(addFilters = false)
class OpenChatMultiImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpenChatMessageService openChatMessageService;

    @MockBean
    private com.example.appcenter_project.common.image.service.ImageService imageService;

    @MockBean
    private com.example.appcenter_project.common.image.repository.ImageRepository imageRepository;

    @MockBean
    private SlackErrorNotifier slackErrorNotifier;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final Long ROOM_ID = 5L;
    private static final Long USER_ID = 42L;

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

    @SuppressWarnings("unchecked")
    private void stubSendImageMessage(List<ResponseOpenChatMessageDto> responseList) {
        doReturn(responseList).when(openChatMessageService).sendImageMessage(any(), eq(ROOM_ID), any(), any());
    }

    @Test
    @DisplayName("AC-1 이미지 3장 전송 성공 — 응답 리스트 크기 3")
    void should_return_list_of_size_3_when_3_images_sent() throws Exception {
        // given
        stubSendImageMessage(OpenChatMultiImageFixture.createMessageResponseList(ROOM_ID, USER_ID, 3));
        MockMultipartFile img1 = new MockMultipartFile("images", "img1.jpg", "image/jpeg", "c".getBytes());
        MockMultipartFile img2 = new MockMultipartFile("images", "img2.jpg", "image/jpeg", "c".getBytes());
        MockMultipartFile img3 = new MockMultipartFile("images", "img3.jpg", "image/jpeg", "c".getBytes());

        // when
        ResultActions result = mockMvc.perform(multipart("/open-chat-rooms/{roomId}/messages/image", ROOM_ID)
                .file(img1).file(img2).file(img3)
                .contentType(MediaType.MULTIPART_FORM_DATA));

        // then
        result.andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("AC-1 이미지 3장 전송 성공 — 201 CREATED 반환")
    void should_return_201_when_3_images_sent() throws Exception {
        // given
        stubSendImageMessage(OpenChatMultiImageFixture.createMessageResponseList(ROOM_ID, USER_ID, 3));
        MockMultipartFile img1 = new MockMultipartFile("images", "img1.jpg", "image/jpeg", "c".getBytes());
        MockMultipartFile img2 = new MockMultipartFile("images", "img2.jpg", "image/jpeg", "c".getBytes());
        MockMultipartFile img3 = new MockMultipartFile("images", "img3.jpg", "image/jpeg", "c".getBytes());

        // when
        ResultActions result = mockMvc.perform(multipart("/open-chat-rooms/{roomId}/messages/image", ROOM_ID)
                .file(img1).file(img2).file(img3)
                .contentType(MediaType.MULTIPART_FORM_DATA));

        // then
        result.andExpect(status().isCreated());
    }

    @Test
    @DisplayName("AC-2 이미지 1장 전송 성공 — 응답 리스트 크기 1")
    void should_return_list_of_size_1_when_1_image_sent() throws Exception {
        // given
        stubSendImageMessage(OpenChatMultiImageFixture.createMessageResponseList(ROOM_ID, USER_ID, 1));
        MockMultipartFile img = new MockMultipartFile("images", "img.jpg", "image/jpeg", "c".getBytes());

        // when
        ResultActions result = mockMvc.perform(multipart("/open-chat-rooms/{roomId}/messages/image", ROOM_ID)
                .file(img)
                .contentType(MediaType.MULTIPART_FORM_DATA));

        // then
        result.andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("AC-3 이미지 6장 전송 — 400 OPEN_CHAT_IMAGE_COUNT_EXCEEDED 반환")
    void should_return_400_when_6_images_sent() throws Exception {
        // given
        willThrow(new CustomException(ErrorCode.OPEN_CHAT_IMAGE_COUNT_EXCEEDED))
                .given(openChatMessageService).sendImageMessage(any(), eq(ROOM_ID), any(), any());
        MockMultipartFile img = new MockMultipartFile("images", "img.jpg", "image/jpeg", "c".getBytes());

        // when
        ResultActions result = mockMvc.perform(multipart("/open-chat-rooms/{roomId}/messages/image", ROOM_ID)
                .file(img).file(img).file(img).file(img).file(img).file(img)
                .contentType(MediaType.MULTIPART_FORM_DATA));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("AC-4 비참여자 이미지 전송 — 403 OPEN_CHAT_NOT_PARTICIPANT 반환")
    void should_return_403_when_non_participant_sends_image() throws Exception {
        // given
        willThrow(new CustomException(ErrorCode.OPEN_CHAT_NOT_PARTICIPANT))
                .given(openChatMessageService).sendImageMessage(any(), eq(ROOM_ID), any(), any());
        MockMultipartFile img = new MockMultipartFile("images", "img.jpg", "image/jpeg", "c".getBytes());

        // when
        ResultActions result = mockMvc.perform(multipart("/open-chat-rooms/{roomId}/messages/image", ROOM_ID)
                .file(img)
                .contentType(MediaType.MULTIPART_FORM_DATA));

        // then
        result.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("이미지 전송 — 404 채팅방 없음 반환")
    void should_return_404_when_room_not_found() throws Exception {
        // given
        willThrow(new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND))
                .given(openChatMessageService).sendImageMessage(any(), eq(ROOM_ID), any(), any());
        MockMultipartFile img = new MockMultipartFile("images", "img.jpg", "image/jpeg", "c".getBytes());

        // when
        ResultActions result = mockMvc.perform(multipart("/open-chat-rooms/{roomId}/messages/image", ROOM_ID)
                .file(img)
                .contentType(MediaType.MULTIPART_FORM_DATA));

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("이미지 전송 — 각 응답 원소의 type이 IMAGE")
    void should_return_each_element_with_type_IMAGE() throws Exception {
        // given
        stubSendImageMessage(OpenChatMultiImageFixture.createMessageResponseList(ROOM_ID, USER_ID, 2));
        MockMultipartFile img1 = new MockMultipartFile("images", "img1.jpg", "image/jpeg", "c".getBytes());
        MockMultipartFile img2 = new MockMultipartFile("images", "img2.jpg", "image/jpeg", "c".getBytes());

        // when
        ResultActions result = mockMvc.perform(multipart("/open-chat-rooms/{roomId}/messages/image", ROOM_ID)
                .file(img1).file(img2)
                .contentType(MediaType.MULTIPART_FORM_DATA));

        // then
        result.andExpect(jsonPath("$[0].type").value("IMAGE"))
              .andExpect(jsonPath("$[1].type").value("IMAGE"));
    }

    @Test
    @DisplayName("이미지 전송 — 각 응답 원소의 content가 빈 문자열")
    void should_return_each_element_with_empty_content() throws Exception {
        // given
        stubSendImageMessage(OpenChatMultiImageFixture.createMessageResponseList(ROOM_ID, USER_ID, 2));
        MockMultipartFile img1 = new MockMultipartFile("images", "img1.jpg", "image/jpeg", "c".getBytes());
        MockMultipartFile img2 = new MockMultipartFile("images", "img2.jpg", "image/jpeg", "c".getBytes());

        // when
        ResultActions result = mockMvc.perform(multipart("/open-chat-rooms/{roomId}/messages/image", ROOM_ID)
                .file(img1).file(img2)
                .contentType(MediaType.MULTIPART_FORM_DATA));

        // then
        result.andExpect(jsonPath("$[0].content").value(""))
              .andExpect(jsonPath("$[1].content").value(""));
    }

    @Test
    @DisplayName("이미지 전송 — 각 응답 원소의 imageUrls가 1건")
    void should_return_each_element_with_single_image_url() throws Exception {
        // given
        stubSendImageMessage(OpenChatMultiImageFixture.createMessageResponseList(ROOM_ID, USER_ID, 2));
        MockMultipartFile img1 = new MockMultipartFile("images", "img1.jpg", "image/jpeg", "c".getBytes());
        MockMultipartFile img2 = new MockMultipartFile("images", "img2.jpg", "image/jpeg", "c".getBytes());

        // when
        ResultActions result = mockMvc.perform(multipart("/open-chat-rooms/{roomId}/messages/image", ROOM_ID)
                .file(img1).file(img2)
                .contentType(MediaType.MULTIPART_FORM_DATA));

        // then
        result.andExpect(jsonPath("$[0].imageUrls.length()").value(1))
              .andExpect(jsonPath("$[1].imageUrls.length()").value(1));
    }

    @Test
    @DisplayName("이미지 전송 — 빈 이미지 목록 요청 시 400 반환")
    void should_return_400_when_images_empty() throws Exception {
        // given
        willThrow(new CustomException(ErrorCode.OPEN_CHAT_IMAGE_EMPTY))
                .given(openChatMessageService).sendImageMessage(any(), eq(ROOM_ID), any(), any());
        MockMultipartFile empty = new MockMultipartFile("images", "", "image/jpeg", new byte[0]);

        // when
        ResultActions result = mockMvc.perform(multipart("/open-chat-rooms/{roomId}/messages/image", ROOM_ID)
                .file(empty)
                .contentType(MediaType.MULTIPART_FORM_DATA));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이미지 전송 — 잘못된 파일 포맷 시 400 반환")
    void should_return_400_when_invalid_image_format() throws Exception {
        // given
        willThrow(new CustomException(ErrorCode.IMAGE_INVALID_FORMAT))
                .given(openChatMessageService).sendImageMessage(any(), eq(ROOM_ID), any(), any());
        MockMultipartFile heic = new MockMultipartFile("images", "test.heic", "image/heic", "c".getBytes());

        // when
        ResultActions result = mockMvc.perform(multipart("/open-chat-rooms/{roomId}/messages/image", ROOM_ID)
                .file(heic)
                .contentType(MediaType.MULTIPART_FORM_DATA));

        // then
        result.andExpect(status().isBadRequest());
    }
}
