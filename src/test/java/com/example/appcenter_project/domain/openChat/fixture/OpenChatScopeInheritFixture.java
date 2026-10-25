package com.example.appcenter_project.domain.openChat.fixture;

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
}
