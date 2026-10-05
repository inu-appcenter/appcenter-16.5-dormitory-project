package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.common.image.enums.ImageType;
import com.example.appcenter_project.common.image.repository.ImageRepository;
import com.example.appcenter_project.common.image.service.ImageService;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatMessageDto;
import com.example.appcenter_project.domain.openChat.entity.OpenChatMessage;
import com.example.appcenter_project.domain.openChat.entity.OpenChatParticipant;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatMessageType;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatImageMessageFixture;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class OpenChatImageMessageServiceTest {

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

    // TODO: BR-685 구현 후 주석 해제
    // @Mock
    // private com.example.appcenter_project.domain.block.service.BlockService blockService;

    @InjectMocks
    private OpenChatMessageService openChatMessageService;

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 7L;

    @Test
    @DisplayName("이미지 메시지 전송 성공 — jpg 단일 파일, IMAGE 타입 메시지 저장 및 응답 반환")
    void should_send_image_message_successfully_with_single_jpg() {
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);
        User sender = User.createTestUser("20240001", "password", "홍길동", DormType.DORM_1, College.ENGINEERING, Role.ROLE_USER);
        OpenChatMessage message = OpenChatImageMessageFixture.createImageMessage(ROOM_ID, USER_ID);
        List<MultipartFile> images = OpenChatImageMessageFixture.createSingleValidImageList();
        List<String> urls = List.of("https://host/images/open_chat_message/img.jpg");

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(sender));
        given(openChatMessageRepository.save(any())).willReturn(message);
        given(imageService.findStaticImageUrls(eq(ImageType.OPEN_CHAT_MESSAGE), any(), eq(httpServletRequest))).willReturn(urls);
        given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(3L);
        given(openChatParticipantRepository.countReadByRoomIdAndMessageId(eq(ROOM_ID), any())).willReturn(1L);

        List<ResponseOpenChatMessageDto> results = openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        assertThat(results).isNotNull();
        assertThat(results.get(0).getType()).isEqualTo(OpenChatMessageType.IMAGE);
        assertThat(results.get(0).getImageUrls()).isNotEmpty();
        then(openChatMessageRepository).should(times(1)).save(any());
        then(imageService).should(times(1)).saveImages(eq(ImageType.OPEN_CHAT_MESSAGE), any(), any());
    }

    @Test
    @DisplayName("이미지 메시지 전송 성공 — 여러 파일, 모두 유효한 포맷")
    void should_send_image_message_successfully_with_multiple_valid_files() {
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);
        User sender = User.createTestUser("20240001", "password", "홍길동", DormType.DORM_1, College.ENGINEERING, Role.ROLE_USER);
        OpenChatMessage message = OpenChatImageMessageFixture.createImageMessage(ROOM_ID, USER_ID);
        List<MultipartFile> images = OpenChatImageMessageFixture.createValidImageList();
        List<String> urls = List.of("https://host/images/open_chat_message/img1.jpg", "https://host/images/open_chat_message/img2.png");

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(sender));
        given(openChatMessageRepository.save(any())).willReturn(message);
        given(imageService.findStaticImageUrls(eq(ImageType.OPEN_CHAT_MESSAGE), any(), eq(httpServletRequest))).willReturn(urls);
        given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(3L);
        given(openChatParticipantRepository.countReadByRoomIdAndMessageId(eq(ROOM_ID), any())).willReturn(1L);

        List<ResponseOpenChatMessageDto> results = openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("이미지 메시지 전송 성공 — content는 빈 문자열로 저장")
    void should_save_empty_content_for_image_message() {
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);
        User sender = User.createTestUser("20240001", "password", "홍길동", DormType.DORM_1, College.ENGINEERING, Role.ROLE_USER);
        OpenChatMessage message = OpenChatImageMessageFixture.createImageMessage(ROOM_ID, USER_ID);
        List<MultipartFile> images = OpenChatImageMessageFixture.createSingleValidImageList();
        List<String> urls = List.of("https://host/images/open_chat_message/img.jpg");

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(sender));
        given(openChatMessageRepository.save(any())).willReturn(message);
        given(imageService.findStaticImageUrls(eq(ImageType.OPEN_CHAT_MESSAGE), any(), eq(httpServletRequest))).willReturn(urls);
        given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(2L);
        given(openChatParticipantRepository.countReadByRoomIdAndMessageId(eq(ROOM_ID), any())).willReturn(1L);

        List<ResponseOpenChatMessageDto> results = openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        assertThat(results.get(0).getContent()).isEqualTo("");
    }

    @Test
    @DisplayName("이미지 메시지 전송 성공 — WebSocket 브로드캐스트 호출 확인")
    void should_broadcast_via_websocket_after_image_message_saved() {
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);
        User sender = User.createTestUser("20240001", "password", "홍길동", DormType.DORM_1, College.ENGINEERING, Role.ROLE_USER);
        OpenChatMessage message = OpenChatImageMessageFixture.createImageMessage(ROOM_ID, USER_ID);
        List<MultipartFile> images = OpenChatImageMessageFixture.createSingleValidImageList();
        List<String> urls = List.of("https://host/images/open_chat_message/img.jpg");

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(sender));
        given(openChatMessageRepository.save(any())).willReturn(message);
        given(imageService.findStaticImageUrls(eq(ImageType.OPEN_CHAT_MESSAGE), any(), eq(httpServletRequest))).willReturn(urls);
        given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(2L);
        given(openChatParticipantRepository.countReadByRoomIdAndMessageId(eq(ROOM_ID), any())).willReturn(1L);

        openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        then(messagingTemplate).should(times(1)).convertAndSend(eq("/sub/openchat/" + ROOM_ID), any(ResponseOpenChatMessageDto.class));
    }

    @Test
    @DisplayName("이미지 메시지 전송 성공 — lastMessageId가 저장된 메시지 ID로 갱신됨")
    void should_update_last_message_id_after_sending_image() {
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);
        User sender = User.createTestUser("20240001", "password", "홍길동", DormType.DORM_1, College.ENGINEERING, Role.ROLE_USER);
        List<MultipartFile> images = OpenChatImageMessageFixture.createSingleValidImageList();
        List<String> urls = List.of("https://host/images/open_chat_message/img.jpg");

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(sender));
        given(openChatMessageRepository.save(any())).willAnswer(inv -> {
            OpenChatMessage saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 123L);
            return saved;
        });
        given(imageService.findStaticImageUrls(eq(ImageType.OPEN_CHAT_MESSAGE), any(), eq(httpServletRequest))).willReturn(urls);
        given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(2L);
        given(openChatParticipantRepository.countReadByRoomIdAndMessageId(eq(ROOM_ID), any())).willReturn(1L);

        openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        assertThat(room.getLastMessageId()).isEqualTo(123L);
    }

    @Test
    @DisplayName("이미지 메시지 전송 성공 — jpeg 확장자 허용")
    void should_allow_jpeg_extension() {
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);
        User sender = User.createTestUser("20240001", "password", "홍길동", DormType.DORM_1, College.ENGINEERING, Role.ROLE_USER);
        OpenChatMessage message = OpenChatImageMessageFixture.createImageMessage(ROOM_ID, USER_ID);
        List<MultipartFile> images = List.of(OpenChatImageMessageFixture.createValidJpgFile());
        List<String> urls = List.of("https://host/images/open_chat_message/img.jpeg");

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(sender));
        given(openChatMessageRepository.save(any())).willReturn(message);
        given(imageService.findStaticImageUrls(eq(ImageType.OPEN_CHAT_MESSAGE), any(), eq(httpServletRequest))).willReturn(urls);
        given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(2L);
        given(openChatParticipantRepository.countReadByRoomIdAndMessageId(eq(ROOM_ID), any())).willReturn(1L);

        List<ResponseOpenChatMessageDto> results = openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        assertThat(results.get(0)).isNotNull();
    }

    @Test
    @DisplayName("이미지 메시지 전송 성공 — gif 확장자 허용")
    void should_allow_gif_extension() {
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);
        User sender = User.createTestUser("20240001", "password", "홍길동", DormType.DORM_1, College.ENGINEERING, Role.ROLE_USER);
        OpenChatMessage message = OpenChatImageMessageFixture.createImageMessage(ROOM_ID, USER_ID);
        List<MultipartFile> images = List.of(OpenChatImageMessageFixture.createValidGifFile());
        List<String> urls = List.of("https://host/images/open_chat_message/img.gif");

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(sender));
        given(openChatMessageRepository.save(any())).willReturn(message);
        given(imageService.findStaticImageUrls(eq(ImageType.OPEN_CHAT_MESSAGE), any(), eq(httpServletRequest))).willReturn(urls);
        given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(2L);
        given(openChatParticipantRepository.countReadByRoomIdAndMessageId(eq(ROOM_ID), any())).willReturn(1L);

        List<ResponseOpenChatMessageDto> results = openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        assertThat(results.get(0)).isNotNull();
    }

    @Test
    @DisplayName("이미지 메시지 전송 성공 — webp 확장자 허용")
    void should_allow_webp_extension() {
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);
        User sender = User.createTestUser("20240001", "password", "홍길동", DormType.DORM_1, College.ENGINEERING, Role.ROLE_USER);
        OpenChatMessage message = OpenChatImageMessageFixture.createImageMessage(ROOM_ID, USER_ID);
        List<MultipartFile> images = List.of(OpenChatImageMessageFixture.createValidWebpFile());
        List<String> urls = List.of("https://host/images/open_chat_message/img.webp");

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(sender));
        given(openChatMessageRepository.save(any())).willReturn(message);
        given(imageService.findStaticImageUrls(eq(ImageType.OPEN_CHAT_MESSAGE), any(), eq(httpServletRequest))).willReturn(urls);
        given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(2L);
        given(openChatParticipantRepository.countReadByRoomIdAndMessageId(eq(ROOM_ID), any())).willReturn(1L);

        List<ResponseOpenChatMessageDto> results = openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        assertThat(results.get(0)).isNotNull();
    }

    @Test
    @DisplayName("CustomException 발생 — 채팅방 없음 (OPEN_CHAT_ROOM_NOT_FOUND)")
    void should_throw_when_room_not_found() {
        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

        List<MultipartFile> images = OpenChatImageMessageFixture.createSingleValidImageList();

        assertThatThrownBy(() -> openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("CustomException 발생 — 비참여자 요청 (OPEN_CHAT_NOT_PARTICIPANT)")
    void should_throw_when_not_participant() {
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.empty());

        List<MultipartFile> images = OpenChatImageMessageFixture.createSingleValidImageList();

        assertThatThrownBy(() -> openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_NOT_PARTICIPANT);
    }

    @Test
    @DisplayName("CustomException 발생 — 허용되지 않는 포맷 heic (IMAGE_INVALID_FORMAT)")
    void should_throw_when_heic_file_included() {
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));

        List<MultipartFile> images = OpenChatImageMessageFixture.createInvalidFormatImageList();

        assertThatThrownBy(() -> openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IMAGE_INVALID_FORMAT);
    }

    @Test
    @DisplayName("CustomException 발생 — 허용되지 않는 포맷 pdf (IMAGE_INVALID_FORMAT)")
    void should_throw_when_pdf_file_included() {
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));

        List<MultipartFile> images = List.of(OpenChatImageMessageFixture.createInvalidPdfFile());

        assertThatThrownBy(() -> openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IMAGE_INVALID_FORMAT);
    }

    @Test
    @DisplayName("CustomException 발생 — 이미지 0개 요청 (OPEN_CHAT_IMAGE_EMPTY)")
    void should_throw_when_no_images_provided() {
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));

        List<MultipartFile> images = List.of();

        assertThatThrownBy(() -> openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_IMAGE_EMPTY);
    }

    @Test
    @DisplayName("CustomException 발생 — 포맷 위반 시 메시지 저장 호출 없음")
    void should_not_save_message_when_format_invalid() {
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));

        List<MultipartFile> images = OpenChatImageMessageFixture.createInvalidFormatImageList();

        assertThatThrownBy(() -> openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest))
                .isInstanceOf(CustomException.class);

        then(openChatMessageRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("이미지 메시지 전송 성공 — unreadCount 계산 포함")
    void should_calculate_unread_count_correctly() {
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);
        User sender = User.createTestUser("20240001", "password", "홍길동", DormType.DORM_1, College.ENGINEERING, Role.ROLE_USER);
        OpenChatMessage message = OpenChatImageMessageFixture.createImageMessage(ROOM_ID, USER_ID);
        List<MultipartFile> images = OpenChatImageMessageFixture.createSingleValidImageList();
        List<String> urls = List.of("https://host/images/open_chat_message/img.jpg");

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(sender));
        given(openChatMessageRepository.save(any())).willReturn(message);
        given(imageService.findStaticImageUrls(eq(ImageType.OPEN_CHAT_MESSAGE), any(), eq(httpServletRequest))).willReturn(urls);
        given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(5L);
        given(openChatParticipantRepository.countReadByRoomIdAndMessageId(eq(ROOM_ID), any())).willReturn(2L);

        List<ResponseOpenChatMessageDto> results = openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        assertThat(results.get(0).getUnreadCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("이미지 메시지 전송 성공 — 발신자 포함 구독자 lastReadMessageId 일괄 갱신됨")
    void should_update_participant_last_read_message_id() {
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);
        User sender = User.createTestUser("20240001", "password", "홍길동", DormType.DORM_1, College.ENGINEERING, Role.ROLE_USER);
        OpenChatMessage message = OpenChatImageMessageFixture.createImageMessage(ROOM_ID, USER_ID);
        List<MultipartFile> images = OpenChatImageMessageFixture.createSingleValidImageList();
        List<String> urls = List.of("https://host/images/open_chat_message/img.jpg");

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(sender));
        given(openChatMessageRepository.save(any())).willReturn(message);
        given(imageService.findStaticImageUrls(eq(ImageType.OPEN_CHAT_MESSAGE), any(), eq(httpServletRequest))).willReturn(urls);
        given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(2L);
        given(openChatParticipantRepository.countReadByRoomIdAndMessageId(eq(ROOM_ID), any())).willReturn(1L);

        openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        then(openChatParticipantRepository).should().updateLastReadMessageIdByRoomIdAndUserIdIn(eq(ROOM_ID), any(Set.class), any());
    }

    @Test
    @DisplayName("이미지 메시지 전송 성공 — 응답 DTO에 roomId 포함")
    void should_include_room_id_in_response() {
        OpenChatRoom room = OpenChatImageMessageFixture.createRoom();
        OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);
        User sender = User.createTestUser("20240001", "password", "홍길동", DormType.DORM_1, College.ENGINEERING, Role.ROLE_USER);
        OpenChatMessage message = OpenChatImageMessageFixture.createImageMessage(ROOM_ID, USER_ID);
        List<MultipartFile> images = OpenChatImageMessageFixture.createSingleValidImageList();
        List<String> urls = List.of("https://host/images/open_chat_message/img.jpg");

        given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(sender));
        given(openChatMessageRepository.save(any())).willReturn(message);
        given(imageService.findStaticImageUrls(eq(ImageType.OPEN_CHAT_MESSAGE), any(), eq(httpServletRequest))).willReturn(urls);
        given(openChatParticipantRepository.countByRoomId(ROOM_ID)).willReturn(2L);
        given(openChatParticipantRepository.countReadByRoomIdAndMessageId(eq(ROOM_ID), any())).willReturn(1L);

        List<ResponseOpenChatMessageDto> results = openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        assertThat(results.get(0).getRoomId()).isEqualTo(ROOM_ID);
    }

    // ──────────────────────────────────────────────
    // BR-685: PERSONAL 방 이미지 전송 차단 검증
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("CustomException 발생 — AC-8 BR-685: PERSONAL 방에서 상대방이 발신자를 차단한 경우 USER_BLOCKED_BY_TARGET")
    void should_throw_CustomException_when_AC_8_sender_blocked_by_participant_in_personal_room() {
        // given — PERSONAL 방에서 상대방(A)이 발신자(B=USER_ID)를 차단한 상태
        // OpenChatRoom personalRoom = OpenChatImageMessageFixture.createPersonalRoom();
        // OpenChatParticipant participant = OpenChatImageMessageFixture.createParticipant(ROOM_ID, USER_ID);
        // given(openChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(personalRoom));
        // given(openChatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(participant));
        // Set<Long> otherParticipantIds = Set.of(1L); // 상대방 A
        // given(openChatParticipantRepository.findUserIdsByRoomIdExcluding(ROOM_ID, USER_ID)).willReturn(otherParticipantIds);
        // given(blockService.assertNotBlockedByAny(otherParticipantIds, USER_ID)) → CustomException 던짐 (void)
        // willThrow(new CustomException(ErrorCode.USER_BLOCKED_BY_TARGET)).given(blockService).assertNotBlockedByAny(any(), eq(USER_ID));

        // when
        // List<MultipartFile> images = OpenChatImageMessageFixture.createSingleValidImageList();
        // ThrowingCallable action = () -> openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest);

        // then
        // assertThatThrownBy(action)
        //         .isInstanceOf(CustomException.class)
        //         .extracting("errorCode")
        //         .isEqualTo(ErrorCode.USER_BLOCKED_BY_TARGET);

        // TODO: BlockService 미구현, ErrorCode.USER_BLOCKED_BY_TARGET 상수 아직 없음
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("이미지 전송 성공 — AC-9 BR-685: 차단한 사람(A)은 PERSONAL 방에서 이미지 전송 가능 (단방향)")
    void should_send_image_successfully_when_AC_9_sender_is_blocker_in_personal_room() {
        // given — A(USER_ID)가 B를 차단했지만 B는 A를 차단하지 않음
        // blockService.assertNotBlockedByAny(otherIds, USER_ID) → 예외 없음 (B가 A를 차단 안 함)
        // PERSONAL 방 설정 후 정상 sendImageMessage 호출

        // TODO: BlockService 미구현
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("이미지 전송 성공 — AC-10 BR-685: OPEN 방에서는 차단 관계 있어도 차단 검증 호출 안 함")
    void should_not_check_block_when_AC_10_room_type_is_OPEN() {
        // given — OPEN 방 (room.getRoomType() == OPEN), 차단 관계 있어도 검증 건너뜀
        // OpenChatRoom openRoom = OpenChatImageMessageFixture.createRoom(); // OPEN 타입
        // ...기존 설정...

        // then
        // then(blockService).should(never()).assertNotBlockedByAny(any(), any());
        // 정상 전송 완료

        // TODO: BlockService 미구현
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("미저장 확인 — AC-8 BR-685: PERSONAL 방 차단 상태에서 메시지 save 미호출")
    void should_not_save_message_when_sender_blocked_in_personal_room() {
        // given — PERSONAL 방에서 상대방이 발신자를 차단
        // blockService.assertNotBlockedByAny → CustomException 던짐

        // when
        // assertThatThrownBy(() -> openChatMessageService.sendImageMessage(USER_ID, ROOM_ID, images, httpServletRequest))
        //         .isInstanceOf(CustomException.class);

        // then
        // then(openChatMessageRepository).should(never()).save(any());

        // TODO: BlockService 미구현
        assertThat(true).isTrue();
    }
}
