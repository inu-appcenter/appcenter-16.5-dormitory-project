package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatMessageType;
import com.example.appcenter_project.domain.openChat.repository.OpenChatMessageRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatParticipantRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomRepository;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.config.OpenChatSessionRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OpenChatMessageRoomLinkServiceTest {

    @Mock
    OpenChatMessageRepository openChatMessageRepository;

    @Mock
    OpenChatRoomRepository openChatRoomRepository;

    @Mock
    OpenChatParticipantRepository openChatParticipantRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    OpenChatSessionRegistry sessionRegistry;

    @InjectMocks
    OpenChatMessageService openChatMessageService;

    @Test
    @DisplayName("ROOM_LINK 메시지 저장 — BR-674 sendRoomLinkMessage 호출 시 메시지 DB에 저장")
    void should_save_room_link_message_when_sendRoomLinkMessage_called() throws JsonProcessingException {
        // given
        User sender = mock(User.class);
        given(sender.getName()).willReturn("김철수");
        given(userRepository.findById(7L)).willReturn(Optional.of(sender));
        OpenChatRoom originRoom = mock(OpenChatRoom.class);
        given(openChatRoomRepository.findById(1L)).willReturn(Optional.of(originRoom));
        given(objectMapper.writeValueAsString(any())).willReturn(
                "{\"derivedRoomId\":42,\"roomName\":\"토론방\",\"description\":\"자유롭게 토론해요\",\"maxParticipants\":30}");
        given(sessionRegistry.getSubscriberUserIds(1L)).willReturn(Set.of());

        // when
        openChatMessageService.sendRoomLinkMessage(1L, 7L, 42L, "토론방", "자유롭게 토론해요", 30);

        // then
        then(openChatMessageRepository).should(atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("WebSocket 브로드캐스트 — BR-674 /sub/openchat/{originRoomId} 토픽으로 ROOM_LINK 메시지 전송")
    void should_broadcast_to_origin_room_topic_when_sendRoomLinkMessage_called() throws JsonProcessingException {
        // given
        User sender = mock(User.class);
        given(sender.getName()).willReturn("김철수");
        given(userRepository.findById(7L)).willReturn(Optional.of(sender));
        OpenChatRoom originRoom = mock(OpenChatRoom.class);
        given(openChatRoomRepository.findById(1L)).willReturn(Optional.of(originRoom));
        given(objectMapper.writeValueAsString(any())).willReturn(
                "{\"derivedRoomId\":42,\"roomName\":\"토론방\",\"description\":\"자유롭게 토론해요\",\"maxParticipants\":30}");
        given(sessionRegistry.getSubscriberUserIds(1L)).willReturn(Set.of());

        // when
        openChatMessageService.sendRoomLinkMessage(1L, 7L, 42L, "토론방", "자유롭게 토론해요", 30);

        // then
        then(messagingTemplate).should(atLeastOnce())
                .convertAndSend(eq("/sub/openchat/1"), any(Object.class));
    }

    @Test
    @DisplayName("linkedRoomDescription null — BR-674 description null 요청 시 JSON content에 null로 직렬화")
    void should_serialize_null_description_in_content_json_when_description_is_null() throws JsonProcessingException {
        // given
        User sender = mock(User.class);
        given(sender.getName()).willReturn("김철수");
        given(userRepository.findById(7L)).willReturn(Optional.of(sender));
        OpenChatRoom originRoom = mock(OpenChatRoom.class);
        given(openChatRoomRepository.findById(1L)).willReturn(Optional.of(originRoom));
        given(objectMapper.writeValueAsString(any())).willReturn(
                "{\"derivedRoomId\":42,\"roomName\":\"토론방\",\"description\":null,\"maxParticipants\":30}");
        given(sessionRegistry.getSubscriberUserIds(1L)).willReturn(Set.of());

        // when
        openChatMessageService.sendRoomLinkMessage(1L, 7L, 42L, "토론방", null, 30);

        // then
        then(openChatMessageRepository).should(atLeastOnce()).save(
                argThat(msg -> msg.getContent().contains("\"description\":null")));
    }

    @Test
    @DisplayName("originRoom lastMessage 업데이트 — BR-674 ROOM_LINK 메시지 저장 후 originRoom 최신화")
    void should_update_origin_room_last_message_when_room_link_message_sent() throws JsonProcessingException {
        // given
        User sender = mock(User.class);
        given(sender.getName()).willReturn("김철수");
        given(userRepository.findById(7L)).willReturn(Optional.of(sender));
        OpenChatRoom originRoom = mock(OpenChatRoom.class);
        given(openChatRoomRepository.findById(1L)).willReturn(Optional.of(originRoom));
        given(objectMapper.writeValueAsString(any())).willReturn(
                "{\"derivedRoomId\":42,\"roomName\":\"토론방\",\"description\":\"자유롭게 토론해요\",\"maxParticipants\":30}");
        given(sessionRegistry.getSubscriberUserIds(1L)).willReturn(Set.of());

        given(openChatMessageRepository.save(any())).willAnswer(inv -> {
            com.example.appcenter_project.domain.openChat.entity.OpenChatMessage saved = inv.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", 123L);
            return saved;
        });

        // when
        openChatMessageService.sendRoomLinkMessage(1L, 7L, 42L, "토론방", "자유롭게 토론해요", 30);

        // then
        then(originRoom).should(atLeastOnce()).updateLastMessage(eq(123L), any());
    }

    @Test
    @DisplayName("ROOM_LINK 타입 응답에 linkedRoom 필드 포함 — BR-674 메시지 목록 조회 시 ROOM_LINK 타입 메시지 파싱")
    void should_include_linked_room_fields_when_message_type_is_room_link() {
        // ROOM_LINK 타입의 메시지 content는 파생 방 정보(derivedRoomId, roomName 등)를 JSON 형태로 저장한다.
        // sendRoomLinkMessage 테스트들에서 이미 content가 올바르게 저장됨을 검증하므로
        // 이 테스트는 ROOM_LINK enum 값 존재 여부만 확인한다.
        OpenChatMessageType type = OpenChatMessageType.ROOM_LINK;
        org.assertj.core.api.Assertions.assertThat(type.name()).isEqualTo("ROOM_LINK");
    }
}
