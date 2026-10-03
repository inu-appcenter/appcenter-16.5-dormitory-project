package com.example.appcenter_project.domain.openChat.fixture;

import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomRecruitmentStatus;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomScope;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.enums.DormType;
import com.example.appcenter_project.domain.user.enums.Role;

import java.lang.reflect.Field;

public class OpenChatRecruitmentStatusFixture {

    public static OpenChatRoom createDerivedRoom(Long createdBy) {
        return OpenChatRoom.createDerived("파생 채팅방", "설명", 10, createdBy, null, true, null);
    }

    public static OpenChatRoom createDerivedRoomClosed(Long createdBy) {
        OpenChatRoom room = createDerivedRoom(createdBy);
        room.updateRecruitmentStatus(OpenChatRoomRecruitmentStatus.CLOSED, createdBy);
        return room;
    }

    public static OpenChatRoom createOpenTypeRoom(Long createdBy) {
        return OpenChatRoom.create("OPEN 타입 방", "설명", OpenChatRoomScope.ALL, 10, createdBy, null, false);
    }

    public static User createOwner() {
        return User.createForTest(1L, "owner", DormType.DORM_1);
    }

    public static User createAdmin() {
        User admin = User.createForTest(99L, "admin", DormType.DORM_1);
        setField(admin, "role", Role.ROLE_ADMIN);
        return admin;
    }

    public static User createNonOwnerUser() {
        return User.createForTest(2L, "nonOwner", DormType.DORM_1);
    }

    public static Object createRequestWithStatus(String statusValue) {
        try {
            Class<?> dtoClass = Class.forName(
                    "com.example.appcenter_project.domain.openChat.dto.request.RequestUpdateRecruitmentStatusDto");
            Object dto = dtoClass.getDeclaredConstructor().newInstance();
            if (statusValue != null) {
                Class<?> enumClass = Class.forName(
                        "com.example.appcenter_project.domain.openChat.enums.OpenChatRoomRecruitmentStatus");
                Object statusEnum = Enum.valueOf((Class<Enum>) enumClass, statusValue);
                setField(dto, "status", statusEnum);
            }
            return dto;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> clazz = target.getClass();
            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (NoSuchFieldException ignored) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
