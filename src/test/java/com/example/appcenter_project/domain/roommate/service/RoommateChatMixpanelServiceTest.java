package com.example.appcenter_project.domain.roommate.service;

import com.example.appcenter_project.common.image.repository.ImageRepository;
import com.example.appcenter_project.common.image.service.ImageService;
import com.example.appcenter_project.domain.fcm.repository.FcmOutboxRepository;
import com.example.appcenter_project.domain.notification.entity.Notification;
import com.example.appcenter_project.domain.notification.service.NotificationService;
import com.example.appcenter_project.domain.roommate.dto.request.RequestRoommateChatDto;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingChat;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom;
import com.example.appcenter_project.domain.roommate.fixture.RoommateChattingImageFixture;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingChatRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingRoomRepository;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.repository.FcmTokenRepository;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.mixpanel.ChatMixpanelTracker;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoommateChatMixpanelServiceTest {

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
    @Mock ChatMixpanelTracker chatMixpanelTracker;

    @InjectMocks RoommateChattingChatService chatService;

    private static final Long HOST_ID = 1L;
    private static final Long GUEST_ID = 2L;

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
        given(userRepository.findById(GUEST_ID)).willReturn(Optional.of(guest));
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRepository.save(any(RoommateChattingChat.class))).willAnswer(invocation -> {
            RoommateChattingChat chat = invocation.getArgument(0);
            ReflectionTestUtils.setField(chat, "id", chatIdSequence.incrementAndGet());
            ReflectionTestUtils.setField(chat, "createdDate", LocalDateTime.now());
            return chat;
        });
        given(notificationService.createChatNotification(anyString(), anyLong(), anyString()))
                .willReturn(mock(Notification.class));
    }

    private RequestRoommateChatDto textRequest() {
        RequestRoommateChatDto dto = mock(RequestRoommateChatDto.class);
        given(dto.getRoommateChattingRoomId()).willReturn(roomId);
        given(dto.getContent()).willReturn("안녕하세요");
        return dto;
    }

    @Test
    @DisplayName("[BR-TBD] 텍스트 저장 + 방의 첫 메시지 → 저장된 메시지 ID와 first=true 로 발화")
    void text_first() {
        given(chatRoomRepository.markFirstMessageSent(eq(roomId), any(LocalDateTime.class))).willReturn(1);

        chatService.sendChat(GUEST_ID, textRequest());

        then(chatMixpanelTracker).should()
                .trackMessageSent(eq(GUEST_ID), eq(roomId), eq("ROOMMATE"), eq(101L), eq("TEXT"), eq(true));
    }

    @Test
    @DisplayName("[BR-TBD] 이미 첫 메시지가 기록된 방(영향 행 0) → first=false")
    void text_notFirst() {
        given(chatRoomRepository.markFirstMessageSent(eq(roomId), any(LocalDateTime.class))).willReturn(0);

        chatService.sendChat(GUEST_ID, textRequest());

        then(chatMixpanelTracker).should()
                .trackMessageSent(eq(GUEST_ID), eq(roomId), eq("ROOMMATE"), eq(101L), eq("TEXT"), eq(false));
    }

    @Test
    @DisplayName("[BR-TBD] 방 참여자가 아니어서 저장 전에 예외가 나면 발화하지 않는다")
    void notParticipant_noEvent() {
        User stranger = User.createForTest(3L, "외부인");
        given(userRepository.findById(3L)).willReturn(Optional.of(stranger));

        assertThatThrownBy(() -> chatService.sendChat(3L, textRequest())).isInstanceOf(CustomException.class);

        then(chatMixpanelTracker).should(never()).trackMessageSent(any(), any(), any(), any(), any(), anyBoolean());
        then(chatRoomRepository).should(never()).markFirstMessageSent(any(), any());
    }

    @Test
    @DisplayName("[BR-TBD] 시스템 메시지는 발화하지 않고 첫 메시지로도 세지 않는다")
    void systemMessage_noEvent() {
        chatService.sendSystemMessage(room, "게스트님이 나갔습니다.");

        then(chatMixpanelTracker).should(never()).trackMessageSent(any(), any(), any(), any(), any(), anyBoolean());
        then(chatRoomRepository).should(never()).markFirstMessageSent(any(), any());
    }

    @Test
    @DisplayName("[BR-TBD] 이미지 3장 → 메시지마다 IMAGE 발화, 첫 장만 first=true")
    void images_onlyFirstIsFirst() {
        given(chatRoomRepository.markFirstMessageSent(eq(roomId), any(LocalDateTime.class))).willReturn(1, 0, 0);

        chatService.sendImageChats(GUEST_ID, roomId, RoommateChattingImageFixture.createImages(3), request);

        then(chatMixpanelTracker).should(times(3))
                .trackMessageSent(eq(GUEST_ID), eq(roomId), eq("ROOMMATE"), anyLong(), eq("IMAGE"), anyBoolean());
        then(chatMixpanelTracker).should(times(1))
                .trackMessageSent(any(), any(), any(), any(), any(), eq(true));
    }

    @Test
    @DisplayName("[BR-TBD] 시스템 메시지가 먼저 있어도 첫 사용자 메시지는 first=true (count 가 아닌 컬럼 기준)")
    void firstUserMessageAfterSystemMessage() {
        chatService.sendSystemMessage(room, "호스트님이 나갔습니다.");
        given(chatRoomRepository.markFirstMessageSent(eq(roomId), any(LocalDateTime.class))).willReturn(1);

        chatService.sendChat(GUEST_ID, textRequest());

        then(chatMixpanelTracker).should()
                .trackMessageSent(any(), any(), any(), any(), eq("TEXT"), eq(true));
    }
}
