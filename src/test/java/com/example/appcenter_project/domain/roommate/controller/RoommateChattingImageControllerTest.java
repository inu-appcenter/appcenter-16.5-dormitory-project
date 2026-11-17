package com.example.appcenter_project.domain.roommate.controller;

import com.example.appcenter_project.domain.roommate.dto.response.ResponseRoommateChatDto;
import com.example.appcenter_project.domain.roommate.service.RoommateChattingChatService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BR-791
@WebMvcTest(RoommateChattingChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoommateChattingImageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    RoommateChattingChatService roommateChattingChatService;

    @MockBean
    SlackErrorNotifier slackErrorNotifier;

    @MockBean
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private final MockMultipartFile img1 = new MockMultipartFile("images", "img1.jpg", "image/jpeg", "c".getBytes());
    private final MockMultipartFile img2 = new MockMultipartFile("images", "img2.jpg", "image/jpeg", "c".getBytes());

    @BeforeEach
    void setUpAuthentication() {
        CustomUserDetails principal = mock(CustomUserDetails.class);
        given(principal.getId()).willReturn(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("BR-791 AC-1 이미지 2장 전송 — 201 CREATED, 응답 2건")
    void should_return_201_with_list_when_images_sent() throws Exception {
        given(roommateChattingChatService.sendImageChats(eq(1L), eq(7L), anyList(), any()))
                .willReturn(List.of(
                        ResponseRoommateChatDto.builder().roommateChatId(1L).imageUrls(List.of("https://host/1.jpg")).build(),
                        ResponseRoommateChatDto.builder().roommateChatId(2L).imageUrls(List.of("https://host/2.jpg")).build()));

        mockMvc.perform(multipart("/roommate/chat/{roomId}/images", 7L).file(img1).file(img2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].imageUrls[0]").value("https://host/1.jpg"));
    }

    @Test
    @DisplayName("BR-791 AC-4 이미지 개수 초과 — 400")
    void should_return_400_when_image_count_exceeded() throws Exception {
        given(roommateChattingChatService.sendImageChats(anyLong(), anyLong(), anyList(), any()))
                .willThrow(new CustomException(ErrorCode.ROOMMATE_CHAT_IMAGE_COUNT_EXCEEDED));

        mockMvc.perform(multipart("/roommate/chat/{roomId}/images", 7L).file(img1).file(img2))
                .andExpect(status().isBadRequest());
    }
}