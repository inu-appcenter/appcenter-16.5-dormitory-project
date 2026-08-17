package com.example.appcenter_project.domain.roommate.controller;

import com.example.appcenter_project.common.metrics.annotation.TrackApi;
import com.example.appcenter_project.domain.roommate.dto.request.RequestRoommateFormDto;
import com.example.appcenter_project.domain.roommate.dto.response.*;
import com.example.appcenter_project.domain.roommate.enums.SemesterType;
import com.example.appcenter_project.global.security.CustomUserDetails;
import com.example.appcenter_project.domain.roommate.service.MyRoommateService;
import com.example.appcenter_project.domain.roommate.service.RoommateQueryService;
import com.example.appcenter_project.domain.roommate.service.RoommateService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roommates")
@RequiredArgsConstructor
public class RoommateController implements RoommateApiSpecification{

    private final RoommateService roommateService;
    private final MyRoommateService myRoommateService;
    private final RoommateQueryService roommateQueryService;

    @TrackApi
    @Override
    @PostMapping
    public ResponseEntity<ResponseRoommatePostDto> createRoommatePost(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails userDetails,
            @RequestBody RequestRoommateFormDto requestDto
    ) {
        Long userId = userDetails != null ? userDetails.getId() : 0L;
        ResponseRoommatePostDto responseDto = roommateService.createRoommateCheckListandBoard(requestDto, userId);
        return ResponseEntity.status(201).body(responseDto);
    }

    @TrackApi
    @Override
    @GetMapping("/list")
    public ResponseEntity<List<ResponseRoommatePostDto>> getRoommateBoardList(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails userDetails,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(roommateService.getRoommateBoardList(userId, request));
    }

    @TrackApi
    @Override
    @GetMapping("/{boardId}")
    public ResponseEntity<ResponseRoommatePostDto> getRoommateBoardDetail(
            @PathVariable Long boardId,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails userDetails,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(roommateService.getRoommateBoardDetail(boardId, userId, request));
    }

    @TrackApi
    @Override
    @GetMapping("/similar")
    public ResponseEntity<List<ResponseRoommateSimilarityDto>> getSimilarRoommates(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        Long userId = userDetails.getId();
        return ResponseEntity.ok(roommateService.getSimilarRoommateBoards(userId, request));
    }

    @Override
    @PutMapping("/{boardId}")
    public ResponseEntity<ResponseRoommatePostDto> updateRoommateCheckListAndBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails userDetails,
            @RequestBody RequestRoommateFormDto requestDto,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        Long userId = userDetails != null ? userDetails.getId() : 0L;
        ResponseRoommatePostDto updated =
                roommateService.updateRoommateChecklistAndBoard(requestDto, boardId, userId, request);
        return ResponseEntity.ok(updated);
    }

    @Override
    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteRoommateBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails userDetails
    ) {
        Long userId = userDetails != null ? userDetails.getId() : 0L;
        roommateService.deleteRoommateBoard(boardId, userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/{boardId}/like")
    public ResponseEntity<Integer> plusLike(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long boardId
    ) {
        Integer likeCount = roommateService.likePlusRoommateBoard(userDetails.getId(), boardId);
        return ResponseEntity.ok(likeCount);
    }

    @Override
    @DeleteMapping("/{boardId}/like")
    public ResponseEntity<Integer> minusLike(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long boardId
    ) {
        Integer likeCount = roommateService.likeMinusRoommateBoard(userDetails.getId(), boardId);
        return ResponseEntity.ok(likeCount);
    }

    @Override
    @GetMapping("/{boardId}/owner-matched")
    public ResponseEntity<Boolean> isBoardOwnerMatched(@PathVariable Long boardId) {
        boolean matched = roommateService.isRoommateBoardOwnerMatched(boardId);
        return ResponseEntity.ok(matched);
    }

    @Override
    @GetMapping("/{boardId}/liked")
    public ResponseEntity<Boolean> isRoommateBoardLiked(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getId();
        boolean isLiked = roommateService.isRoommateBoardLikedByUser(boardId, userId);
        return ResponseEntity.ok(isLiked);
    }

    @TrackApi
    @Override
    @GetMapping("/my-checklist/board-id")
    public ResponseEntity<ResponseMyRoommateBoardIdDto> getMyBoardId(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(roommateService.getMyBoardId(userDetails.getId()));
    }

    @TrackApi
    @Override
    @GetMapping("/my-checklist")
    public ResponseEntity<ResponseRoommateCheckListDto> getMyCheckList(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(roommateService.getMyCheckList(userDetails.getId()));
    }

    @TrackApi
    @Override
    @GetMapping("/my-checklist/previous")
    public ResponseEntity<ResponseRoommateCheckListDto> getPreviousCheckListContent(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(roommateService.getPreviousCheckListContent(userDetails.getId()));
    }

    @GetMapping("/latest10/random")
    public ResponseEntity<ResponseRoommatePostDto> getRandomFromLatest10(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        Long userId = userDetails.getId();
        return ResponseEntity.ok(roommateService.getRandomFromLatest10(userId, request));
    }

    @GetMapping("/list/scroll")
    public ResponseEntity<List<ResponseRoommatePostDto>> getRoommateBoardListScroll(
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer semester,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails userDetails,
            HttpServletRequest request
    ) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        SemesterType semesterType = SemesterType.fromCodeOrNull(semester);
        return ResponseEntity.ok(roommateService.getRoommateBoardListScroll(userId, request, lastId, size, keyword, year, semesterType));
    }

    @GetMapping("/list/similar/scroll/me")
    public ResponseEntity<List<ResponseRoommateSimilarityDto>> getSimilarRoommateBoardListScrollForMe(
            @RequestParam(required = false) Integer lastPct,    // 마지막 유사도 퍼센트
            @RequestParam(required = false) Long lastBoardId,   // 마지막 boardId
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                roommateService.getSimilarRoommateBoardListScrollForMe(
                        request,
                        userDetails.getId(),
                        lastPct,
                        lastBoardId,
                        size
                )
        );
    }

    @TrackApi
    @Override
    @GetMapping("/matching-status")
    public ResponseEntity<ResponseRoommateMatchingStatusDto> getMatchingStatus() {
        return ResponseEntity.ok(roommateService.getMatchingStatus());
    }

    @TrackApi
    @Override
    @GetMapping("/matching-periods")
    public ResponseEntity<List<ResponseRoommateMatchingPeriodDto>> getMatchingPeriods() {
        return ResponseEntity.ok(roommateService.getMatchingPeriods());
    }

}
