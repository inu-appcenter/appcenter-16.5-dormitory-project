package com.example.appcenter_project.domain.roommate.controller;

import com.example.appcenter_project.domain.roommate.dto.response.ResponseRoommateChatRoomDetailDto;
import com.example.appcenter_project.domain.roommate.dto.response.ResponseRoommateChatRoomDto;
import com.example.appcenter_project.domain.roommate.dto.response.ResponseRoommateCheckListDto;
import com.example.appcenter_project.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;   // 추가

import java.util.List;

@Tag(name = "RoommateChattingRoom", description = "룸메이트 채팅방 관련 API")
public interface RoommateChattingRoomApiSpecification {

    @Operation(
            summary = "채팅방 생성",
            description = "특정 룸메이트 게시글을 기반으로 채팅방을 생성합니다. (게스트가 요청)",
            responses = {
                    @ApiResponse(responseCode = "201", description = "채팅방 생성 성공",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Long.class))),
                    @ApiResponse(responseCode = "404", description = "게시글 또는 유저를 찾을 수 없음"),
                    @ApiResponse(responseCode = "400", description = "자기 자신과의 채팅 방지, 중복 생성 방지")
            }
    )
    ResponseEntity<Long> createChatRoom(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            Long roommateBoardId
    );

    @Operation(
            summary = "채팅방 나가기",
            description = "게스트가 특정 채팅방에서 나가며 채팅방이 삭제됩니다.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "나가기 성공"),
                    @ApiResponse(responseCode = "404", description = "유저 또는 채팅방을 찾을 수 없음"),
                    @ApiResponse(responseCode = "403", description = "게스트가 아닌 사용자의 접근")
            }
    )
    ResponseEntity<Void> leaveChatRoom(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            Long chatRoomId
    );

    @Operation(
            summary = "채팅방 목록 조회",
            description = "로그인한 사용자가 참여한 모든 채팅방 목록을 조회합니다. (마지막 메시지 기준 최신순)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseRoommateChatRoomDto.class))),
                    @ApiResponse(responseCode = "404", description = "유저를 찾을 수 없음")
            }
    )
    ResponseEntity<List<ResponseRoommateChatRoomDto>> getRoommateChatRoomList(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(hidden = true) HttpServletRequest request   // 추가
    );

    @Operation(
            summary = "룸메이트 채팅방 상세 조회",
            description = "채팅방 ID로 상세 정보(상대방 이름, 프로필, 나간 여부, 차단 여부, 게시글 제목)를 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseRoommateChatRoomDetailDto.class))),
                    @ApiResponse(responseCode = "404", description = "채팅방이 존재하지 않음"),
                    @ApiResponse(responseCode = "403", description = "해당 채팅방 참여자가 아님")
            }
    )
    ResponseEntity<ResponseRoommateChatRoomDetailDto> getRoommateChatRoomDetail(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "채팅방 ID") Long chatRoomId,
            @Parameter(hidden = true) HttpServletRequest request
    );

    @Operation(
            summary = "상대방 체크리스트 조회",
            description = "채팅방 ID를 통해 현재 로그인한 사용자의 상대방 체크리스트를 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseRoommateCheckListDto.class))),
                    @ApiResponse(responseCode = "404", description = "채팅방이 존재하지 않음"),
                    @ApiResponse(responseCode = "403", description = "해당 채팅방에 접근할 권한 없음")
            }
    )
    ResponseEntity<ResponseRoommateCheckListDto> getOpponentChecklist(
            @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "채팅방 ID") Long chatRoomId
    );
}
