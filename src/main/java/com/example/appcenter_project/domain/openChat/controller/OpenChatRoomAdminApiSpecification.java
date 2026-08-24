package com.example.appcenter_project.domain.openChat.controller;

import com.example.appcenter_project.domain.openChat.dto.request.RequestAdminBotMessageDto;
import com.example.appcenter_project.domain.openChat.dto.request.RequestCreateDormOfficialRoomDto;
import com.example.appcenter_project.domain.openChat.dto.request.RequestUpdateDormOfficialRoomDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseAdminChatRoomDto;
import com.example.appcenter_project.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@Tag(name = "OpenChat Room Admin", description = "오픈채팅방 관리자 API (ROLE_ADMIN 전용)")
public interface OpenChatRoomAdminApiSpecification {

    @Operation(summary = "기숙사 공식 채팅방 생성", description = "ROLE_ADMIN 전용.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content)
    })
    ResponseEntity<Map<String, Long>> createDormOfficialRoom(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails userDetails,
            @RequestBody @Valid RequestCreateDormOfficialRoomDto request);

    @Operation(summary = "기숙사 공식 채팅방 수정", description = "ROLE_ADMIN 전용.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "수정 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "채팅방 없음", content = @Content)
    })
    ResponseEntity<Void> updateDormOfficialRoom(
            @Parameter(description = "채팅방 ID", required = true) @PathVariable Long roomId,
            @RequestBody @Valid RequestUpdateDormOfficialRoomDto request);

    @Operation(summary = "기숙사 공식 채팅방 삭제", description = "ROLE_ADMIN 전용.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "채팅방 없음", content = @Content)
    })
    ResponseEntity<Void> deleteDormOfficialRoom(
            @Parameter(description = "채팅방 ID", required = true) @PathVariable Long roomId);

    @Operation(summary = "어드민 채팅방 목록 조회", description = "개인 채팅방(PERSONAL)을 제외한 전체 오픈채팅방 목록을 반환한다. ROLE_ADMIN 전용.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content)
    })
    ResponseEntity<List<ResponseAdminChatRoomDto>> getAdminChatRooms();

    @Operation(summary = "어드민 챗봇 메시지 전송", description = "어드민이 채팅방에 참여하지 않고 BOT 타입 메시지를 전송한다. ROLE_ADMIN 전용.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "전송 성공"),
            @ApiResponse(responseCode = "400", description = "개인 채팅방에는 전송 불가", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "채팅방 없음", content = @Content)
    })
    ResponseEntity<Void> sendBotMessage(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails userDetails,
            @Parameter(description = "채팅방 ID", required = true) @PathVariable Long roomId,
            @RequestBody @Valid RequestAdminBotMessageDto request);
}
