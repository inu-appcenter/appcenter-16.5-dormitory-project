package com.example.appcenter_project.domain.roommate.service;

import com.example.appcenter_project.common.image.dto.ImageLinkDto;
import com.example.appcenter_project.common.image.entity.Image;
import com.example.appcenter_project.common.image.enums.ImageType;
import com.example.appcenter_project.common.image.repository.ImageRepository;
import com.example.appcenter_project.common.image.service.ImageService;
import com.example.appcenter_project.domain.fcm.repository.FcmOutboxRepository;
import com.example.appcenter_project.domain.notification.entity.Notification;
import com.example.appcenter_project.domain.notification.service.NotificationService;
import com.example.appcenter_project.domain.roommate.dto.response.ResponseRoommateChatCreateEventDto;
import com.example.appcenter_project.domain.roommate.dto.response.ResponseRoommateChatDto;
import com.example.appcenter_project.domain.roommate.dto.response.ResponseRoommateChatReadEventDto;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingChat;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom;
import com.example.appcenter_project.domain.roommate.enums.RoommateChattingMessageType;
import com.example.appcenter_project.domain.roommate.fixture.RoommateChattingImageFixture;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingChatRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingRoomRepository;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.repository.FcmTokenRepository;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.config.RoommateWebSocketEventListener;
import com.example.appcenter_project.global.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static com.example.appcenter_project.global.exception.ErrorCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

// BR-791 룸메이트 채팅 이미지 전송·조회
@ExtendWith(MockitoExtension.class)
class RoommateChattingImageServiceTest {

    @Mock RoommateChattingChatRepository chatRepository;
    @Mock RoommateChattingRoomRepository chatRoomRepository;
    @Mock UserRepository userRepository;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock NotificationService notificationService;
    @Mock FcmOutboxRepository fcmOutboxRepository;
    @Mock FcmTokenRepository fcmTokenRepository;
    @Mock ImageService imageService;
    @Mock ImageRepository imageRepository;
    @Mock HttpServletRequest request;

    @InjectMocks
    RoommateChattingChatService chatService;

    private static final Long HOST_ID = 1L;
    private static final Long GUEST_ID = 2L;
    private static final String IMAGE_URL = "https://host/images/roommate_chat_message/img.jpg";

    private User host;
    private User guest;
    private RoommateChattingRoom room;
    private Long roomId;
    private final AtomicLong chatIdSequence = new AtomicLong(100L);

    @BeforeEach
    void setUp() {
        host = User.createForTest(HOST_ID, "호스트");
        guest = User.createForTest(GUEST_ID, "게스트");
        room = RoommateChattingImageFixture.createRoom(host, guest);
        roomId = room.getId();
    }

    @AfterEach
    void clearOnlineMap() {
        RoommateWebSocketEventListener.roommateChatRoomInUserMap.clear();
    }

    private void givenGuestSenderAndRoom() {
        given(userRepository.findById(GUEST_ID)).willReturn(Optional.of(guest));
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
    }

    private void givenSaveAssignsIdAndImageUrl() {
        given(chatRepository.save(any(RoommateChattingChat.class))).willAnswer(invocation -> {
            RoommateChattingChat chat = invocation.getArgument(0);
            ReflectionTestUtils.setField(chat, "id", chatIdSequence.incrementAndGet());
            ReflectionTestUtils.setField(chat, "createdDate", LocalDateTime.now());
            return chat;
        });
        given(imageService.findStaticImageUrls(eq(ImageType.ROOMMATE_CHAT_MESSAGE), anyLong(), eq(request)))
                .willReturn(List.of(IMAGE_URL));
    }

    private void givenNotificationCreated() {
        given(notificationService.createChatNotification(anyString(), anyLong(), anyString()))
                .willReturn(mock(Notification.class));
    }

    private void givenHostOnline() {
        RoommateWebSocketEventListener.roommateChatRoomInUserMap
                .computeIfAbsent(roomId.toString(), k -> new ArrayList<>())
                .add(HOST_ID.toString());
    }

    // ===== 전송 =====

    @Test
    @DisplayName("BR-791 AC-1 이미지 3장 전송 — 메시지 3개 저장, 이미지 3장 저장, 응답 3건")
    void should_save_3_image_messages_when_3_images_sent() {
        givenGuestSenderAndRoom();
        givenSaveAssignsIdAndImageUrl();
        givenNotificationCreated();

        List<ResponseRoommateChatDto> result =
                chatService.sendImageChats(GUEST_ID, roomId, RoommateChattingImageFixture.createImages(3), request);

        assertThat(result).hasSize(3);
        then(chatRepository).should(times(3)).save(any(RoommateChattingChat.class));
        then(imageService).should(times(3)).saveImage(eq(ImageType.ROOMMATE_CHAT_MESSAGE), anyLong(), any(MultipartFile.class));
    }

    @Test
    @DisplayName("BR-791 AC-1 이미지 메시지 — IMAGE 타입으로 저장, 응답 content [이미지]·imageUrls 포함")
    void should_return_image_urls_and_placeholder_content() {
        givenGuestSenderAndRoom();
        givenSaveAssignsIdAndImageUrl();
        givenNotificationCreated();

        List<ResponseRoommateChatDto> result =
                chatService.sendImageChats(GUEST_ID, roomId, RoommateChattingImageFixture.createImages(1), request);

        then(chatRepository).should().save(argThat(chat -> chat.getMessageType() == RoommateChattingMessageType.IMAGE));
        assertThat(result.get(0).getContent()).isEqualTo("[이미지]");
        assertThat(result.get(0).getImageUrls()).containsExactly(IMAGE_URL);
    }

    @Test
    @DisplayName("BR-791 AC-2 이미지 3장 전송 — 생성 이벤트 3회 브로드캐스트")
    void should_broadcast_create_event_per_image() {
        givenGuestSenderAndRoom();
        givenSaveAssignsIdAndImageUrl();
        givenNotificationCreated();

        chatService.sendImageChats(GUEST_ID, roomId, RoommateChattingImageFixture.createImages(3), request);

        then(messagingTemplate).should(times(3))
                .convertAndSend(eq("/sub/roommate/chat/" + roomId), any(ResponseRoommateChatCreateEventDto.class));
    }

    @Test
    @DisplayName("BR-791 AC-5 수신자 오프라인 — 여러 장이어도 [이미지] 알림 1회")
    void should_notify_once_with_image_placeholder_when_receiver_offline() {
        givenGuestSenderAndRoom();
        givenSaveAssignsIdAndImageUrl();
        givenNotificationCreated();

        chatService.sendImageChats(GUEST_ID, roomId, RoommateChattingImageFixture.createImages(3), request);

        then(notificationService).should(times(1)).createChatNotification("게스트", roomId, "[이미지]");
    }

    @Test
    @DisplayName("BR-791 AC-5 수신자 온라인 — 이미지마다 읽음 이벤트, 알림 없음")
    void should_send_read_event_and_skip_notification_when_receiver_online() {
        givenGuestSenderAndRoom();
        givenSaveAssignsIdAndImageUrl();
        givenHostOnline();

        List<ResponseRoommateChatDto> result =
                chatService.sendImageChats(GUEST_ID, roomId, RoommateChattingImageFixture.createImages(2), request);

        then(messagingTemplate).should(times(2))
                .convertAndSend(eq("/sub/roommate/chat/" + roomId), any(ResponseRoommateChatReadEventDto.class));
        then(notificationService).shouldHaveNoInteractions();
        assertThat(result).allMatch(ResponseRoommateChatDto::isRead);
    }

    @Test
    @DisplayName("BR-791 AC-6 수신자(host)가 나간 상태 — 이미지 전송 시 자동 재진입")
    void should_rejoin_receiver_when_receiver_left() {
        room.leaveAsHost();
        givenGuestSenderAndRoom();
        givenSaveAssignsIdAndImageUrl();
        givenNotificationCreated();

        chatService.sendImageChats(GUEST_ID, roomId, RoommateChattingImageFixture.createImages(1), request);

        assertThat(room.isHostLeft()).isFalse();
    }

    // ===== 예외 =====

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("BR-791 AC-4 이미지 없음 — 400 ROOMMATE_CHAT_IMAGE_EMPTY")
    void should_throw_when_images_empty(List<MultipartFile> images) {
        givenGuestSenderAndRoom();

        assertThatThrownBy(() -> chatService.sendImageChats(GUEST_ID, roomId, images, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ROOMMATE_CHAT_IMAGE_EMPTY);
        then(chatRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("BR-791 AC-4 이미지 6장 — 400 ROOMMATE_CHAT_IMAGE_COUNT_EXCEEDED, 저장 없음")
    void should_throw_when_more_than_5_images() {
        givenGuestSenderAndRoom();

        assertThatThrownBy(() -> chatService.sendImageChats(GUEST_ID, roomId, RoommateChattingImageFixture.createImages(6), request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ROOMMATE_CHAT_IMAGE_COUNT_EXCEEDED);
        then(chatRepository).should(never()).save(any());
    }

    @ParameterizedTest(name = "{0} / {1}")
    @CsvSource({
            "memo.txt, text/plain",
            "photo.heic, image/heic",
            "noext, image/jpeg",
            "fake.jpg, text/plain"
    })
    @DisplayName("BR-791 AC-4 허용되지 않는 형식 — 400 IMAGE_INVALID_FORMAT")
    void should_throw_when_invalid_format(String filename, String contentType) {
        givenGuestSenderAndRoom();
        MultipartFile image = RoommateChattingImageFixture.createImage(filename, contentType, 10);

        assertThatThrownBy(() -> chatService.sendImageChats(GUEST_ID, roomId, List.of(image), request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", IMAGE_INVALID_FORMAT);
    }

    @Test
    @DisplayName("BR-791 AC-4 10MB 초과 — 400 IMAGE_INVALID_FORMAT")
    void should_throw_when_image_exceeds_10mb() {
        givenGuestSenderAndRoom();
        MultipartFile image = RoommateChattingImageFixture.createImage("big.jpg", "image/jpeg", 10 * 1024 * 1024 + 1);

        assertThatThrownBy(() -> chatService.sendImageChats(GUEST_ID, roomId, List.of(image), request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", IMAGE_INVALID_FORMAT);
    }

    @Test
    @DisplayName("BR-791 AC-4 채팅방 참여자가 아님 — 403 ROOMMATE_CHAT_PARTICIPANT_FORBIDDEN")
    void should_throw_when_sender_not_participant() {
        User stranger = User.createForTest(3L, "외부인");
        given(userRepository.findById(3L)).willReturn(Optional.of(stranger));
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));

        assertThatThrownBy(() -> chatService.sendImageChats(3L, roomId, RoommateChattingImageFixture.createImages(1), request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ROOMMATE_CHAT_PARTICIPANT_FORBIDDEN);
    }

    @Test
    @DisplayName("BR-791 AC-4 채팅방 없음 — 404 ROOMMATE_CHAT_ROOM_NOT_FOUND")
    void should_throw_when_room_not_found() {
        given(userRepository.findById(GUEST_ID)).willReturn(Optional.of(guest));
        given(chatRoomRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.sendImageChats(GUEST_ID, 999L, RoommateChattingImageFixture.createImages(1), request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ROOMMATE_CHAT_ROOM_NOT_FOUND);
    }

    // ===== 조회 =====

    @Test
    @DisplayName("BR-791 AC-3 채팅 내역 조회 — 이미지 메시지는 imageUrls 포함, 텍스트는 빈 배열")
    void should_include_image_urls_for_image_message_in_chat_list() {
        RoommateChattingChat imageChat = RoommateChattingImageFixture.createSavedImageChat(10L, room, host, false);
        RoommateChattingChat textChat = RoommateChattingImageFixture.createSavedTextChat(11L, room, host);
        Image stored = RoommateChattingImageFixture.createStoredImage(10L);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(userRepository.findById(GUEST_ID)).willReturn(Optional.of(guest));
        given(chatRepository.findByRoommateChattingRoom(room)).willReturn(List.of(imageChat, textChat));
        given(imageService.findImage(eq(ImageType.USER), anyLong(), eq(request))).willReturn(ImageLinkDto.ofNull());
        given(imageRepository.findByImageTypeAndEntityIdIn(ImageType.ROOMMATE_CHAT_MESSAGE, List.of(10L)))
                .willReturn(List.of(stored));
        given(imageService.getImageUrl(ImageType.ROOMMATE_CHAT_MESSAGE, stored, request)).willReturn(IMAGE_URL);

        List<ResponseRoommateChatDto> result = chatService.getChatList(GUEST_ID, roomId, request);

        assertThat(result.get(0).getImageUrls()).containsExactly(IMAGE_URL);
        assertThat(result.get(1).getImageUrls()).isEmpty();
    }

    @Test
    @DisplayName("BR-791 AC-3 삭제된 이미지 메시지 — imageUrls 빈 배열, 이미지 조회 안 함")
    void should_hide_image_urls_for_deleted_image_message() {
        RoommateChattingChat deletedImageChat = RoommateChattingImageFixture.createSavedImageChat(10L, room, host, true);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(userRepository.findById(GUEST_ID)).willReturn(Optional.of(guest));
        given(chatRepository.findByRoommateChattingRoom(room)).willReturn(List.of(deletedImageChat));
        given(imageService.findImage(eq(ImageType.USER), anyLong(), eq(request))).willReturn(ImageLinkDto.ofNull());

        List<ResponseRoommateChatDto> result = chatService.getChatList(GUEST_ID, roomId, request);

        assertThat(result.get(0).getImageUrls()).isEmpty();
        assertThat(result.get(0).getContent()).isEqualTo("삭제된 메시지입니다.");
        then(imageRepository).shouldHaveNoInteractions();
    }
}