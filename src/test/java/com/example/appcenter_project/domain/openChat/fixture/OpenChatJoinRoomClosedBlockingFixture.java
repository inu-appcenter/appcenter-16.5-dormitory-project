package com.example.appcenter_project.domain.openChat.fixture;

import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomRecruitmentStatus;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomScope;

public class OpenChatJoinRoomClosedBlockingFixture {

    public static OpenChatRoom createOpenDerivedRoom(Long createdBy) {
        return OpenChatRoom.createDerived("파생 채팅방", "설명", 10, createdBy, null, true, null);
    }

    public static OpenChatRoom createClosedDerivedRoom(Long createdBy) {
        OpenChatRoom room = createOpenDerivedRoom(createdBy);
        room.updateRecruitmentStatus(OpenChatRoomRecruitmentStatus.CLOSED, createdBy);
        return room;
    }

    public static OpenChatRoom createReopenedDerivedRoom(Long createdBy) {
        OpenChatRoom room = createClosedDerivedRoom(createdBy);
        room.updateRecruitmentStatus(OpenChatRoomRecruitmentStatus.OPEN, createdBy);
        return room;
    }

    public static OpenChatRoom createOpenTypeRoom(Long createdBy) {
        return OpenChatRoom.create("OPEN 타입 방", "설명", OpenChatRoomScope.ALL, 10, createdBy, null, false);
    }
}
