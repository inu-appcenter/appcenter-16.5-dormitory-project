package com.example.appcenter_project.domain.openChat.controller;

import com.example.appcenter_project.domain.openChat.dto.request.RequestCreateDerivedRoomDto;
import com.example.appcenter_project.domain.openChat.dto.request.RequestCreateOpenChatRoomDto;
import com.example.appcenter_project.domain.openChat.dto.request.RequestCreatePersonalRoomDto;
import com.example.appcenter_project.domain.openChat.dto.request.RequestUpdateNotificationModeDto;
import com.example.appcenter_project.domain.openChat.dto.request.RequestUpdateOpenChatRoomDto;
import com.example.appcenter_project.domain.openChat.dto.response.*;
import com.example.appcenter_project.domain.openChat.enums.KickReason;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomTab;
import com.example.appcenter_project.domain.openChat.service.OpenChatRoomService;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/open-chat-rooms")
public class OpenChatRoomController implements OpenChatRoomApiSpecification {

    private final OpenChatRoomService openChatRoomService;

    @PostMapping("/derived")
    public ResponseEntity<ResponseDerivedRoomCreatedDto> createDerivedRoom(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid RequestCreateDerivedRoomDto request) {
        ResponseDerivedRoomCreatedDto result = openChatRoomService.createDerivedRoom(user.getId(), request);
        return ResponseEntity.status(CREATED).body(result);
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> createRoom(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid RequestCreateOpenChatRoomDto request) {
        Long roomId = openChatRoomService.createRoom(request, user.getId());
        return ResponseEntity.status(CREATED).body(Map.of("roomId", roomId));
    }

    @PostMapping("/personal")
    public ResponseEntity<ResponsePersonalRoomCreatedDto> createPersonalRoom(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid RequestCreatePersonalRoomDto request) {
        ResponsePersonalRoomCreatedDto result = openChatRoomService.createPersonalRoom(user.getId(), request);
        return ResponseEntity.status(CREATED).body(result);
    }

    @GetMapping
    public ResponseEntity<ResponseChatRoomListDto> getRooms(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam OpenChatRoomTab tab,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        Long userId = user != null ? user.getId() : null;
        ResponseChatRoomListDto result = openChatRoomService.getRooms(userId, tab, keyword, pageable);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{roomId}/participants/me")
    public ResponseEntity<ResponseOpenChatRoomDetailDto> joinRoom(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long roomId,
            @RequestParam(required = false) String password) {
        ResponseOpenChatRoomDetailDto result = openChatRoomService.joinRoom(user.getId(), roomId, password);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{roomId}/participants/me")
    public ResponseEntity<ResponseLeaveOpenChatRoomDto> leaveRoom(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long newHostUserId) {
        ResponseLeaveOpenChatRoomDto result = openChatRoomService.leaveRoom(roomId, user.getId(), newHostUserId);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{roomId}/participants/me/notification")
    public ResponseEntity<Void> updateNotification(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long roomId,
            @RequestBody @Valid RequestUpdateNotificationModeDto dto) {
        openChatRoomService.updateNotificationMode(user.getId(), roomId, dto.getMode());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roomId}/participants/me/notification")
    public ResponseEntity<ResponseNotificationModeDto> getNotification(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long roomId) {
        ResponseNotificationModeDto result = openChatRoomService.getNotificationMode(user.getId(), roomId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{roomId}/participants/{targetUserId}")
    public ResponseEntity<Void> kickParticipant(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long roomId,
            @PathVariable Long targetUserId,
            @RequestParam KickReason reason,
            @RequestParam(required = false) Long newHostUserId) {
        openChatRoomService.kickParticipant(user.getId(), roomId, targetUserId, reason, newHostUserId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{roomId}")
    public ResponseEntity<Void> updateRoom(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long roomId,
            @RequestBody @Valid RequestUpdateOpenChatRoomDto request) {
        Long userId = user != null ? user.getId() : 0L;
        openChatRoomService.updateRoom(userId, roomId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long roomId) {
        openChatRoomService.deleteRoom(roomId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{roomId}/close-recruitment")
    public ResponseEntity<Void> closeRecruitment(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long roomId) {
        openChatRoomService.closeRecruitment(user.getId(), roomId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{roomId}/hosts/{targetUserId}")
    public ResponseEntity<Void> grantHost(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long roomId,
            @PathVariable Long targetUserId) {
        openChatRoomService.grantHost(roomId, user.getId(), targetUserId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{roomId}/hosts/{targetUserId}")
    public ResponseEntity<Void> revokeHostByAdmin(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long roomId,
            @PathVariable Long targetUserId) {
        openChatRoomService.revokeHostByAdmin(roomId, user.getId(), targetUserId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{roomId}/hosts/me")
    public ResponseEntity<Void> transferHost(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long roomId,
            @RequestParam Long targetUserId) {
        openChatRoomService.transferHost(roomId, user.getId(), targetUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roomId}/participants")
    public ResponseEntity<ResponseOpenChatParticipantListDto> getParticipants(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long roomId) {
        ResponseOpenChatParticipantListDto result = openChatRoomService.getParticipants(roomId, user.getId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{roomId}/participants/simple")
    public ResponseEntity<ResponseSimpleParticipantListDto> getSimpleParticipants(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long roomId) {
        ResponseSimpleParticipantListDto result = openChatRoomService.getSimpleParticipants(roomId, user.getId());
        return ResponseEntity.ok(result);
    }

    private String getDormType(CustomUserDetails user) {
        User u = user.getUser();
        if (u == null || u.getDormType() == null) {
            return "NONE";
        }
        return u.getDormType().name();
    }
}
