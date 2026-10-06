package com.example.appcenter_project.domain.openChat.controller;

import com.example.appcenter_project.domain.openChat.dto.request.RequestEditOpenChatMessageDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatMessageDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatMessageListDto;
import com.example.appcenter_project.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "OpenChat Message", description = "오픈 채팅 메시지 API")
public interface OpenChatMessageApiSpecification {

    @Operation(
            summary = "채팅 메시지 목록 조회",
            description = """
                    커서 기반 페이지네이션으로 채팅 메시지를 최신순으로 조회합니다.

                    - **lastMessageId**: 이전 응답의 `nextCursor` 값을 그대로 전달합니다. 최초 요청 시 생략.
                    - **size**: 한 번에 가져올 메시지 수 (기본 30)
                    - **hasNext**: `true`이면 더 이전 메시지가 존재합니다. `nextCursor`로 다음 요청.

                    **메시지 type 설명**:
                    - `TEXT`: 일반 텍스트 메시지
                    - `IMAGE`: 이미지 메시지 — `imageUrls` 배열에 URL 포함
                    - `SYSTEM`: 시스템 메시지 (입장/퇴장 등) — `senderId`가 null일 수 있음
                    - `ROOM_LINK`: 파생 톡방 링크 메시지 — `linkedRoomId`, `linkedRoomName`, `linkedRoomDescription`, `linkedRoomMaxParticipants` 포함
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = ResponseOpenChatMessageListDto.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "인증 필요"),
                    @ApiResponse(responseCode = "403", description = "채팅방 참여자가 아님"),
                    @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
            }
    )
    ResponseEntity<ResponseOpenChatMessageListDto> getMessages(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable
            @Parameter(description = "채팅방 ID", required = true, example = "1")
            Long roomId,
            @RequestParam(required = false)
            @Parameter(description = "커서: 이전 응답의 nextCursor 값 (첫 요청 시 생략)")
            Long lastMessageId,
            @RequestParam(defaultValue = "30")
            @Parameter(description = "페이지 크기 (기본 30)", example = "30")
            int size,
            @Parameter(hidden = true) HttpServletRequest request);

    @Operation(
            summary = "이미지 메시지 전송",
            description = """
                    채팅방에 이미지를 전송합니다.

                    - **이미지 1장 = 메시지 1개**로 저장되며, 각 메시지가 WebSocket으로 브로드캐스트됩니다.
                    - 지원 형식: jpg / jpeg / png / gif / webp
                    - 최대 **5장**까지 동시 전송 가능
                    - Content-Type은 `multipart/form-data` 로 요청해야 합니다.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "전송 성공 — 생성된 메시지 목록 반환",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResponseOpenChatMessageDto.class)))
                    ),
                    @ApiResponse(responseCode = "400", description = "지원하지 않는 파일 형식 또는 파일 없음"),
                    @ApiResponse(responseCode = "401", description = "인증 필요"),
                    @ApiResponse(responseCode = "403", description = "채팅방 참여자가 아님"),
                    @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
            }
    )
    ResponseEntity<List<ResponseOpenChatMessageDto>> sendImageMessage(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable
            @Parameter(description = "채팅방 ID", required = true, example = "1")
            Long roomId,
            @RequestPart(value = "images", required = false)
            @Parameter(description = "전송할 이미지 파일 목록 (jpg/jpeg/png/gif/webp, 최대 5개)")
            List<MultipartFile> images,
            @Parameter(hidden = true) HttpServletRequest request);

    @Operation(
            summary = "채팅 메시지 삭제",
            description = """
                    - 채팅 메시지를 삭제합니다.
                    - 메시지 작성자이면서 현재 채팅방 참여자인 사용자만 메시지를 삭제할 수 있습니다.
                    - 메시지 삭제는 언제든 가능합니다.
                    - 삭제 성공 후 WebSocket을 통해 삭제된 메시지 정보를 전달합니다.
                    - 구독 경로: `/sub/openchat/{roomId}`
                    - 전달 DTO: `ResponseOpenChatMessageDto`
                    """,
            responses = {
                    @ApiResponse(responseCode = "204", description = "삭제 성공", content = @Content),
                    @ApiResponse(responseCode = "400", description = "해당 채팅방의 메시지가 아님"),
                    @ApiResponse(responseCode = "401", description = "인증 필요"),
                    @ApiResponse(responseCode = "403", description = "채팅방 참여자가 아니거나 메시지 작성자가 아님"),
                    @ApiResponse(responseCode = "404", description = "채팅방 또는 메시지를 찾을 수 없음")
            }
    )
    ResponseEntity<Void> deleteMessage(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "채팅방 ID", required = true, example = "1")
            @PathVariable Long roomId,
            @Parameter(description = "채팅 메시지 ID", required = true, example = "1")
            @PathVariable Long messageId);

    @Operation(
            summary = "텍스트 메시지 수정",
            description = "본인이 작성한 TEXT 타입 메시지의 내용을 수정합니다. 성공 시 수정 이벤트를 WebSocket으로 broadcast합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "수정 성공",
                            content = @Content(schema = @Schema(implementation = ResponseOpenChatMessageDto.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "403", description = "본인 메시지가 아님"),
                    @ApiResponse(responseCode = "404", description = "메시지를 찾을 수 없음")
            }
    )
    ResponseEntity<ResponseOpenChatMessageDto> editMessage(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @RequestBody @Valid RequestEditOpenChatMessageDto dto);
}
