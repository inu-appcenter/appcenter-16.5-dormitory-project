package com.example.appcenter_project.domain.openChat.fixture;

import com.example.appcenter_project.domain.openChat.entity.OpenChatMessage;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatMessageType;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomRecruitmentStatus;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomScope;

import java.lang.reflect.Field;

public class OpenChatParentCardFixture {

    // ────────────────────────────────────────────────────────────
    // OpenChatRoom
    // ────────────────────────────────────────────────────────────

    public static OpenChatRoom createOpenDerivedRoom(Long id) {
        OpenChatRoom room = OpenChatRoom.createDerived("파생 채팅방", "설명", 30, 1L, null, true, null);
        setEntityField(room, "id", id);
        return room;
    }

    public static OpenChatRoom createClosedDerivedRoom(Long id) {
        OpenChatRoom room = createOpenDerivedRoom(id);
        room.updateRecruitmentStatus(OpenChatRoomRecruitmentStatus.CLOSED, 1L);
        return room;
    }

    /**
     * originRoomId를 가진 OPEN DERIVED 방.
     * ★ BR-764 RED: OpenChatRoom에 originRoomId 필드 미구현.
     *   필드 추가 후 setEntityField 라인 주석 해제.
     */
    public static OpenChatRoom createDerivedRoomWithOrigin(Long id, Long originRoomId) {
        OpenChatRoom room = createOpenDerivedRoom(id);
        setEntityField(room, "originRoomId", originRoomId);
        return room;
    }

    /**
     * originRoomId를 가진 CLOSED DERIVED 방.
     * ★ BR-764 RED: OpenChatRoom에 originRoomId 필드 미구현.
     */
    public static OpenChatRoom createClosedDerivedRoomWithOrigin(Long id, Long originRoomId) {
        OpenChatRoom room = createClosedDerivedRoom(id);
        setEntityField(room, "originRoomId", originRoomId);
        return room;
    }

    public static OpenChatRoom createParentOpenRoom(Long id) {
        OpenChatRoom room = OpenChatRoom.create(
                "부모 채팅방", "설명", OpenChatRoomScope.ALL, 50, 1L, null, false);
        setEntityField(room, "id", id);
        return room;
    }

    // ────────────────────────────────────────────────────────────
    // OpenChatMessage — ROOM_LINK 타입
    // ────────────────────────────────────────────────────────────

    public static OpenChatMessage createRoomLinkMessage(Long roomId, Long senderId, Long derivedRoomId,
                                                        String roomName, String description,
                                                        int maxParticipants) {
        String content = String.format(
                "{\"derivedRoomId\":%d,\"roomName\":\"%s\",\"description\":\"%s\",\"maxParticipants\":%d}",
                derivedRoomId, roomName, description, maxParticipants);
        return OpenChatMessage.create(roomId, senderId, content, OpenChatMessageType.ROOM_LINK);
    }

    public static OpenChatMessage createRoomLinkMessageWithNullDescription(Long roomId, Long senderId,
                                                                            Long derivedRoomId,
                                                                            String roomName) {
        String content = String.format(
                "{\"derivedRoomId\":%d,\"roomName\":\"%s\",\"description\":null,\"maxParticipants\":30}",
                derivedRoomId, roomName);
        return OpenChatMessage.create(roomId, senderId, content, OpenChatMessageType.ROOM_LINK);
    }

    public static OpenChatMessage createRoomLinkMessageWithMalformedContent(Long roomId, Long senderId) {
        return OpenChatMessage.create(roomId, senderId, "not-valid-json", OpenChatMessageType.ROOM_LINK);
    }

    /**
     * ★ BR-764 RED: REOPEN_CARD 타입 메시지 생성.
     *   OpenChatMessageType.REOPEN_CARD 미구현 — 추가 후 아래 주석 해제.
     */
    public static OpenChatMessage createReopenCardMessage(Long roomId, Long senderId, Long derivedRoomId,
                                                          String roomName, String duplKey) {
        String content = String.format(
                "{\"derivedRoomId\":%d,\"roomName\":\"%s\",\"description\":\"설명\",\"maxParticipants\":30}",
                derivedRoomId, roomName);
        return OpenChatMessage.createReopenCard(roomId, senderId, content, duplKey);
    }

    // ────────────────────────────────────────────────────────────
    // Private helpers
    // ────────────────────────────────────────────────────────────

    private static void setEntityField(Object target, String fieldName, Object value) {
        try {
            Class<?> clazz = target.getClass();
            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
