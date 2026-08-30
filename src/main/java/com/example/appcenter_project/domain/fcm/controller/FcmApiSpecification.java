package com.example.appcenter_project.domain.fcm.controller;

import com.example.appcenter_project.domain.fcm.dto.request.RequestFcmMessageDto;
import com.example.appcenter_project.domain.fcm.dto.request.RequestFcmTokenDto;
import com.example.appcenter_project.domain.fcm.dto.response.ResponseFcmDlqDto;
import com.example.appcenter_project.domain.fcm.dto.response.ResponseFcmMessageDto;
import com.example.appcenter_project.domain.fcm.dto.response.ResponseFcmStatsDto;
import com.example.appcenter_project.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "FCM", description = "Firebase Cloud Messaging 관련 API")
public interface FcmApiSpecification {

    @Operation(
            summary = "FCM 토큰 저장",
            description = "로그인한 사용자의 FCM 토큰을 저장합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "토큰 저장 성공"),
                    @ApiResponse(responseCode = "404", description = "유저 없음 (USER_NOT_FOUND)", content = @Content())
            }
    )
    ResponseEntity<Void> saveToken(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @RequestBody @Parameter(description = "FCM 토큰 요청 DTO", required = true)
            RequestFcmTokenDto requestDto
    );

    @Operation(
            summary = "FCM 토큰 연결 해제",
            description = "현재 로그인한 사용자의 모든 FCM 토큰에서 user_id를 제거합니다. 이후 같은 토큰으로 POST /fcm/token 호출 시 user_id가 재매핑됩니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "연결 해제 성공"),
                    @ApiResponse(responseCode = "404", description = "유저 없음 (USER_NOT_FOUND)", content = @Content())
            }
    )
    ResponseEntity<Void> unlinkToken(
            @Parameter(hidden = true) CustomUserDetails userDetails
    );

    @Operation(
            summary = "FCM 발송 통계 조회 (ADMIN)",
            description = "오늘 날짜 기준 FCM 알림 발송 성공/실패 건수를 조회합니다. Redis TTL 24시간 기준이므로 당일 데이터만 제공됩니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "통계 조회 성공",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseFcmStatsDto.class))
                    ),
                    @ApiResponse(responseCode = "403", description = "ADMIN 권한 없음", content = @Content())
            }
    )
    ResponseEntity<ResponseFcmStatsDto> getFcmStats();

    @Operation(
            summary = "전체 사용자에게 FCM 알림 전송",
            description = "모든 사용자(로그인하지 않은 사용자 포함)의 FCM 토큰을 대상으로 알림을 발송합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "전체 사용자에게 메시지 전송 성공",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseFcmMessageDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "보낼 FCM 토큰이 존재하지 않음",
                            content = @Content()
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "FCM 발송 중 서버 오류 발생",
                            content = @Content()
                    )
            }
    )
    ResponseEntity<ResponseFcmMessageDto> sendMessageToAllUsers(
            @RequestBody @Parameter(description = "전체 사용자에게 보낼 알림 메시지 요청 DTO", required = true)
            RequestFcmMessageDto requestDto
    );


    @Operation(
            summary = "FCM DLQ 목록 조회 (ADMIN)",
            description = "DEAD_PERMANENT / DEAD_EXHAUSTED 상태인 Outbox 레코드를 페이지 단위로 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "DLQ 목록 조회 성공",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseFcmDlqDto.class))),
                    @ApiResponse(responseCode = "403", description = "ADMIN 권한 없음", content = @Content())
            }
    )
    ResponseEntity<Page<ResponseFcmDlqDto>> getDlqList(Pageable pageable);

    @Operation(
            summary = "FCM DLQ 재시도 (ADMIN)",
            description = "DEAD_EXHAUSTED 상태인 Outbox 레코드를 PENDING으로 초기화하여 재전송을 시도합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "재시도 요청 성공"),
                    @ApiResponse(responseCode = "400", description = "DEAD_EXHAUSTED 상태가 아님", content = @Content()),
                    @ApiResponse(responseCode = "403", description = "ADMIN 권한 없음", content = @Content()),
                    @ApiResponse(responseCode = "404", description = "Outbox 레코드 없음", content = @Content())
            }
    )
    ResponseEntity<Void> retryDlq(@Parameter(description = "재시도할 Outbox ID") Long outboxId);

/*    @Operation(
            summary = "FCM 메시지 발송",
            description = "지정한 targetToken으로 푸시 알림을 발송합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "메시지 발송 성공",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseFcmMessageDto.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청 (토큰/제목/내용 누락)", content = @Content()),
                    @ApiResponse(responseCode = "500", description = "서버 내부 오류 (FCM 발송 실패)", content = @Content())
            }
    )
    ResponseEntity<ResponseFcmMessageDto> sendMessage(
            @RequestBody @Parameter(description = "FCM 메시지 요청 DTO", required = true)
            RequestFcmMessageDto requestDto
    );*/
}
