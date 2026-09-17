package com.example.appcenter_project.domain.openChat.controller;

import com.example.appcenter_project.domain.openChat.dto.request.RequestCreateDerivedRoomDto;
import com.example.appcenter_project.domain.openChat.dto.request.RequestCreateOpenChatRoomDto;
import com.example.appcenter_project.domain.openChat.dto.request.RequestCreatePersonalRoomDto;
import com.example.appcenter_project.domain.openChat.dto.request.RequestUpdateNotificationModeDto;
import com.example.appcenter_project.domain.openChat.dto.response.*;
import com.example.appcenter_project.domain.openChat.enums.KickReason;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomTab;
import com.example.appcenter_project.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Tag(name = "OpenChat", description = "오픈 채팅방 API")
public interface OpenChatRoomApiSpecification {

    @Operation(
            summary = "파생 톡방 생성",
            description = """
                    원본 채팅방에서 파생된 하위 채팅방을 생성합니다.

                    - 원본 채팅방 참여자만 생성할 수 있습니다.
                    - 생성 직후 원본 채팅방에 `ROOM_LINK` 타입 메시지가 자동 전송됩니다.
                    - **originRoomId**: 원본 채팅방의 ID (필수)
                    - **isPublic**: 검색 목록 노출 여부 (`true`=공개, `false`=비노출) — password와 독립적
                    - **password**: 입장 비밀번호. isPublic 값과 무관하게 설정 가능. `null`이면 비밀번호 없이 입장.
                    - **maxParticipants**: 최소 2명, 최대 100명
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "생성 성공",
                            content = @Content(schema = @Schema(implementation = ResponseDerivedRoomCreatedDto.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "유효성 검사 실패 (name 공백, maxParticipants 범위 초과 등)"),
                    @ApiResponse(responseCode = "401", description = "인증 필요"),
                    @ApiResponse(responseCode = "403", description = "원본 채팅방 참여자가 아님"),
                    @ApiResponse(responseCode = "404", description = "원본 채팅방을 찾을 수 없음")
            }
    )
    ResponseEntity<ResponseDerivedRoomCreatedDto> createDerivedRoom(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid
            @Parameter(description = "파생 톡방 생성 정보", required = true)
            RequestCreateDerivedRoomDto request);

    @Operation(
            summary = "채팅방 생성",
            description = """
                    새 오픈 채팅방을 생성하고 생성자를 방장으로 등록합니다.

                    **scope**: `DORMITORY`(기숙사 전용) / `ALL`(전체 공개)

                    **maxParticipants**: 최소 2명, 최대 100명

                    **isPublic**: 검색 목록 노출 여부 (`true`=공개 노출, `false`=비노출) — password와 독립적

                    **password**: 입장 비밀번호. `null`이면 비밀번호 없이 입장 가능. isPublic과 무관하게 설정 가능.
                    """,
            responses = {
                    @ApiResponse(responseCode = "201", description = "생성 성공 — `{ roomId: Long }` 반환"),
                    @ApiResponse(responseCode = "400", description = "유효성 검사 실패 (name 공백, maxParticipants 범위 초과 등)"),
                    @ApiResponse(responseCode = "401", description = "인증 필요")
            }
    )
    ResponseEntity<Map<String, Long>> createRoom(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid
            @Parameter(description = "채팅방 생성 정보", required = true)
            RequestCreateOpenChatRoomDto request);

    @Operation(
            summary = "개인(1:1) 채팅방 생성",
            description = """
                    특정 유저와 1:1 개인 채팅방을 생성합니다.

                    - 동일한 두 유저 사이에 이미 개인 채팅방이 존재하면 409 반환
                    - **targetUserId**: 대화 상대방의 userId
                    - **password**: 입장 비밀번호 (선택, 최대 50자)
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "생성 성공",
                            content = @Content(schema = @Schema(implementation = ResponsePersonalRoomCreatedDto.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "유효성 검사 실패"),
                    @ApiResponse(responseCode = "401", description = "인증 필요"),
                    @ApiResponse(responseCode = "404", description = "대화 상대 유저를 찾을 수 없음"),
                    @ApiResponse(responseCode = "409", description = "이미 해당 유저와 개인 채팅방 존재")
            }
    )
    ResponseEntity<ResponsePersonalRoomCreatedDto> createPersonalRoom(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid
            @Parameter(description = "개인 채팅방 생성 정보", required = true)
            RequestCreatePersonalRoomDto request);

    @Operation(
            summary = "채팅방 목록 조회",
            description = """
                    탭에 따라 채팅방 목록을 페이지 단위로 조회합니다.

                    **tab**:
                    - `MY`: 내가 참여 중인 방 (keyword 무시)
                    - `DORMITORY`: 기숙사 전용 공개 방
                    - `ALL`: 전체 공개 방

                    **totalUnreadCount**: tab=MY 일 때 내 모든 방의 안읽은 메시지 총합
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = ResponseChatRoomListDto.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "인증 필요")
            }
    )
    ResponseEntity<ResponseChatRoomListDto> getRooms(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam
            @Parameter(description = "조회 탭 (MY / DORMITORY / ALL)", required = true, example = "MY")
            OpenChatRoomTab tab,
            @RequestParam(required = false)
            @Parameter(description = "방 이름 검색어 (선택)")
            String keyword,
            Pageable pageable);

    @Operation(
            summary = "채팅방 입장",
            description = """
                    지정한 채팅방에 입장합니다.

                    - 이미 참여 중인 방에 재요청하면 현재 방 정보를 그대로 반환합니다.
                    - **password**: 방에 비밀번호가 설정되어 있으면 필요. isPublic 값과 무관하게 적용됩니다.
                    - 인원이 꽉 찬 경우 409 반환
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "입장 성공",
                            content = @Content(schema = @Schema(implementation = ResponseOpenChatRoomDetailDto.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "비밀번호 불일치"),
                    @ApiResponse(responseCode = "401", description = "인증 필요"),
                    @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
                    @ApiResponse(responseCode = "409", description = "최대 인원 초과")
            }
    )
    ResponseEntity<ResponseOpenChatRoomDetailDto> joinRoom(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable
            @Parameter(description = "입장할 채팅방 ID", required = true, example = "1")
            Long roomId,
            @RequestParam(required = false)
            @Parameter(description = "비공개 방 비밀번호 (선택)")
            String password);

    @Operation(
            summary = "채팅방 나가기",
            description = """
                    채팅방에서 나갑니다.

                    - **roomDeleted**: 마지막 참여자가 나가면 방이 자동 삭제되며 `true` 반환
                    - 방장이 나갈 때 다른 참여자가 남아있으면 **newHostUserId**를 반드시 지정해야 합니다.
                    - 방장이 유일한 참여자라면 newHostUserId 없이 나갈 수 있습니다.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "나가기 성공",
                            content = @Content(schema = @Schema(implementation = ResponseLeaveOpenChatRoomDto.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "방장이 newHostUserId를 지정하지 않음"),
                    @ApiResponse(responseCode = "401", description = "인증 필요"),
                    @ApiResponse(responseCode = "404", description = "채팅방 또는 참여 정보를 찾을 수 없음")
            }
    )
    ResponseEntity<ResponseLeaveOpenChatRoomDto> leaveRoom(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable
            @Parameter(description = "나갈 채팅방 ID", required = true, example = "1")
            Long roomId,
            @RequestParam(required = false)
            @Parameter(description = "방장 권한을 넘길 참여자 userId (방장이 나갈 때 필수)")
            Long newHostUserId);

    @Operation(
            summary = "FCM 알림 모드 변경",
            description = """
                    해당 채팅방의 FCM 푸시 알림 모드를 변경합니다.

                    **mode**:
                    - `EVERY`: 메시지 수신 시마다 알림 발송
                    - `BUNDLED`: 일정 시간마다 묶음 알림
                    - `OFF`: 알림 끄기
                    """,
            responses = {
                    @ApiResponse(responseCode = "204", description = "변경 성공"),
                    @ApiResponse(responseCode = "400", description = "유효하지 않은 mode 값"),
                    @ApiResponse(responseCode = "401", description = "인증 필요"),
                    @ApiResponse(responseCode = "404", description = "채팅방 또는 참여 정보를 찾을 수 없음")
            }
    )
    ResponseEntity<Void> updateNotification(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable
            @Parameter(description = "채팅방 ID", required = true, example = "1")
            Long roomId,
            @RequestBody @Valid
            @Parameter(description = "알림 모드 (EVERY / BUNDLED / OFF)", required = true)
            RequestUpdateNotificationModeDto dto);

    @Operation(
            summary = "FCM 알림 모드 조회",
            description = """
                    해당 채팅방에서 내 현재 FCM 알림 모드를 조회합니다.

                    **mode**: `EVERY`(즉시) / `BUNDLED`(묶음) / `OFF`(끄기)
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = ResponseNotificationModeDto.class))),
                    @ApiResponse(responseCode = "401", description = "인증 필요"),
                    @ApiResponse(responseCode = "404", description = "채팅방 또는 참여 정보를 찾을 수 없음")
            }
    )
    ResponseEntity<ResponseNotificationModeDto> getNotification(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable
            @Parameter(description = "채팅방 ID", required = true, example = "1")
            Long roomId);

    @Operation(
            summary = "채팅방 강제 삭제",
            description = """
                    채팅방을 강제로 삭제합니다.

                    - 방장 또는 ADMIN 권한이 필요합니다.
                    - 삭제 시 모든 참여자가 퇴장 처리되고 메시지도 함께 삭제됩니다.
                    """,
            responses = {
                    @ApiResponse(responseCode = "204", description = "삭제 성공"),
                    @ApiResponse(responseCode = "401", description = "인증 필요"),
                    @ApiResponse(responseCode = "403", description = "방장 또는 ADMIN 권한 없음"),
                    @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
            }
    )
    ResponseEntity<Void> deleteRoom(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable
            @Parameter(description = "삭제할 채팅방 ID", required = true, example = "1")
            Long roomId);

    @Operation(
            summary = "파생 톡방 모집 마감",
            description = """
                    파생 톡방(`DERIVED`)의 모집을 마감합니다.

                    - 방장(`createdBy`) 또는 ADMIN 권한 사용자만 호출할 수 있습니다.
                    - 마감 후에는 비참여자의 입장이 차단됩니다. 기존 참여자의 채팅·활동은 계속 가능합니다.
                    - 단방향(재개 불가). 이미 마감된 방을 다시 마감 시도 시 409.
                    - `OPEN` / `PERSONAL` / 공식방은 400 반환.
                    """,
            responses = {
                    @ApiResponse(responseCode = "204", description = "마감 성공"),
                    @ApiResponse(responseCode = "400", description = "DERIVED 타입이 아닌 방"),
                    @ApiResponse(responseCode = "401", description = "인증 필요"),
                    @ApiResponse(responseCode = "403", description = "방장 또는 ADMIN 권한 없음"),
                    @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
                    @ApiResponse(responseCode = "409", description = "이미 마감된 채팅방")
            }
    )
    ResponseEntity<Void> closeRecruitment(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable
            @Parameter(description = "마감할 파생 톡방 ID", required = true, example = "1")
            Long roomId);

    @Operation(
            summary = "방장 권한 부여",
            description = """
                    방장이 다른 참여자에게 방장 권한을 추가로 부여합니다.

                    - 요청자(현 방장)는 방장 권한을 **유지합니다**. (권한 이전이 아닌 추가 부여)
                    - 이미 방장인 참여자에게 요청하면 409 반환
                    """,
            responses = {
                    @ApiResponse(responseCode = "204", description = "권한 부여 성공"),
                    @ApiResponse(responseCode = "401", description = "인증 필요"),
                    @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
                    @ApiResponse(responseCode = "404", description = "채팅방 또는 대상 참여자를 찾을 수 없음"),
                    @ApiResponse(responseCode = "409", description = "이미 방장인 참여자")
            }
    )
    ResponseEntity<Void> grantHost(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable
            @Parameter(description = "채팅방 ID", required = true, example = "1")
            Long roomId,
            @PathVariable
            @Parameter(description = "방장 권한을 부여할 참여자 userId", required = true, example = "42")
            Long targetUserId);

    @Operation(
            summary = "방장 권한 제거 (ADMIN 전용)",
            description = """
                    ADMIN이 특정 참여자의 방장 권한을 제거합니다.

                    - 참여자는 방에 계속 남아있습니다.
                    - ADMIN 권한이 없는 사용자가 호출하면 403 반환
                    """,
            responses = {
                    @ApiResponse(responseCode = "204", description = "권한 제거 성공"),
                    @ApiResponse(responseCode = "401", description = "인증 필요"),
                    @ApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
                    @ApiResponse(responseCode = "404", description = "채팅방 또는 대상 참여자를 찾을 수 없음")
            }
    )
    ResponseEntity<Void> revokeHostByAdmin(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable
            @Parameter(description = "채팅방 ID", required = true, example = "1")
            Long roomId,
            @PathVariable
            @Parameter(description = "방장 권한을 제거할 참여자 userId", required = true, example = "42")
            Long targetUserId);

    @Operation(
            summary = "방장 위임",
            description = """
                    방장이 자신의 방장 권한을 다른 참여자에게 위임합니다.

                    - 위임 후 요청자는 방에 남지만 **방장 권한을 잃습니다**.
                    - **targetUserId**: 방장을 위임받을 참여자 userId (쿼리 파라미터, 필수)
                    """,
            responses = {
                    @ApiResponse(responseCode = "204", description = "위임 성공"),
                    @ApiResponse(responseCode = "401", description = "인증 필요"),
                    @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
                    @ApiResponse(responseCode = "404", description = "채팅방 또는 대상 참여자를 찾을 수 없음")
            }
    )
    ResponseEntity<Void> transferHost(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable
            @Parameter(description = "채팅방 ID", required = true, example = "1")
            Long roomId,
            @RequestParam
            @Parameter(description = "방장을 위임받을 참여자 userId", required = true, example = "42")
            Long targetUserId);

    @Operation(
            summary = "채팅방 참여자 목록 조회",
            description = """
                    채팅방 전체 참여자 목록과 방장 수를 조회합니다.

                    - **isHost**: 방장 여부
                    - **isAdmin**: 앱 관리자 여부
                    - **joinedAt**: ISO-8601 형식 입장 시각
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = ResponseOpenChatParticipantListDto.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "인증 필요"),
                    @ApiResponse(responseCode = "403", description = "채팅방 참여자가 아님"),
                    @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
            }
    )
    ResponseEntity<ResponseOpenChatParticipantListDto> getParticipants(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable
            @Parameter(description = "채팅방 ID", required = true, example = "1")
            Long roomId);

    @Operation(
            summary = "참여자 강제퇴장",
            description = """
                    방장 또는 ADMIN이 특정 참여자를 강제퇴장(kick)시킵니다.

                    **reason**: `SPAM` / `ABUSE` / `IMPERSONATION` / `REPORT_ACCUMULATED` / `OTHER`

                    - 강제퇴장된 참여자에게 퇴장 알림이 전송됩니다.
                    - 강제퇴장 대상이 방장이었다면 **newHostUserId**를 지정해야 합니다.
                    """,
            responses = {
                    @ApiResponse(responseCode = "204", description = "강제퇴장 성공"),
                    @ApiResponse(responseCode = "400", description = "newHostUserId 누락 (대상이 방장인 경우)"),
                    @ApiResponse(responseCode = "401", description = "인증 필요"),
                    @ApiResponse(responseCode = "403", description = "방장 또는 ADMIN 권한 없음"),
                    @ApiResponse(responseCode = "404", description = "채팅방 또는 대상 참여자를 찾을 수 없음")
            }
    )
    ResponseEntity<Void> kickParticipant(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable
            @Parameter(description = "채팅방 ID", required = true, example = "1")
            Long roomId,
            @PathVariable
            @Parameter(description = "강제퇴장할 참여자 userId", required = true, example = "42")
            Long targetUserId,
            @RequestParam
            @Parameter(description = "퇴장 사유 (SPAM / ABUSE / IMPERSONATION / REPORT_ACCUMULATED / OTHER)", required = true, example = "SPAM")
            KickReason reason,
            @RequestParam(required = false)
            @Parameter(description = "대상이 방장이었을 때 방장 권한을 넘길 참여자 userId")
            Long newHostUserId);

    @Operation(
            summary = "참여자 단순 목록 조회",
            description = """
                    채팅방 참여자의 userId와 이름만 간략히 반환합니다.

                    학번 공개 요청 등 상대방 선택 UI에서 사용합니다.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = ResponseSimpleParticipantListDto.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "인증 필요"),
                    @ApiResponse(responseCode = "403", description = "채팅방 참여자가 아님"),
                    @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
            }
    )
    ResponseEntity<ResponseSimpleParticipantListDto> getSimpleParticipants(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable
            @Parameter(description = "채팅방 ID", required = true, example = "1")
            Long roomId);
}
