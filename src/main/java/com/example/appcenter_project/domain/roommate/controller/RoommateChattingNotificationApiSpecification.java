package com.example.appcenter_project.domain.roommate.controller;

import com.example.appcenter_project.domain.openChat.dto.request.RequestUpdateNotificationModeDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseNotificationModeDto;
import com.example.appcenter_project.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "룸메이트 채팅방 알림 모드", description = "룸메이트 채팅방 알림 모드 변경/조회 API")
public interface RoommateChattingNotificationApiSpecification {

    @Operation(summary = "알림 모드 변경", description = "룸메이트 채팅방의 알림 모드를 변경합니다.")
    ResponseEntity<ResponseNotificationModeDto> updateNotificationMode(
            CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @RequestBody @Valid RequestUpdateNotificationModeDto dto);

    @Operation(summary = "알림 모드 조회", description = "룸메이트 채팅방의 현재 알림 모드를 조회합니다.")
    ResponseEntity<ResponseNotificationModeDto> getNotificationMode(
            CustomUserDetails userDetails,
            @PathVariable Long roomId);
}
