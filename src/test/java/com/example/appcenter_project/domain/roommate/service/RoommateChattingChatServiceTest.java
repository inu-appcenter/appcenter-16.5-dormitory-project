package com.example.appcenter_project.domain.roommate.service;

import com.example.appcenter_project.common.image.dto.ImageLinkDto;
import com.example.appcenter_project.common.image.enums.ImageType;
import com.example.appcenter_project.common.image.service.ImageService;
import com.example.appcenter_project.domain.fcm.entity.FcmToken;
import com.example.appcenter_project.domain.fcm.repository.FcmOutboxRepository;
import com.example.appcenter_project.domain.notification.entity.Notification;
import com.example.appcenter_project.domain.user.repository.FcmTokenRepository;
import com.example.appcenter_project.domain.notification.service.NotificationService;
import com.example.appcenter_project.domain.roommate.dto.request.RequestRoommateChatDto;
import com.example.appcenter_project.domain.roommate.dto.response.ResponseRoommateChatDto;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingChat;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingChatRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingRoomRepository;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.config.RoommateWebSocketEventListener;
import com.example.appcenter_project.global.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.example.appcenter_project.global.exception.ErrorCode.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoommateChattingChatServiceTest {

    @Mock
    RoommateChattingChatRepository chatRepository;

    @Mock
    RoommateChattingRoomRepository chatRoomRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @Mock
    NotificationService notificationService;

    @Mock
    FcmOutboxRepository fcmOutboxRepository;

    @Mock
    FcmTokenRepository fcmTokenRepository;

    @Mock
    ImageService imageService;

    @InjectMocks
    RoommateChattingChatService roommateChattingChatService;

    @AfterEach
    void clearOnlineMap() {
        RoommateWebSocketEventListener.roommateChatRoomInUserMap.clear();
    }

    private User buildMockUser(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getName()).thenReturn("사용자" + id);
        when(user.getStudentNumber()).thenReturn("2025000" + id);
        return user;
    }

    @Test
    @DisplayName("채팅 전송 - 정상 전송 (guest가 host에게)")
    void sendChat_정상_전송() {
        User guest = buildMockUser(2L);
        User host = buildMockUser(1L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(guest));

        RoommateChattingRoom room = mock(RoommateChattingRoom.class);
        when(room.getId()).thenReturn(100L);
        when(room.getGuest()).thenReturn(guest);
        when(room.getHost()).thenReturn(host);

        RequestRoommateChatDto dto = mock(RequestRoommateChatDto.class);
        when(dto.getRoommateChattingRoomId()).thenReturn(100L);
        when(dto.getContent()).thenReturn("안녕하세요!");

        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        RoommateChattingChat savedChat = mock(RoommateChattingChat.class);
        when(savedChat.getId()).thenReturn(1L);
        when(savedChat.getMember()).thenReturn(guest);
        when(savedChat.getContent()).thenReturn("안녕하세요!");
        when(savedChat.isReadByReceiver()).thenReturn(false);
        when(savedChat.getCreatedDate()).thenReturn(LocalDateTime.now());
        when(savedChat.getRoommateChattingRoom()).thenReturn(room);
        when(chatRepository.save(any(RoommateChattingChat.class))).thenReturn(savedChat);

        Notification mockNotification = mock(Notification.class);
        when(mockNotification.getTitle()).thenReturn("채팅 알림");
        when(mockNotification.getBody()).thenReturn("안녕하세요!");
        when(notificationService.createChatNotification(anyString(), anyLong(), anyString()))
                .thenReturn(mockNotification);

        ResponseRoommateChatDto result = roommateChattingChatService.sendChat(2L, dto);

        assertThat(result).isNotNull();
        verify(chatRepository).save(any(RoommateChattingChat.class));
        verify(messagingTemplate).convertAndSend(anyString(), any(ResponseRoommateChatDto.class));
    }

    @Test
    @DisplayName("채팅 전송 - 유저 없으면 예외")
    void sendChat_유저없으면_예외() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RequestRoommateChatDto dto = mock(RequestRoommateChatDto.class);

        assertThatThrownBy(() -> roommateChattingChatService.sendChat(99L, dto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", USER_NOT_FOUND);
    }

    @Test
    @DisplayName("채팅 전송 - 채팅방 없으면 예외")
    void sendChat_채팅방없으면_예외() {
        User user = buildMockUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        RequestRoommateChatDto dto = mock(RequestRoommateChatDto.class);
        when(dto.getRoommateChattingRoomId()).thenReturn(99L);
        when(chatRoomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roommateChattingChatService.sendChat(1L, dto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ROOMMATE_CHAT_ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("채팅 전송 - 채팅방 참여자가 아니면 예외")
    void sendChat_참여자아님_예외() {
        User user = buildMockUser(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        User host = buildMockUser(1L);
        User guest = buildMockUser(2L);
        RoommateChattingRoom room = mock(RoommateChattingRoom.class);
        when(room.getId()).thenReturn(100L);
        when(room.getHost()).thenReturn(host);
        when(room.getGuest()).thenReturn(guest);

        RequestRoommateChatDto dto = mock(RequestRoommateChatDto.class);
        when(dto.getRoommateChattingRoomId()).thenReturn(100L);
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> roommateChattingChatService.sendChat(3L, dto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ROOMMATE_CHAT_ROOM_FORBIDDEN);
    }

    @Test
    @DisplayName("읽지 않은 메시지 수 조회 - 정상 반환")
    void getUnReadCountByUserIdAdRoomId_정상_반환() {
        User user = buildMockUser(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        RoommateChattingRoom room = mock(RoommateChattingRoom.class);
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        RoommateChattingChat unreadChat = mock(RoommateChattingChat.class);
        when(chatRepository.findByRoommateChattingRoomAndMemberNotAndReadByReceiverFalse(room, user))
                .thenReturn(List.of(unreadChat, unreadChat));

        Integer result = roommateChattingChatService.getUnReadCountByUserIdAdRoomId(2L, 100L);

        assertThat(result).isEqualTo(2);
    }

    @Test
    @DisplayName("채팅 목록 조회 - 권한 없으면 예외")
    void getChatList_권한없으면_예외() {
        User user = buildMockUser(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        User host = buildMockUser(1L);
        User guest = buildMockUser(2L);
        RoommateChattingRoom room = mock(RoommateChattingRoom.class);
        when(room.getHost()).thenReturn(host);
        when(room.getGuest()).thenReturn(guest);
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() ->
                roommateChattingChatService.getChatList(3L, 100L, mock(HttpServletRequest.class)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ROOMMATE_CHAT_ROOM_FORBIDDEN);
    }

    // ===== @ParameterizedTest: FCM 발송 조건 =====

    @ParameterizedTest(name = "수신자 온라인={0} → FCM 발송={1}")
    @CsvSource({
        "false, true",   // 수신자 오프라인 → FCM 발송해야 함
        "true,  false"   // 수신자 온라인   → FCM 미발송
    })
    @DisplayName("채팅 전송 - 수신자 온라인 여부에 따른 FCM 발송 조건")
    void sendChat_FCM_발송_조건(boolean receiverOnline, boolean expectFcmSent) {
        User guest = buildMockUser(2L);
        User host  = buildMockUser(1L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(guest));

        RoommateChattingRoom room = mock(RoommateChattingRoom.class);
        when(room.getId()).thenReturn(100L);
        when(room.getGuest()).thenReturn(guest);
        when(room.getHost()).thenReturn(host);
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        RoommateChattingChat savedChat = mock(RoommateChattingChat.class);
        when(savedChat.getId()).thenReturn(1L);
        when(savedChat.getMember()).thenReturn(guest);
        when(savedChat.getContent()).thenReturn("테스트");
        when(savedChat.isReadByReceiver()).thenReturn(receiverOnline);
        when(savedChat.getCreatedDate()).thenReturn(LocalDateTime.now());
        when(savedChat.getRoommateChattingRoom()).thenReturn(room);
        when(chatRepository.save(any(RoommateChattingChat.class))).thenReturn(savedChat);

        if (receiverOnline) {
            RoommateWebSocketEventListener.roommateChatRoomInUserMap
                    .computeIfAbsent("100", k -> new ArrayList<>())
                    .add("1");
        }

        Notification mockNotification = mock(Notification.class);
        when(mockNotification.getTitle()).thenReturn("채팅 알림");
        when(mockNotification.getBody()).thenReturn("테스트");
        when(notificationService.createChatNotification(anyString(), anyLong(), anyString()))
                .thenReturn(mockNotification);

        FcmToken mockToken = mock(FcmToken.class);
        when(mockToken.getToken()).thenReturn("test-token");
        when(fcmTokenRepository.findAllByUser(host)).thenReturn(List.of(mockToken));

        RequestRoommateChatDto dto = mock(RequestRoommateChatDto.class);
        when(dto.getRoommateChattingRoomId()).thenReturn(100L);
        when(dto.getContent()).thenReturn("테스트");

        roommateChattingChatService.sendChat(2L, dto);

        if (expectFcmSent) {
            verify(fcmOutboxRepository).saveAll(anyList());
        } else {
            verify(fcmOutboxRepository, never()).saveAll(anyList());
        }
    }

    // ===== @ParameterizedTest: 발신자 역할(host/guest) 양방향 =====

    static Stream<org.junit.jupiter.params.provider.Arguments> senderRoleProvider() {
        return Stream.of(
            arguments("guest가 host에게", 2L, 1L),
            arguments("host가 guest에게", 1L, 2L)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("senderRoleProvider")
    @DisplayName("채팅 전송 - host/guest 양방향 정상 전송")
    void sendChat_발신자_역할별_정상_전송(String scenario, Long senderId, Long receiverId) {
        User sender   = buildMockUser(senderId);
        User receiver = buildMockUser(receiverId);
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));

        RoommateChattingRoom room = mock(RoommateChattingRoom.class);
        when(room.getId()).thenReturn(100L);
        // senderId == 2L 이면 sender가 guest
        when(room.getGuest()).thenReturn(senderId == 2L ? sender : receiver);
        when(room.getHost()).thenReturn(senderId == 1L ? sender : receiver);
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        RoommateChattingChat savedChat = mock(RoommateChattingChat.class);
        when(savedChat.getId()).thenReturn(1L);
        when(savedChat.getMember()).thenReturn(sender);
        when(savedChat.getContent()).thenReturn("안녕");
        when(savedChat.isReadByReceiver()).thenReturn(false);
        when(savedChat.getCreatedDate()).thenReturn(LocalDateTime.now());
        when(savedChat.getRoommateChattingRoom()).thenReturn(room);
        when(chatRepository.save(any(RoommateChattingChat.class))).thenReturn(savedChat);

        Notification mockNotification = mock(Notification.class);
        when(mockNotification.getTitle()).thenReturn("채팅 알림");
        when(mockNotification.getBody()).thenReturn("안녕");
        when(notificationService.createChatNotification(anyString(), anyLong(), anyString()))
                .thenReturn(mockNotification);

        RequestRoommateChatDto dto = mock(RequestRoommateChatDto.class);
        when(dto.getRoommateChattingRoomId()).thenReturn(100L);
        when(dto.getContent()).thenReturn("안녕");

        ResponseRoommateChatDto result = roommateChattingChatService.sendChat(senderId, dto);

        assertThat(result).isNotNull();
        verify(chatRepository).save(any(RoommateChattingChat.class));
    }

    @Test
    @DisplayName("채팅 전송 - 수신자(host)가 나간 상태면 자동 재진입")
    void sendChat_수신자가_나간_상태면_자동_재진입() {
        User guest = buildMockUser(2L);
        User host = buildMockUser(1L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(guest));

        RoommateChattingRoom room = mock(RoommateChattingRoom.class);
        when(room.getId()).thenReturn(100L);
        when(room.getGuest()).thenReturn(guest);
        when(room.getHost()).thenReturn(host);
        when(room.isHostLeft()).thenReturn(true);
        when(room.isGuestLeft()).thenReturn(false);
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        RoommateChattingChat savedChat = mock(RoommateChattingChat.class);
        when(savedChat.getId()).thenReturn(1L);
        when(savedChat.getMember()).thenReturn(guest);
        when(savedChat.getContent()).thenReturn("안녕하세요!");
        when(savedChat.isReadByReceiver()).thenReturn(false);
        when(savedChat.getCreatedDate()).thenReturn(LocalDateTime.now());
        when(savedChat.getRoommateChattingRoom()).thenReturn(room);
        when(chatRepository.save(any(RoommateChattingChat.class))).thenReturn(savedChat);

        Notification mockNotification = mock(Notification.class);
        when(mockNotification.getTitle()).thenReturn("채팅 알림");
        when(mockNotification.getBody()).thenReturn("안녕하세요!");
        when(notificationService.createChatNotification(anyString(), anyLong(), anyString()))
                .thenReturn(mockNotification);

        RequestRoommateChatDto dto = mock(RequestRoommateChatDto.class);
        when(dto.getRoommateChattingRoomId()).thenReturn(100L);
        when(dto.getContent()).thenReturn("안녕하세요!");

        roommateChattingChatService.sendChat(2L, dto);

        verify(room).rejoinAsHost();
    }

    @Test
    @DisplayName("채팅 목록 조회 - 정상 반환")
    void getChatList_정상_반환() {
        User guest = buildMockUser(2L);
        User host = buildMockUser(1L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(guest));

        RoommateChattingRoom room = mock(RoommateChattingRoom.class);
        when(room.getId()).thenReturn(100L);
        when(room.getGuest()).thenReturn(guest);
        when(room.getHost()).thenReturn(host);
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        RoommateChattingChat chat = mock(RoommateChattingChat.class);
        when(chat.getId()).thenReturn(1L);
        when(chat.getMember()).thenReturn(host);
        when(chat.getContent()).thenReturn("안녕하세요!");
        when(chat.isReadByReceiver()).thenReturn(false);
        when(chat.getCreatedDate()).thenReturn(LocalDateTime.now());
        when(chat.getRoommateChattingRoom()).thenReturn(room);
        when(chatRepository.findByRoommateChattingRoom(room)).thenReturn(List.of(chat));

        ImageLinkDto mockImage = mock(ImageLinkDto.class);
        when(mockImage.getImageUrl()).thenReturn("http://example.com/image.jpg");
        when(imageService.findImage(eq(ImageType.USER), anyLong(), any(HttpServletRequest.class)))
                .thenReturn(mockImage);

        List<ResponseRoommateChatDto> result =
                roommateChattingChatService.getChatList(2L, 100L, mock(HttpServletRequest.class));

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("채팅 목록 조회 - 시스템 메시지(member==null) 포함해도 NPE 없이 반환")
    void getChatList_시스템메시지포함_정상반환() {
        User guest = buildMockUser(2L);
        User host = buildMockUser(1L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(guest));

        RoommateChattingRoom room = mock(RoommateChattingRoom.class);
        when(room.getId()).thenReturn(100L);
        when(room.getGuest()).thenReturn(guest);
        when(room.getHost()).thenReturn(host);
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        // 일반 메시지 (host가 보냄)
        RoommateChattingChat normal = mock(RoommateChattingChat.class);
        when(normal.getId()).thenReturn(1L);
        when(normal.getMember()).thenReturn(host);
        when(normal.getContent()).thenReturn("안녕하세요!");
        when(normal.isReadByReceiver()).thenReturn(false);
        when(normal.getCreatedDate()).thenReturn(LocalDateTime.now());
        when(normal.getRoommateChattingRoom()).thenReturn(room);

        // 시스템 메시지 (member == null) — 예전 코드면 여기서 NPE
        RoommateChattingChat system = mock(RoommateChattingChat.class);
        when(system.getId()).thenReturn(2L);
        when(system.getMember()).thenReturn(null);
        when(system.getContent()).thenReturn("사용자1님이 나갔습니다.");
        when(system.isSystem()).thenReturn(true);
        when(system.isReadByReceiver()).thenReturn(true);
        when(system.getCreatedDate()).thenReturn(LocalDateTime.now());
        when(system.getRoommateChattingRoom()).thenReturn(room);

        when(chatRepository.findByRoommateChattingRoom(room)).thenReturn(List.of(normal, system));

        ImageLinkDto mockImage = mock(ImageLinkDto.class);
        when(mockImage.getImageUrl()).thenReturn("http://example.com/image.jpg");
        when(imageService.findImage(eq(ImageType.USER), anyLong(), any(HttpServletRequest.class)))
                .thenReturn(mockImage);

        List<ResponseRoommateChatDto> result =
                roommateChattingChatService.getChatList(2L, 100L, mock(HttpServletRequest.class));

        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(ResponseRoommateChatDto::isSystem);
    }


}
