package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.common.image.enums.ImageType;
import com.example.appcenter_project.common.image.repository.ImageRepository;
import com.example.appcenter_project.common.image.service.ImageService;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatMessageDto;
import com.example.appcenter_project.domain.openChat.entity.OpenChatMessage;
import com.example.appcenter_project.domain.openChat.entity.OpenChatParticipant;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatImageMessageFixture;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatMultiImageFixture;
import com.example.appcenter_project.domain.openChat.repository.OpenChatMessageRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatParticipantRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomRepository;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.enums.College;
import com.example.appcenter_project.domain.user.enums.DormType;
import com.example.appcenter_project.domain.user.enums.Role;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.config.OpenChatSessionRegistry;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Iterator;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class OpenChatMultiImageServiceTest {

    @Mock
    private OpenChatRoomRepository openChatRoomRepository;
    @Mock
    private OpenChatParticipantRepository openChatParticipantRepository;
    @Mock
    private OpenChatMessageRepository openChatMessageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private ImageService imageService;
    @Mock
    private ImageRepository imageRepository;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private OpenChatSessionRegistry sessionRegistry;

    @InjectMocks
    private OpenChatMessageService openChatMessageService;

    private static final Long USER_ID = 42L;
    private static final Long ROOM_ID = 5L;

    @Test
    @DisplayName("AC-1 이미지 3장 전송 성공 — 반환 리스트 크기 3")
    void should_return_list_of_size_3_when_3_images_sent() {
        // given
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);
        User sender = User.createTestUser("20240001", "pw", "홍길동", DormType.DORM_1, College.ENGINEERING, Role.ROLE_USER);
        OpenChatMessage message = OpenChatImageMessageFixture.createImageMessage(ROOM_ID, USER_ID);
        List<MultipartFile> images = OpenChatMultiImageFixture.createImageList(3);

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(sender));
        given(openChatMessageRepository.save(any())).willReturn(message);
        given(imageService.findStaticImageUrls(eq(ImageType.OPEN_CHAT_MESSAGE), any(), eq(httpServletRequest)))
                .willReturn(List.of("https://host/img.jpg"));
        given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(3L);
        given(openChatParticipantRepository.countReadByRoomIdAndMessageId(eq(ROOM_ID), any())).willReturn(1L);

        // when
        var result = openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        // then
        assertThat(result).isInstanceOf(List.class);
        assertThat((List<?>) result).hasSize(3);
    }

    @Test
    @DisplayName("AC-1 이미지 3장 전송 성공 — 메시지 3개 저장")
    void should_save_3_messages_when_3_images_sent() {
        // given
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);
        User sender = User.createTestUser("20240001", "pw", "홍길동", DormType.DORM_1, College.ENGINEERING, Role.ROLE_USER);
        OpenChatMessage message = OpenChatImageMessageFixture.createImageMessage(ROOM_ID, USER_ID);
        List<MultipartFile> images = OpenChatMultiImageFixture.createImageList(3);

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(sender));
        given(openChatMessageRepository.save(any())).willReturn(message);
        given(imageService.findStaticImageUrls(eq(ImageType.OPEN_CHAT_MESSAGE), any(), eq(httpServletRequest)))
                .willReturn(List.of("https://host/img.jpg"));
        given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(3L);
        given(openChatParticipantRepository.countReadByRoomIdAndMessageId(eq(ROOM_ID), any())).willReturn(1L);

        // when
        openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        // then
        then(openChatMessageRepository).should(times(3)).save(any());
    }

    @Test
    @DisplayName("AC-1 이미지 3장 전송 성공 — imageService.saveImages 3회 호출")
    void should_call_saveImages_3_times_when_3_images_sent() {
        // given
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);
        User sender = User.createTestUser("20240001", "pw", "홍길동", DormType.DORM_1, College.ENGINEERING, Role.ROLE_USER);
        OpenChatMessage message = OpenChatImageMessageFixture.createImageMessage(ROOM_ID, USER_ID);
        List<MultipartFile> images = OpenChatMultiImageFixture.createImageList(3);

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(sender));
        given(openChatMessageRepository.save(any())).willReturn(message);
        given(imageService.findStaticImageUrls(eq(ImageType.OPEN_CHAT_MESSAGE), any(), eq(httpServletRequest)))
                .willReturn(List.of("https://host/img.jpg"));
        given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(3L);
        given(openChatParticipantRepository.countReadByRoomIdAndMessageId(eq(ROOM_ID), any())).willReturn(1L);

        // when
        openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        // then
        then(imageService).should(times(3)).saveImages(eq(ImageType.OPEN_CHAT_MESSAGE), any(), any());
    }

    @Test
    @DisplayName("AC-2 이미지 1장 전송 성공 — 반환 리스트 크기 1")
    void should_return_list_of_size_1_when_1_image_sent() {
        // given
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);
        User sender = User.createTestUser("20240001", "pw", "홍길동", DormType.DORM_1, College.ENGINEERING, Role.ROLE_USER);
        OpenChatMessage message = OpenChatImageMessageFixture.createImageMessage(ROOM_ID, USER_ID);
        List<MultipartFile> images = OpenChatMultiImageFixture.createImageList(1);

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(sender));
        given(openChatMessageRepository.save(any())).willReturn(message);
        given(imageService.findStaticImageUrls(eq(ImageType.OPEN_CHAT_MESSAGE), any(), eq(httpServletRequest)))
                .willReturn(List.of("https://host/img.jpg"));
        given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(2L);
        given(openChatParticipantRepository.countReadByRoomIdAndMessageId(eq(ROOM_ID), any())).willReturn(1L);

        // when
        var result = openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        // then
        assertThat(result).isInstanceOf(List.class);
        assertThat((List<?>) result).hasSize(1);
    }

    @Test
    @DisplayName("AC-3 이미지 6장 전송 — CustomException OPEN_CHAT_IMAGE_COUNT_EXCEEDED 발생")
    void should_throw_when_AC_3_image_count_exceeds_5() {
        // given
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));

        List<MultipartFile> images = OpenChatMultiImageFixture.createImageList(6);

        // when
        ThrowingCallable action = () -> openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_IMAGE_COUNT_EXCEEDED);
    }

    @Test
    @DisplayName("AC-3 이미지 6장 전송 — 메시지 저장 호출 없음")
    void should_not_save_message_when_image_count_exceeds_5() {
        // given
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));

        List<MultipartFile> images = OpenChatMultiImageFixture.createImageList(6);

        // when
        ThrowingCallable action = () -> openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        // then
        assertThatThrownBy(action).isInstanceOf(CustomException.class);
        then(openChatMessageRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("AC-4 비참여자 이미지 전송 — CustomException OPEN_CHAT_NOT_PARTICIPANT 발생")
    void should_throw_when_AC_4_non_participant_sends_image() {
        // given
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.empty());

        List<MultipartFile> images = OpenChatMultiImageFixture.createImageList(1);

        // when
        ThrowingCallable action = () -> openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_NOT_PARTICIPANT);
    }

    @Test
    @DisplayName("AC-5 이미지 3장 전송 — WebSocket 브로드캐스트 3회 발생")
    void should_broadcast_3_times_when_AC_5_3_images_sent() {
        // given
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);
        User sender = User.createTestUser("20240001", "pw", "홍길동", DormType.DORM_1, College.ENGINEERING, Role.ROLE_USER);
        OpenChatMessage message = OpenChatImageMessageFixture.createImageMessage(ROOM_ID, USER_ID);
        List<MultipartFile> images = OpenChatMultiImageFixture.createImageList(3);

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(sender));
        given(openChatMessageRepository.save(any())).willReturn(message);
        given(imageService.findStaticImageUrls(eq(ImageType.OPEN_CHAT_MESSAGE), any(), eq(httpServletRequest)))
                .willReturn(List.of("https://host/img.jpg"));
        given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(3L);
        given(openChatParticipantRepository.countReadByRoomIdAndMessageId(eq(ROOM_ID), any())).willReturn(1L);

        // when
        openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        // then
        then(messagingTemplate).should(times(3)).convertAndSend(eq("/sub/openchat/" + ROOM_ID), any(ResponseOpenChatMessageDto.class));
    }

    @Test
    @DisplayName("AC-5 이미지 3장 전송 — 마지막 메시지 기준 lastMessage=[이미지] 갱신")
    void should_update_lastMessage_to_image_placeholder_after_AC_5_3_images_sent() {
        // given
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);
        User sender = User.createTestUser("20240001", "pw", "홍길동", DormType.DORM_1, College.ENGINEERING, Role.ROLE_USER);
        Iterator<Long> messageIds = List.of(1L, 2L, 3L).iterator();
        List<MultipartFile> images = OpenChatMultiImageFixture.createImageList(3);

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(sender));
        given(openChatMessageRepository.save(any())).willAnswer(inv -> {
            OpenChatMessage saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", messageIds.next());
            return saved;
        });
        given(imageService.findStaticImageUrls(eq(ImageType.OPEN_CHAT_MESSAGE), any(), eq(httpServletRequest)))
                .willReturn(List.of("https://host/img.jpg"));
        given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(3L);
        given(openChatParticipantRepository.countReadByRoomIdAndMessageId(eq(ROOM_ID), any())).willReturn(1L);

        // when
        openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        // then
        assertThat(room.getLastMessageId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("이미지 전송 — 채팅방 없음 시 CustomException OPEN_CHAT_ROOM_NOT_FOUND 발생")
    void should_throw_when_room_not_found_for_multi_image() {
        // given
        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

        List<MultipartFile> images = OpenChatMultiImageFixture.createImageList(1);

        // when
        ThrowingCallable action = () -> openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND);
    }
}
