package com.example.appcenter_project.domain.feature.dto.response;

import com.example.appcenter_project.domain.feature.enums.AbGroup;
import lombok.Getter;

@Getter
public class ResponseAbGroupDto {

    private final AbGroup group;

    private ResponseAbGroupDto(AbGroup group) {
        this.group = group;
    }

    public static ResponseAbGroupDto of(AbGroup group) {
        return new ResponseAbGroupDto(group);
    }
}
