package com.example.appcenter_project.domain.openChat.controller;

import com.example.appcenter_project.domain.openChat.dto.request.RequestOpenChatMessageDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatMessageDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatMessageListDto;
import com.example.appcenter_project.domain.openChat.service.OpenChatMessageService;
import com.example.appcenter_project.global.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/open-chat-rooms")
public class OpenChatMessageController implements OpenChatMessageApiSpecification {

    private final OpenChatMessageService openChatMessageService;

    @MessageMapping("/openchat/socketchat")
    public void sendChatViaWebSocket(@Valid RequestOpenChatMessageDto dto, SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) return;
        String userIdStr = (String) sessionAttributes.get("userId");
        if (userIdStr == null) return;
        Long userId = Long.parseLong(userIdStr);
        openChatMessageService.sendMessage(userId, dto);
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<ResponseOpenChatMessageListDto> getMessages(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long lastMessageId,
            @RequestParam(defaultValue = "30") int size,
            HttpServletRequest request) {
        return ResponseEntity.ok(openChatMessageService.getMessages(user.getId(), roomId, lastMessageId, size, request));
    }

    @PostMapping(value = "/{roomId}/messages/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ResponseOpenChatMessageDto>> sendImageMessage(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long roomId,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(openChatMessageService.sendImageMessage(user.getId(), roomId, images, request));
    }

    @DeleteMapping("/{roomId}/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long roomId,
            @PathVariable Long messageId) {
        openChatMessageService.deleteMessage(user.getId(), roomId, messageId);
        return ResponseEntity.noContent().build();
    }
}
