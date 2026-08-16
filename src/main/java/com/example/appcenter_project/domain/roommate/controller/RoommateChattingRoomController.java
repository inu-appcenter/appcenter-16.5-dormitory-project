package com.example.appcenter_project.domain.roommate.controller;

import com.example.appcenter_project.common.metrics.annotation.TrackApi;
import com.example.appcenter_project.domain.roommate.dto.response.ResponseRoommateChatRoomDetailDto;
import com.example.appcenter_project.domain.roommate.dto.response.ResponseRoommateChatRoomDto;
import com.example.appcenter_project.domain.roommate.dto.response.ResponseRoommateCheckListDto;
import com.example.appcenter_project.domain.roommate.entity.RoommateCheckList;
import com.example.appcenter_project.global.security.CustomUserDetails;
import com.example.appcenter_project.domain.roommate.service.RoommateChattingRoomService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/roommate-chatting-room")
@RequiredArgsConstructor
public class RoommateChattingRoomController implements RoommateChattingRoomApiSpecification{

    private final RoommateChattingRoomService roommateChattingRoomService;

    // 게스트가 채팅방 참여 요청
    @TrackApi
    @Override
    @PostMapping("/board/{roommateBoardId}")
    public ResponseEntity<Long> createChatRoom(@AuthenticationPrincipal CustomUserDetails user,
                                               @PathVariable Long roommateBoardId) {
        Long roomId = roommateChattingRoomService.createChatRoom(user.getId(), roommateBoardId);
        return ResponseEntity.status(201).body(roomId);
    }

    // 채팅방 나가기 (게스트 기준)
    @Override
    @DeleteMapping("/{chatRoomId}")
    public ResponseEntity<Void> leaveChatRoom(@AuthenticationPrincipal CustomUserDetails user,
                                              @PathVariable Long chatRoomId) {
        roommateChattingRoomService.leaveChatRoom(user.getId(), chatRoomId);
        return ResponseEntity.status(CREATED).build();
    }

    // 채팅방 목록 조회
    @TrackApi
    @Override
    @GetMapping
    public ResponseEntity<List<ResponseRoommateChatRoomDto>> getRoommateChatRoomList(
            @AuthenticationPrincipal CustomUserDetails user,
            HttpServletRequest request // 추가
    ) {
        List<ResponseRoommateChatRoomDto> chatRooms =
                roommateChattingRoomService.findRoommateChatRoomListByUser(user, request); // 변경
        return ResponseEntity.ok(chatRooms);
    }

    @Override
    @GetMapping("/{chatRoomId}")
    public ResponseEntity<ResponseRoommateChatRoomDetailDto> getRoommateChatRoomDetail(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long chatRoomId,
            HttpServletRequest request) {
        return ResponseEntity.ok(roommateChattingRoomService.getRoommateChatRoomDetail(user.getId(), chatRoomId, request));
    }

    //상대방 체크리스트 확인
    @Override
    @GetMapping("/roommate-chatrooms/{chatRoomId}/opponent-checklist")
    public ResponseEntity<ResponseRoommateCheckListDto> getOpponentChecklist(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long chatRoomId
    ) {
        RoommateCheckList checklist = roommateChattingRoomService.getOpponentChecklist(user.getId(), chatRoomId);
        return ResponseEntity.ok(ResponseRoommateCheckListDto.from(checklist));
    }
}