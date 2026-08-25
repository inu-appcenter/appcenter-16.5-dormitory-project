package com.example.appcenter_project.domain.openChat.fixture;

import com.example.appcenter_project.domain.openChat.entity.OpenChatMessage;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatMessageType;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomType;
import com.example.appcenter_project.domain.user.entity.User;

import java.util.List;

public class AdminBotMessageFixture {

    public static final Long ADMIN_ID = 1L;
    public static final Long OPEN_ROOM_ID = 10L;
    public static final Long PERSONAL_ROOM_ID = 20L;
    public static final String BOT_CONTENT = "기숙사 공지입니다.";

    public static OpenChatRoom openRoom() {
        return OpenChatRoom.createForTest(OPEN_ROOM_ID, "1기숙사 자유채팅", OpenChatRoomType.OPEN);
    }

    public static OpenChatRoom openRoom(Long id, String name) {
        return OpenChatRoom.createForTest(id, name, OpenChatRoomType.OPEN);
    }

    public static OpenChatRoom personalRoom() {
        return OpenChatRoom.createForTest(PERSONAL_ROOM_ID, "개인 채팅방", OpenChatRoomType.PERSONAL);
    }

    public static OpenChatRoom derivedRoom(Long id, String name) {
        return OpenChatRoom.createForTest(id, name, OpenChatRoomType.DERIVED);
    }

    public static User admin() {
        return User.createForTest(ADMIN_ID, "관리자");
    }

    public static OpenChatMessage botMessage(Long roomId, Long senderId) {
        return OpenChatMessage.create(roomId, senderId, BOT_CONTENT, OpenChatMessageType.BOT);
    }

    public static List<OpenChatRoom> threeOpenRooms() {
        return List.of(
                openRoom(1L, "1기숙사 자유채팅"),
                openRoom(2L, "기숙사 공지방"),
                openRoom(3L, "자유게시판 채팅")
        );
    }
}
