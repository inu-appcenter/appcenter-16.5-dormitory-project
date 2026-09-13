package com.example.appcenter_project.domain.fcm.controller;

import com.example.appcenter_project.common.metrics.annotation.TrackApi;
import com.example.appcenter_project.domain.fcm.dto.request.RequestFcmMessageDto;
import com.example.appcenter_project.domain.fcm.dto.request.RequestFcmTokenDto;
import com.example.appcenter_project.domain.fcm.dto.response.ResponseFcmDlqDto;
import com.example.appcenter_project.domain.fcm.dto.response.ResponseFcmMessageDto;
import com.example.appcenter_project.domain.fcm.dto.response.ResponseFcmStatsDto;
import com.example.appcenter_project.global.security.CustomUserDetails;
import com.example.appcenter_project.domain.fcm.service.FcmMessageService;
import com.example.appcenter_project.domain.fcm.service.FcmTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/fcm")
@RequiredArgsConstructor
public class FcmController implements FcmApiSpecification{

    private final FcmTokenService fcmTokenService;
    private final FcmMessageService fcmMessageService;

    @PostMapping("/token")
    public ResponseEntity<Void> saveToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody RequestFcmTokenDto requestDto
    ) {
        fcmTokenService.saveToken(userDetails, requestDto.getFcmToken());
        return ResponseEntity.ok().build();
    }

    @TrackApi
    @GetMapping("/stats")
    public ResponseEntity<ResponseFcmStatsDto> getFcmStats() {
        return ResponseEntity.ok(fcmMessageService.getFcmStats());
    }

    @DeleteMapping("/token")
    public ResponseEntity<Void> unlinkToken(
            @RequestBody RequestFcmTokenDto requestDto
    ) {
        fcmTokenService.unlinkToken(requestDto.getFcmToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send/all")
    public ResponseEntity<ResponseFcmMessageDto> sendMessageToAllUsers(
            @RequestBody RequestFcmMessageDto requestDto
    ) {
        ResponseFcmMessageDto response = fcmMessageService.sendNotificationToAllUsers(
                requestDto.getTitle(),
                requestDto.getBody()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dlq")
    public ResponseEntity<Page<ResponseFcmDlqDto>> getDlqList(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(fcmMessageService.getDlqList(pageable));
    }

    @PostMapping("/dlq/{outboxId}/retry")
    public ResponseEntity<Void> retryDlq(@PathVariable Long outboxId) {
        fcmMessageService.retryDlq(outboxId);
        return ResponseEntity.ok().build();
    }

/*    @PostMapping("/send")
    public ResponseEntity<ResponseFcmMessageDto> sendMessage(
            @RequestBody RequestFcmMessageDto requestDto
    ) {
        String messageId = fcmMessageService.sendNotification(
                requestDto.getTargetToken(),
                requestDto.getTitle(),
                requestDto.getBody()
        );

        return ResponseEntity.ok(
                ResponseFcmMessageDto.builder()
                        .messageId(messageId)
                        .status("SUCCESS")
                        .build()
        );
    }*/
}
