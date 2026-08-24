package com.example.appcenter_project.domain.openChat.controller;

import com.example.appcenter_project.domain.openChat.dto.request.RequestAdminBotMessageDto;
import com.example.appcenter_project.domain.openChat.dto.request.RequestCreateDormOfficialRoomDto;
import com.example.appcenter_project.domain.openChat.dto.request.RequestUpdateDormOfficialRoomDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseAdminChatRoomDto;
import com.example.appcenter_project.domain.openChat.service.OpenChatDormOfficialRoomService;
import com.example.appcenter_project.domain.openChat.service.OpenChatMessageService;
import com.example.appcenter_project.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/open-chat-rooms")
@RequiredArgsConstructor
public class OpenChatRoomAdminController implements OpenChatRoomAdminApiSpecification {

    private final OpenChatDormOfficialRoomService openChatDormOfficialRoomService;
    private final OpenChatMessageService openChatMessageService;

    @PostMapping("/dorm")
    public ResponseEntity<Map<String, Long>> createDormOfficialRoom(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails userDetails,
            @RequestBody @Valid RequestCreateDormOfficialRoomDto request
    ) {
        Long adminId = userDetails != null ? userDetails.getId() : 0L;
        Long roomId = openChatDormOfficialRoomService.createDormOfficialRoom(adminId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("roomId", roomId));
    }

    @PatchMapping("/dorm/{roomId}")
    public ResponseEntity<Void> updateDormOfficialRoom(
            @PathVariable Long roomId,
            @RequestBody @Valid RequestUpdateDormOfficialRoomDto request) {
        openChatDormOfficialRoomService.updateDormOfficialRoom(roomId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/dorm/{roomId}")
    public ResponseEntity<Void> deleteDormOfficialRoom(
            @PathVariable Long roomId) {
        openChatDormOfficialRoomService.deleteDormOfficialRoom(roomId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ResponseAdminChatRoomDto>> getAdminChatRooms() {
        return ResponseEntity.ok(openChatMessageService.getAdminChatRooms());
    }

    @PostMapping("/{roomId}/bot")
    public ResponseEntity<Void> sendBotMessage(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @RequestBody @Valid RequestAdminBotMessageDto request) {
        Long adminId = userDetails != null ? userDetails.getId() : 0L;
        openChatMessageService.sendBotMessage(adminId, roomId, request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
