package com.example.appcenter_project.domain.openChat.fixture;

import com.example.appcenter_project.domain.openChat.dto.request.RequestCreateOpenChatRoomDto;
import com.example.appcenter_project.domain.openChat.dto.request.RequestCreatePersonalRoomDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseLeaveOpenChatRoomDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatRoomDetailDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatRoomDto;
import com.example.appcenter_project.domain.openChat.entity.OpenChatParticipant;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomScope;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.enums.DormType;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

public class OpenChatRoomFixture {

    public static OpenChatRoom createRoom() {
        return OpenChatRoom.create("테스트 채팅방", "설명", OpenChatRoomScope.ALL, 10, 1L, null, false);
    }

    public static OpenChatRoom createRoomWithScope(OpenChatRoomScope scope) {
        String dormitory = scope == OpenChatRoomScope.DORMITORY ? "SAMSUNG" : null;
        return OpenChatRoom.create("테스트 채팅방", "설명", scope, 10, 1L, dormitory, false);
    }

    public static OpenChatRoom createOfficialRoom() {
        return OpenChatRoom.create("공식 방", "설명", OpenChatRoomScope.ALL, 100, null, null, true);
    }

    public static OpenChatRoom createFullRoom() {
        return OpenChatRoom.create("꽉 찬 방", "설명", OpenChatRoomScope.ALL, 2, 1L, null, false);
    }

    public static OpenChatRoom createRoomWithLastMessageAt(LocalDateTime lastMessageAt) {
        OpenChatRoom room = OpenChatRoom.create("테스트 채팅방", "설명", OpenChatRoomScope.ALL, 10, 1L, null, false);
        room.updateLastMessage(1L, lastMessageAt);
        return room;
    }

    public static OpenChatRoom createPrivateRoomWithPassword() {
        return OpenChatRoom.create("비공개 채팅방", "설명", OpenChatRoomScope.ALL, 10, 1L, null, false, "1234", false);
    }

    public static OpenChatRoom createRoomWithId(Long id) {
        OpenChatRoom room = Mockito.mock(OpenChatRoom.class);
        Mockito.when(room.getId()).thenReturn(id);
        return room;
    }

    public static OpenChatRoom createPersonalRoomWithId(Long id) {
        OpenChatRoom room = Mockito.mock(OpenChatRoom.class);
        Mockito.when(room.getId()).thenReturn(id);
        return room;
    }

    public static OpenChatRoom createPersonalRoomWithPassword(String password) {
        return OpenChatRoom.createPersonal("개인 채팅방", 1L, password);
    }

    public static OpenChatParticipant createParticipant(Long userId) {
        return OpenChatParticipant.create(1L, userId, LocalDateTime.now());
    }

    public static OpenChatParticipant createParticipantWithJoinedAt(Long userId, LocalDateTime joinedAt) {
        return OpenChatParticipant.create(1L, userId, joinedAt);
    }

    public static RequestCreateOpenChatRoomDto createRequest() {
        return RequestCreateOpenChatRoomDto.builder()
                .name("테스트 채팅방")
                .description("설명")
                .scope(OpenChatRoomScope.ALL)
                .maxParticipants(10)
                .build();
    }

    public static RequestCreateOpenChatRoomDto createRequestWithPasswordAndPrivate() {
        return RequestCreateOpenChatRoomDto.builder()
                .name("테스트 채팅방")
                .description("설명")
                .scope(OpenChatRoomScope.ALL)
                .maxParticipants(10)
                .password("1234")
                .isPublic(false)
                .build();
    }

    public static RequestCreateOpenChatRoomDto createRequestWithBlankPassword() {
        return RequestCreateOpenChatRoomDto.builder()
                .name("테스트 채팅방")
                .description("설명")
                .scope(OpenChatRoomScope.ALL)
                .maxParticipants(10)
                .password("")
                .build();
    }

    public static RequestCreateOpenChatRoomDto createRequestWithDormitoryScope() {
        return RequestCreateOpenChatRoomDto.builder()
                .name("테스트 채팅방")
                .description("설명")
                .scope(OpenChatRoomScope.DORMITORY)
                .maxParticipants(10)
                .build();
    }

    public static RequestCreateOpenChatRoomDto createRequestWithBlankName() {
        return RequestCreateOpenChatRoomDto.builder()
                .name("   ")
                .description("설명")
                .scope(OpenChatRoomScope.ALL)
                .maxParticipants(10)
                .build();
    }

    public static RequestCreateOpenChatRoomDto createRequestWithLongName() {
        return RequestCreateOpenChatRoomDto.builder()
                .name("a".repeat(31))
                .description("설명")
                .scope(OpenChatRoomScope.ALL)
                .maxParticipants(10)
                .build();
    }

    public static RequestCreateOpenChatRoomDto createRequestWithLongDescription() {
        return RequestCreateOpenChatRoomDto.builder()
                .name("테스트 채팅방")
                .description("a".repeat(101))
                .scope(OpenChatRoomScope.ALL)
                .maxParticipants(10)
                .build();
    }

    public static RequestCreateOpenChatRoomDto createRequestWithMinParticipants() {
        return RequestCreateOpenChatRoomDto.builder()
                .name("테스트 채팅방")
                .description("설명")
                .scope(OpenChatRoomScope.ALL)
                .maxParticipants(1)
                .build();
    }

    public static RequestCreateOpenChatRoomDto createRequestWithMaxParticipantsExceeded() {
        return RequestCreateOpenChatRoomDto.builder()
                .name("테스트 채팅방")
                .description("설명")
                .scope(OpenChatRoomScope.ALL)
                .maxParticipants(101)
                .build();
    }

    public static RequestCreateOpenChatRoomDto createRequestWithNullScope() {
        return RequestCreateOpenChatRoomDto.builder()
                .name("테스트 채팅방")
                .description("설명")
                .scope(null)
                .maxParticipants(10)
                .build();
    }

    public static RequestCreatePersonalRoomDto createPersonalRoomRequest(Long targetUserId) {
        return RequestCreatePersonalRoomDto.builder()
                .name("개인 채팅방")
                .targetUserId(targetUserId)
                .build();
    }

    public static RequestCreatePersonalRoomDto createPersonalRoomRequestWithPassword(Long targetUserId, String password) {
        return RequestCreatePersonalRoomDto.builder()
                .name("비밀 채팅방")
                .targetUserId(targetUserId)
                .password(password)
                .build();
    }

    public static User createUserWithId(Long id) {
        return User.createForTest(id, "user-" + id, DormType.DORM_1);
    }

    public static ResponseOpenChatRoomDetailDto createDetailResponse() {
        return ResponseOpenChatRoomDetailDto.builder()
                .roomId(1L)
                .name("테스트 채팅방")
                .description("설명")
                .scope(OpenChatRoomScope.ALL)
                .currentParticipants(1)
                .maxParticipants(10)
                .isOfficial(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static ResponseOpenChatRoomDto createRoomResponse() {
        return ResponseOpenChatRoomDto.builder()
                .roomId(1L)
                .name("테스트 채팅방")
                .description("설명")
                .scope(OpenChatRoomScope.ALL)
                .currentParticipants(1)
                .maxParticipants(10)
                .isJoined(false)
                .lastMessageAt(null)
                .lastMessage(null)
                .unreadCount(0)
                .build();
    }

    public static ResponseLeaveOpenChatRoomDto createLeaveResponse(boolean roomDeleted) {
        return ResponseLeaveOpenChatRoomDto.builder()
                .roomDeleted(roomDeleted)
                .build();
    }

    public static List<ResponseOpenChatRoomDto> createRoomResponseList() {
        return List.of(createRoomResponse());
    }
}
