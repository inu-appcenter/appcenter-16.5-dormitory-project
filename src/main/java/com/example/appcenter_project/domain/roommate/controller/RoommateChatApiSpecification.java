package com.example.appcenter_project.domain.roommate.controller;

import com.example.appcenter_project.domain.roommate.dto.request.RequestRoommateChatDto;
import com.example.appcenter_project.domain.roommate.dto.response.ResponseRoommateChatDto;
import com.example.appcenter_project.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.OK;

@Tag(name = "RoommateChat", description = "룸메이트 채팅 관련 API")
public interface RoommateChatApiSpecification  {

    @Operation(
            summary = "채팅 메시지 전송",
            description = "해당 채팅방에 메시지를 전송합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "메시지 전송 성공",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseRoommateChatDto.class))),
                    @ApiResponse(responseCode = "404", description = "유저 또는 채팅방 없음 (USER_NOT_FOUND, ROOMMATE_CHAT_ROOM_NOT_FOUND)",
                            content = @Content()),
                    @ApiResponse(responseCode = "403", description = "채팅방 권한 없음 (ROOMMATE_CHAT_ROOM_FORBIDDEN)",
                            content = @Content())
            }
    )
    ResponseEntity<ResponseRoommateChatDto> sendChat(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @RequestBody @Parameter(description = "채팅 메시지 요청 DTO", required = true)
            RequestRoommateChatDto requestRoommateChatDto
    );

    @Operation(
            summary = "채팅 내역 조회",
            description = "채팅방에 입장 시, 해당 방의 전체 채팅 내역을 반환합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "채팅 내역 조회 성공",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseRoommateChatDto.class))),
                    @ApiResponse(responseCode = "404", description = "유저 또는 채팅방 없음 (USER_NOT_FOUND, ROOMMATE_CHAT_ROOM_NOT_FOUND)",
                            content = @Content()),
                    @ApiResponse(responseCode = "403", description = "채팅방 권한 없음 (ROOMMATE_CHAT_ROOM_FORBIDDEN)",
                            content = @Content())
            }
    )
    ResponseEntity<List<ResponseRoommateChatDto>> getChatList(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            HttpServletRequest request,
            @Parameter(description = "조회할 채팅방 ID", example = "1") @PathVariable Long roomId
    );

    @Operation(
            summary = "채팅 읽음 처리",
            description = "해당 채팅방의 안 읽은 메시지를 읽음 처리합니다. (백업용)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
                    @ApiResponse(responseCode = "404", description = "유저 또는 채팅방 없음 (USER_NOT_FOUND, ROOMMATE_CHAT_ROOM_NOT_FOUND)",
                            content = @Content())
            }
    )
    ResponseEntity<Void> markAsRead(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "읽음 처리할 채팅방 ID", example = "1") @PathVariable Long roomId
    );

    @Operation(
            summary = "특정 유저의 특정 채팅방에서 안읽은 메시지 카운트",
            description = "특정 유저의 특정 채팅방에서 안읽은 메시지 카운트"
    )
    public ResponseEntity<Integer> getUnReadCountByUserIdAdRoomId(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId
    );


    @Operation(
            summary = "특정 유저 의모든 채팅방에서 안읽은 메시지 카운트",
            description = "특정 유저의 모든 채팅방에서 안읽은 메시지 카운트"
    )
    public ResponseEntity<Integer> getUnReadCountByUserId(@AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(
            summary = "[WebSocket] 채팅 메시지 전송",
            description = """
                    WebSocket을 통해 채팅 메시지를 전송합니다.  
                    STOMP 엔드포인트: `/pub/roommate/chat`  
                    메시지 형식:
                    ```json
                    {
                      "roommateChattingRoomId": 1,
                      "content": "안녕하세요"
                    }
                    ```
                    """
    )
    default void sendChatViaWebSocket() {
        // 설명용으로 Swagger UI에만 노출되며 실제 구현은 컨트롤러에 있음.
    }

    @Operation(
            summary = "룸메이트 채팅 메시지 삭제",
            description = """
                    - 메시지 작성자이면서 채팅방 참여자인 사용자만 메시지를 삭제할 수 있습니다.
                    - 메시지 작성 후 경과 시간에 제한 없이 소프트 삭제합니다.
                    - 삭제 성공 시 응답 본문 없이 204 No Content를 반환합니다.
                    - 이후 채팅 내역 조회 시 본문은 `삭제된 메시지입니다.`로 반환됩니다.
                    - WebSocket 구독 경로: `/sub/roommate/chat/{roomId}`
                    - 삭제 이벤트 DTO: `ResponseRoommateChatDeleteEventDto`
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
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "채팅방 ID", required = true, example = "1")
            @PathVariable Long roomId,
            @Parameter(description = "채팅 메시지 ID", required = true, example = "1")
            @PathVariable Long messageId);
}
