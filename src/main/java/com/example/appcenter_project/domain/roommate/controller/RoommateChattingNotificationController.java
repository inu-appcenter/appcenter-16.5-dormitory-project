package com.example.appcenter_project.domain.roommate.controller;

import com.example.appcenter_project.domain.openChat.dto.request.RequestUpdateNotificationModeDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseNotificationModeDto;
import com.example.appcenter_project.domain.roommate.service.RoommateChattingNotificationService;
import com.example.appcenter_project.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/roommate-chatting-room")
public class RoommateChattingNotificationController implements RoommateChattingNotificationApiSpecification {

    private final RoommateChattingNotificationService roommateChattingNotificationService;

    @Override
    @PatchMapping("/{roomId}/notification-mode")
    public ResponseEntity<ResponseNotificationModeDto> updateNotificationMode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @RequestBody @Valid RequestUpdateNotificationModeDto dto) {
        ResponseNotificationModeDto result = roommateChattingNotificationService
                .updateNotificationMode(userDetails.getId(), roomId, dto.getMode());
        return ResponseEntity.ok(result);
    }

    @Override
    @GetMapping("/{roomId}/notification-mode")
    public ResponseEntity<ResponseNotificationModeDto> getNotificationMode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId) {
        ResponseNotificationModeDto result = roommateChattingNotificationService
                .getNotificationMode(userDetails.getId(), roomId);
        return ResponseEntity.ok(result);
    }
}
