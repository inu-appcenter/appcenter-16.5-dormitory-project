package com.example.appcenter_project.domain.roommate.fixture;

import com.example.appcenter_project.domain.fcm.entity.FcmToken;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.enums.DormType;

public class RoommateChattingNotificationFixture {

    public static User createUser(Long id) {
        return User.createForTest(id, "user-" + id, DormType.DORM_1);
    }

    public static RoommateChattingRoom createRoom(Long hostId, Long guestId) {
        User host = createUser(hostId);
        User guest = createUser(guestId);
        return RoommateChattingRoom.create(host, guest);
    }

    public static FcmToken createFcmToken(User user, String tokenValue) {
        return FcmToken.builder().user(user).token(tokenValue).build();
    }
}
