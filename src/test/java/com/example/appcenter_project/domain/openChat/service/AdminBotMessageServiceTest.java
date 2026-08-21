package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.openChat.entity.OpenChatMessage;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatMessageType;
import com.example.appcenter_project.domain.openChat.fixture.AdminBotMessageFixture;
import com.example.appcenter_project.domain.openChat.repository.OpenChatMessageRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatParticipantRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomRepository;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.config.OpenChatSessionRegistry;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.exception.ErrorCode;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBotMessageServiceTest {

    @Mock
    OpenChatRoomRepository openChatRoomRepository;

    @Mock
    OpenChatParticipantRepository openChatParticipantRepository;

    @Mock
    OpenChatMessageRepository openChatMessageRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @Mock
    OpenChatSessionRegistry sessionRegistry;

    @Mock
    OpenChatNotificationService openChatNotificationService;

    @InjectMocks
    OpenChatMessageService openChatMessageService;

    // =====================================================================
    // sendBotMessage — 정상
    // =====================================================================

    @Test
    @DisplayName("메시지 저장 — AC-2 BOT 타입으로 OpenChatMessage 저장")
    void should_save_message_with_BOT_type_when_valid_request() {
        // given
        OpenChatRoom room = AdminBotMessageFixture.openRoom();
        User admin = AdminBotMessageFixture.admin();
        given(openChatRoomRepository.findById(AdminBotMessageFixture.OPEN_ROOM_ID)).willReturn(Optional.of(room));
        given(userRepository.findById(AdminBotMessageFixture.ADMIN_ID)).willReturn(Optional.of(admin));
        given(openChatMessageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(openChatParticipantRepository.countByRoomId(any())).willReturn(0L);
        given(openChatParticipantRepository.countReadByRoomIdAndMessageId(any(), any())).willReturn(0L);
        given(sessionRegistry.getSubscriberUserIds(any())).willReturn(Set.of());
        ArgumentCaptor<OpenChatMessage> captor = ArgumentCaptor.forClass(OpenChatMessage.class);

        // when
        openChatMessageService.sendBotMessage(AdminBotMessageFixture.ADMIN_ID, AdminBotMessageFixture.OPEN_ROOM_ID, AdminBotMessageFixture.BOT_CONTENT);

        // then
        then(openChatMessageRepository).should().save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(OpenChatMessageType.BOT);
    }

    @Test
    @DisplayName("senderId = 어드민 ID — AC-2 저장된 메시지의 senderId가 어드민 id")
    void should_save_message_with_admin_as_sender() {
        // given
        OpenChatRoom room = AdminBotMessageFixture.openRoom();
        User admin = AdminBotMessageFixture.admin();
        given(openChatRoomRepository.findById(AdminBotMessageFixture.OPEN_ROOM_ID)).willReturn(Optional.of(room));
        given(userRepository.findById(AdminBotMessageFixture.ADMIN_ID)).willReturn(Optional.of(admin));
        given(openChatMessageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(openChatParticipantRepository.countByRoomId(any())).willReturn(0L);
        given(openChatParticipantRepository.countReadByRoomIdAndMessageId(any(), any())).willReturn(0L);
        given(sessionRegistry.getSubscriberUserIds(any())).willReturn(Set.of());
        ArgumentCaptor<OpenChatMessage> captor = ArgumentCaptor.forClass(OpenChatMessage.class);

        // when
        openChatMessageService.sendBotMessage(AdminBotMessageFixture.ADMIN_ID, AdminBotMessageFixture.OPEN_ROOM_ID, AdminBotMessageFixture.BOT_CONTENT);

        // then
        then(openChatMessageRepository).should().save(captor.capture());
        assertThat(captor.getValue().getSenderId()).isEqualTo(AdminBotMessageFixture.ADMIN_ID);
    }

    @Test
    @DisplayName("lastMessage 갱신 — AC-3 room.updateLastMessage() 호출")
    void should_update_room_last_message_after_sending() {
        // given
        OpenChatRoom room = AdminBotMessageFixture.openRoom();
        User admin = AdminBotMessageFixture.admin();
        given(openChatRoomRepository.findById(AdminBotMessageFixture.OPEN_ROOM_ID)).willReturn(Optional.of(room));
        given(userRepository.findById(AdminBotMessageFixture.ADMIN_ID)).willReturn(Optional.of(admin));
        given(openChatMessageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(openChatParticipantRepository.countByRoomId(any())).willReturn(0L);
        given(openChatParticipantRepository.countReadByRoomIdAndMessageId(any(), any())).willReturn(0L);
        given(sessionRegistry.getSubscriberUserIds(any())).willReturn(Set.of());

        // when
        openChatMessageService.sendBotMessage(AdminBotMessageFixture.ADMIN_ID, AdminBotMessageFixture.OPEN_ROOM_ID, AdminBotMessageFixture.BOT_CONTENT);

        // then
        assertThat(room.getLastMessage()).isEqualTo(AdminBotMessageFixture.BOT_CONTENT);
    }

    @Test
    @DisplayName("WebSocket 브로드캐스트 — AC-2 /sub/openchat/{roomId} 채널로 전송")
    void should_broadcast_to_websocket_topic_after_sending() {
        // given
        OpenChatRoom room = AdminBotMessageFixture.openRoom();
        User admin = AdminBotMessageFixture.admin();
        given(openChatRoomRepository.findById(AdminBotMessageFixture.OPEN_ROOM_ID)).willReturn(Optional.of(room));
        given(userRepository.findById(AdminBotMessageFixture.ADMIN_ID)).willReturn(Optional.of(admin));
        given(openChatMessageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(openChatParticipantRepository.countByRoomId(any())).willReturn(0L);
        given(openChatParticipantRepository.countReadByRoomIdAndMessageId(any(), any())).willReturn(0L);
        given(sessionRegistry.getSubscriberUserIds(any())).willReturn(Set.of());

        // when
        openChatMessageService.sendBotMessage(AdminBotMessageFixture.ADMIN_ID, AdminBotMessageFixture.OPEN_ROOM_ID, AdminBotMessageFixture.BOT_CONTENT);

        // then
        then(messagingTemplate).should().convertAndSend(
                eq("/sub/openchat/" + AdminBotMessageFixture.OPEN_ROOM_ID), any(Object.class));
    }

    // =====================================================================
    // sendBotMessage — 예외
    // =====================================================================

    @Test
    @DisplayName("OPEN_CHAT_ROOM_NOT_FOUND — AC-5 없는 roomId 전달 시 404 예외")
    void should_throw_OPEN_CHAT_ROOM_NOT_FOUND_when_room_not_exists() {
        // given
        given(openChatRoomRepository.findById(999L)).willReturn(Optional.empty());

        // when
        ThrowingCallable action = () -> openChatMessageService.sendBotMessage(
                AdminBotMessageFixture.ADMIN_ID, 999L, AdminBotMessageFixture.BOT_CONTENT);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("OPEN_CHAT_BOT_TARGET_PERSONAL — AC-4 PERSONAL 방 전송 시 400 예외")
    void should_throw_OPEN_CHAT_BOT_TARGET_PERSONAL_when_room_is_personal() {
        // given
        OpenChatRoom personalRoom = AdminBotMessageFixture.personalRoom();
        given(openChatRoomRepository.findById(AdminBotMessageFixture.PERSONAL_ROOM_ID)).willReturn(Optional.of(personalRoom));

        // when
        ThrowingCallable action = () -> openChatMessageService.sendBotMessage(
                AdminBotMessageFixture.ADMIN_ID, AdminBotMessageFixture.PERSONAL_ROOM_ID, AdminBotMessageFixture.BOT_CONTENT);

        // then
        assertThatThrownBy(action)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OPEN_CHAT_BOT_TARGET_PERSONAL);
    }

    @Test
    @DisplayName("메시지 저장 미호출 — PERSONAL 방 예외 시 save 호출하지 않음")
    void should_not_save_message_when_room_is_personal() {
        // given
        OpenChatRoom personalRoom = AdminBotMessageFixture.personalRoom();
        given(openChatRoomRepository.findById(AdminBotMessageFixture.PERSONAL_ROOM_ID)).willReturn(Optional.of(personalRoom));

        // when
        ThrowingCallable action = () -> openChatMessageService.sendBotMessage(
                AdminBotMessageFixture.ADMIN_ID, AdminBotMessageFixture.PERSONAL_ROOM_ID, AdminBotMessageFixture.BOT_CONTENT);

        // then
        assertThatThrownBy(action).isInstanceOf(CustomException.class);
        then(openChatMessageRepository).should(never()).save(any());
    }

    // =====================================================================
    // getAdminChatRooms — 오픈채팅방 목록 조회
    // =====================================================================

    @Test
    @DisplayName("PERSONAL 제외 3개 반환 — AC-1 PERSONAL 방 제외한 채팅방 목록 반환")
    void should_return_rooms_excluding_personal() {
        // given
        var rooms = AdminBotMessageFixture.threeOpenRooms();
        given(openChatRoomRepository.findByRoomTypeNot(any())).willReturn(rooms);

        // when
        var result = openChatMessageService.getAdminChatRooms();

        // then
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("roomId 포함 확인 — AC-1 각 항목에 roomId 포함")
    void should_include_roomId_in_each_item() {
        // given
        var rooms = AdminBotMessageFixture.threeOpenRooms();
        given(openChatRoomRepository.findByRoomTypeNot(any())).willReturn(rooms);

        // when
        var result = openChatMessageService.getAdminChatRooms();

        // then
        assertThat(result).allSatisfy(dto -> assertThat(dto.getRoomId()).isNotNull());
    }

    @Test
    @DisplayName("roomName 포함 확인 — AC-1 각 항목에 roomName 포함")
    void should_include_roomName_in_each_item() {
        // given
        var rooms = AdminBotMessageFixture.threeOpenRooms();
        given(openChatRoomRepository.findByRoomTypeNot(any())).willReturn(rooms);

        // when
        var result = openChatMessageService.getAdminChatRooms();

        // then
        assertThat(result).allSatisfy(dto -> assertThat(dto.getRoomName()).isNotBlank());
    }

    @Test
    @DisplayName("PERSONAL 타입으로 조회 — findByRoomTypeNot에 PERSONAL 전달")
    void should_query_with_PERSONAL_excluded() {
        // given
        given(openChatRoomRepository.findByRoomTypeNot(any())).willReturn(java.util.List.of());

        // when
        openChatMessageService.getAdminChatRooms();

        // then
        then(openChatRoomRepository).should().findByRoomTypeNot(
                com.example.appcenter_project.domain.openChat.enums.OpenChatRoomType.PERSONAL);
    }
}
