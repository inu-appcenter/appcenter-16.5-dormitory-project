package com.example.appcenter_project.domain.roommate.controller;

import com.example.appcenter_project.domain.roommate.dto.request.RequestRoommateFormDto;
import com.example.appcenter_project.domain.roommate.dto.response.*;
import com.example.appcenter_project.domain.roommate.enums.SemesterType;
import com.example.appcenter_project.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@Tag(name = "Roommate", description = "룸메이트 게시글 및 체크리스트 관련 API")
public interface RoommateApiSpecification {

    @Operation(
            summary = "룸메이트 체크리스트 및 게시글 작성",
            description = "룸메이트 체크리스트를 작성하고 동시에 게시글을 등록합니다.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "룸메이트 게시글 등록 성공",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseRoommatePostDto.class))),
                    @ApiResponse(responseCode = "404", description = "해당 유저가 존재하지 않습니다. (ROOMMATE_USER_NOT_FOUND)")
            }
    )
    ResponseEntity<ResponseRoommatePostDto> createRoommatePost(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @RequestBody
            @Parameter(description = "룸메이트 체크리스트 요청 DTO", required = true)
            RequestRoommateFormDto requestDto
            // ※ 현재 컨트롤러가 request를 안 받으니 인터페이스도 받지 않음
    );

    @Operation(
            summary = "룸메이트 게시글 목록 조회 (전체 학기)",
            description = "작성된 룸메이트 게시글 전체를 최신순으로 조회합니다. " +
                    "각 게시글의 year/semester 값으로 학기를 구분(라벨링)할 수 있습니다."
    )
    ResponseEntity<List<ResponseRoommatePostDto>> getRoommateBoardList(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(hidden = true) HttpServletRequest request
    );

    @Operation(
            summary = "룸메이트 게시글 단일 조회",
            description = "특정 게시글 ID를 통해 룸메이트 게시글 상세 정보를 조회합니다. (작성자 프로필 이미지 URL 포함)"
    )
    ResponseEntity<ResponseRoommatePostDto> getRoommateBoardDetail(
            @Parameter(description = "조회할 게시글 ID", example = "1") @PathVariable Long boardId,
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(hidden = true) HttpServletRequest request
    );

    @Operation(
            summary = "유사한 룸메이트 게시글 추천",
            description = "로그인한 사용자의 체크리스트 기준으로 유사한 게시글을 추천합니다. (작성자 프로필 이미지 URL 포함)"
    )
    ResponseEntity<List<ResponseRoommateSimilarityDto>> getSimilarRoommates(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(hidden = true) HttpServletRequest request
    );

    @Operation(
            summary = "룸메이트 체크리스트 및 게시글 수정",
            description = "기존에 작성한 룸메이트 체크리스트 및 게시글을 수정합니다. (작성자 프로필 이미지 URL 포함)"
    )
    ResponseEntity<ResponseRoommatePostDto> updateRoommateCheckListAndBoard(
            @Parameter(description = "수정할 게시글 ID", example = "10") @PathVariable Long boardId,
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @RequestBody
            @Parameter(description = "수정할 룸메이트 체크리스트 요청 DTO", required = true)
            RequestRoommateFormDto requestDto,
            @Parameter(hidden = true) HttpServletRequest request
    );

    @Operation(
            summary = "룸메이트 게시글 삭제",
            description = "본인이 작성한 룸메이트 게시글을 삭제합니다. 연결된 채팅방과 채팅 내역은 유지됩니다.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "삭제 성공"),
                    @ApiResponse(responseCode = "403", description = "삭제 권한이 없습니다. (ROOMMATE_DELETE_NOT_ALLOWED)"),
                    @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없습니다. (ROOMMATE_BOARD_NOT_FOUND)")
            }
    )
    ResponseEntity<Void> deleteRoommateBoard(
            @Parameter(description = "삭제할 게시글 ID", example = "10") @PathVariable Long boardId,
            @Parameter(hidden = true) CustomUserDetails userDetails
    );

    @Operation(summary = "룸메이트 게시글 좋아요")
    ResponseEntity<Integer> plusLike(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "좋아요를 누를 게시글 ID", example = "1") @PathVariable Long boardId
    );

    @Operation(summary = "룸메이트 게시글 좋아요 취소")
    ResponseEntity<Integer> minusLike(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "좋아요 취소할 게시글 ID", example = "1") @PathVariable Long boardId
    );

    @Operation(summary = "룸메이트 게시글 주인의 매칭 여부 조회")
    ResponseEntity<Boolean> isBoardOwnerMatched(
            @Parameter(description = "조회할 게시글 ID", example = "1") @PathVariable Long boardId
    );

    @Operation(summary = "룸메이트 게시글 좋아요 여부 조회")
    ResponseEntity<Boolean> isRoommateBoardLiked(
            @Parameter(description = "조회할 게시글 ID", example = "1") @PathVariable Long boardId,
            @Parameter(hidden = true) CustomUserDetails userDetails
    );

    @Operation(
            summary = "내 체크리스트 내용 조회",
            description = "로그인한 사용자가 작성한 가장 최근(현재 학기) 체크리스트 내용을 반환합니다. " +
                    "체크리스트 수정 화면 프리필에 사용합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseRoommateCheckListDto.class))),
                    @ApiResponse(responseCode = "404", description = "체크리스트를 찾을 수 없습니다. (ROOMMATE_CHECKLIST_NOT_FOUND)")
            }
    )
    ResponseEntity<ResponseRoommateCheckListDto> getMyCheckList(
            @Parameter(hidden = true) CustomUserDetails userDetails
    );

    @Operation(
            summary = "현재 학기 내 게시물 번호 조회",
            description = "로그인한 사용자의 현재 학기 룸메이트 게시물 ID를 반환합니다. " +
                    "페이지 밖으로 밀려난 경우에도 게시물을 특정하기 위해 사용합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseMyRoommateBoardIdDto.class))),
                    @ApiResponse(responseCode = "404", description = "게시물을 찾을 수 없습니다. (ROOMMATE_BOARD_NOT_FOUND)")
            }
    )
    ResponseEntity<ResponseMyRoommateBoardIdDto> getMyBoardId(
            @Parameter(hidden = true) CustomUserDetails userDetails
    );

    @Operation(
            summary = "이전 학기 체크리스트 내용 조회",
            description = "현재 학기를 제외한, 로그인한 사용자의 가장 최근 과거 체크리스트 내용을 반환합니다. " +
                    "신규 학기 체크리스트 작성 시 '이전 학기에서 불러오기'에 사용합니다. " +
                    "직전 학기가 없어도 가장 최근인 과거 데이터를 반환합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseRoommateCheckListDto.class))),
                    @ApiResponse(responseCode = "404", description = "불러올 이전 학기 체크리스트가 없습니다. (ROOMMATE_CHECKLIST_NOT_FOUND)")
            }
    )
    ResponseEntity<ResponseRoommateCheckListDto> getPreviousCheckListContent(
            @Parameter(hidden = true) CustomUserDetails userDetails
    );
    @Operation(
            summary = "최신 10개 중 무작위 1개 조회",
            description = "최신 10개 게시글 중 무작위 1개를 반환합니다. 작성자 프로필 이미지 URL 포함"
    )
    ResponseEntity<ResponseRoommatePostDto> getRandomFromLatest10(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(hidden = true) HttpServletRequest request
    );

    @Operation(
            summary = "룸메이트 게시글 최신순 스크롤 조회",
            description = "boardId 내림차순으로 최신순 게시글을 페이지네이션하여 조회합니다. " +
                    "lastId를 기준으로 이전 페이지 데이터를 불러옵니다. 로그인 시 isMyPost 반환.\n\n" +
                    "**학기 필터 규칙**\n" +
                    "- `semester` 미전송 또는 미인식 값(0 등) → 전체 학기: year 값에 상관없이 모든 게시글 반환\n" +
                    "- `semester=1~4` 지정 → 해당 학기만 필터링, year도 함께 지정하면 연도까지 필터링"
    )
    ResponseEntity<List<ResponseRoommatePostDto>> getRoommateBoardListScroll(
            @Parameter(description = "마지막으로 조회한 게시글 ID (첫 페이지일 경우 비움)", example = "15")
            @RequestParam(required = false) Long lastId,
            @Parameter(description = "한 번에 가져올 데이터 개수", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "제목/내용 검색어")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "년도 필터 (예: 2026). semester 미전송 시 무시됨")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "학기 코드 (1=1학기, 2=2학기, 3=여름방학, 4=겨울방학). 미전송 시 전체 학기 조회")
            @RequestParam(required = false) Integer semester,
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(hidden = true) HttpServletRequest request
    );

    @Operation(
            summary = "유사도 정렬 스크롤 조회",
            description = "로그인한 사용자의 체크리스트를 기준으로 유사도가 높은 게시글부터 내림차순 정렬하여 페이지네이션합니다. " +
                    "유사도가 같을 경우 boardId 내림차순으로 정렬합니다. " +
                    "lastPct와 lastBoardId를 기준으로 커서 페이지네이션을 수행합니다."
    )
    ResponseEntity<List<ResponseRoommateSimilarityDto>> getSimilarRoommateBoardListScrollForMe(
            @Parameter(description = "마지막으로 조회한 게시글의 유사도 퍼센트 (첫 페이지일 경우 비움)", example = "95")
            @RequestParam(required = false) Integer lastPct,
            @Parameter(description = "마지막으로 조회한 게시글 ID (첫 페이지일 경우 비움)", example = "42")
            @RequestParam(required = false) Long lastBoardId,
            @Parameter(description = "한 번에 가져올 데이터 개수", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(hidden = true) HttpServletRequest request
    );

    @Operation(
            summary = "룸메이트 매칭 기간/상태 조회",
            description = "현재 매칭 기간(year, semester)과 매칭 상태(OPEN/CLOSED)를 반환합니다. " +
                    "CLOSED이면 매칭 기간 종료 상태로, UI 안내/차단 처리에 사용합니다."
    )
    ResponseEntity<ResponseRoommateMatchingStatusDto> getMatchingStatus();

    @Operation(
            summary = "룸메이트 매칭 선택 가능 기간 목록 조회",
            description = "드롭다운용 — 게시글이 존재하는 학기(중복 제거)와 현재 학기를 최신순으로 반환합니다. " +
                    "각 항목은 year, semester(1~4), label(예: \"2026년 2학기\"), isCurrent 플래그를 포함합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseRoommateMatchingPeriodDto.class)))
            }
    )
    ResponseEntity<List<ResponseRoommateMatchingPeriodDto>> getMatchingPeriods();
}