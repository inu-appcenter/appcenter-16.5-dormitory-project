package com.example.appcenter_project.domain.feature.dto.response;

import com.example.appcenter_project.domain.feature.entity.Feature;
import com.example.appcenter_project.domain.feature.enums.AbGroup;
import com.example.appcenter_project.domain.user.entity.User;
import lombok.Getter;

import java.time.Instant;

@Getter
public class ResponseAbGroupDto {

    private final AbGroup group;
    private final String experimentId;
    private final String userId;
    private final String userType;
    private final String timestamp;

    private ResponseAbGroupDto(AbGroup group, String experimentId, String userId, String userType) {
        this.group = group;
        this.experimentId = experimentId;
        this.userId = userId;
        this.userType = userType;
        this.timestamp = Instant.now().toString();
    }

    public static ResponseAbGroupDto of(AbGroup group, Feature feature, User user) {
        String userType = null;
        if (feature.getCreatedDate() != null) {
            userType = user.getCreatedDate().isBefore(feature.getCreatedDate()) ? "existing" : "new";
        }
        return new ResponseAbGroupDto(group, feature.getKey(), String.valueOf(user.getId()), userType);
    }

    public static ResponseAbGroupDto ofOff(String experimentId, Long userId) {
        return new ResponseAbGroupDto(AbGroup.OFF, experimentId, String.valueOf(userId), null);
    }
}
