package com.example.appcenter_project.domain.openChat.fixture;

import com.example.appcenter_project.domain.openChat.dto.request.RequestCreateDerivedRoomDto;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomScope;
import org.springframework.test.util.ReflectionTestUtils;

public final class OpenChatScopeInheritFixture {

    private OpenChatScopeInheritFixture() {
    }

    public static OpenChatRoom createDerivedRoomWithScopeAndIsPublic(
            OpenChatRoomScope scope, boolean isPublic) {
        String dormitory = scope == OpenChatRoomScope.DORMITORY ? "SAMSUNG" : null;
        OpenChatRoom room = OpenChatRoom.createDerived(
                "파생 채팅방", "scope 상속 테스트", 10, 1L, null, isPublic, dormitory, scope);
        ReflectionTestUtils.setField(room, "id", 1L);
        return room;
    }

    public static OpenChatRoom createParentRoomWithScope(OpenChatRoomScope scope) {
        String dormitory = scope == OpenChatRoomScope.DORMITORY ? "SAMSUNG" : null;
        OpenChatRoom room = OpenChatRoom.createForTest(1L, "부모 채팅방");
        ReflectionTestUtils.setField(room, "scope", scope);
        if (dormitory != null) {
            ReflectionTestUtils.setField(room, "creatorDormitory", dormitory);
        }
        return room;
    }

    public static RequestCreateDerivedRoomDto createDerivedRoomRequest(boolean isPublic) {
        RequestCreateDerivedRoomDto dto = new RequestCreateDerivedRoomDto();
        ReflectionTestUtils.setField(dto, "originRoomId", 1L);
        ReflectionTestUtils.setField(dto, "name", "파생 채팅방");
        ReflectionTestUtils.setField(dto, "description", "테스트");
        ReflectionTestUtils.setField(dto, "maxParticipants", 10);
        ReflectionTestUtils.setField(dto, "isPublic", isPublic);
        return dto;
    }
}
