package com.example.appcenter_project.domain.roommate.service;

import com.example.appcenter_project.common.image.enums.ImageType;
import com.example.appcenter_project.common.image.service.ImageService;
import com.example.appcenter_project.domain.block.service.BlockService;
import com.example.appcenter_project.domain.roommate.dto.response.ResponseRoommateChatRoomDetailDto;
import com.example.appcenter_project.domain.roommate.dto.response.ResponseRoommateChatRoomDto;
import com.example.appcenter_project.domain.roommate.entity.RoommateBoard;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingChat;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom;
import com.example.appcenter_project.domain.roommate.entity.RoommateCheckList;
import com.example.appcenter_project.domain.roommate.enums.SemesterType;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.domain.roommate.repository.MyRoommateRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateBoardRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateCheckListRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingRoomRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateCheckListRepository;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.example.appcenter_project.global.exception.ErrorCode.*;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RoommateChattingRoomService {

    private final RoommateChattingRoomRepository roommateChattingRoomRepository;
    private final RoommateBoardRepository roommateBoardRepository;
    private final UserRepository userRepository;
    private final ImageService imageService;
    private final RoommateChattingChatService roommateChattingChatService;
    private final MyRoommateRepository myRoommateRepository;
    private final RoommateMatchingPeriodResolver periodResolver;
    private final RoommateCheckListRepository roommateCheckListRepository;
    private final BlockService blockService;


    //채팅방 생성
    @Transactional
    public Long createChatRoom(Long guestId, Long roommateBoardId) throws CustomException {
        // 게시글 조회
        RoommateBoard roommateBoard = roommateBoardRepository.findById(roommateBoardId)
                .orElseThrow(() -> new CustomException(ROOMMATE_BOARD_NOT_FOUND));

        // host는 게시글 작성자
        User host = roommateBoard.getUser();
        User guest = userRepository.findById(guestId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        // 양방향 생성 제한 — 동일 학기 게시글 기준으로 체크해 학기 간 채팅방 재사용 방지
        Integer boardYear = roommateBoard.getYear();
        SemesterType boardSemester = roommateBoard.getSemester();
        Optional<RoommateChattingRoom> existingRoom =
                roommateChattingRoomRepository.findByGuestAndHostAndBoardYearAndSemester(guest, host, boardYear, boardSemester);
        if (existingRoom.isPresent()) {
            return existingRoom.get().getId();
        }
        Optional<RoommateChattingRoom> reversedRoom =
                roommateChattingRoomRepository.findByGuestAndHostAndBoardYearAndSemester(host, guest, boardYear, boardSemester);
        if (reversedRoom.isPresent()) {
            return reversedRoom.get().getId();
        }

        // 자기 자신과 채팅 방지
        if (host.getId().equals(guest.getId())) {
            throw new CustomException(ROOMMATE_CHAT_CANNOT_CHAT_WITH_SELF);
        }

        // 이미 채팅방이 있는지 확인
        boolean exists = roommateChattingRoomRepository.existsByRoommateBoardAndGuest(roommateBoard, guest);
        if (exists) {
            throw new CustomException(DUPLICATE_CHAT_ROOM);
        }

        // 채팅방 생성
        MatchingPeriod period = periodResolver.resolveCurrent(LocalDate.now());
        RoommateCheckList guestChecklist = roommateCheckListRepository
                .findFirstByUserIdAndYearAndSemester(guestId, period.year(), period.semester())
                .orElse(null);
        RoommateChattingRoom chattingRoom = RoommateChattingRoom.builder()
                .roommateBoard(roommateBoard)
                .host(host)
                .guest(guest)
                .hostChecklist(roommateBoard.getRoommateCheckList())
                .guestChecklist(guestChecklist)
                .build();

        roommateChattingRoomRepository.save(chattingRoom);
        return chattingRoom.getId();
    }

    //채팅방 나가기
    @Transactional
    public void leaveChatRoom(Long userId, Long chatRoomId) {
        // 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        // 채팅방 조회
        RoommateChattingRoom chatRoom = roommateChattingRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ROOMMATE_CHAT_ROOM_NOT_FOUND));

        // 채팅방에 속한 두 명(호스트 또는 게스트)만 나가기 가능
        if (!chatRoom.getGuest().getId().equals(user.getId()) && !chatRoom.getHost().getId().equals(user.getId())) {
            throw new CustomException(ROOMMATE_FORBIDDEN_ACCESS);
        }

        // 나간 사람만 플래그 처리 (상대방 채팅 내역 유지)
        if (chatRoom.getHost().getId().equals(user.getId())) {
            chatRoom.leaveAsHost();
        } else {
            chatRoom.leaveAsGuest();
        }

        roommateChattingChatService.sendSystemMessage(chatRoom, user.getName() + "님이 나갔습니다.");

        // 양쪽 모두 나간 경우에만 채팅방 삭제
        if (chatRoom.isBothLeft()) {
            roommateChattingRoomRepository.delete(chatRoom);
        }
    }

    @Transactional(readOnly = true)
    public List<ResponseRoommateChatRoomDto> findRoommateChatRoomListByUser(
            CustomUserDetails userDetails,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        List<RoommateChattingRoom> rooms = roommateChattingRoomRepository.findAllByHostOrGuest(user, user);

        MatchingPeriod current = periodResolver.resolveCurrent(LocalDate.now());

        List<ResponseRoommateChatRoomDto> result = new ArrayList<>();
        int index = 1;

        for (RoommateChattingRoom room : rooms) {
            // 내가 나간 채팅방은 목록에서 제외
            boolean iLeft = (room.getHost().getId().equals(user.getId()) && room.isHostLeft())
                    || (room.getGuest().getId().equals(user.getId()) && room.isGuestLeft());
            if (iLeft) continue;

            String opponentNickname = "익명 " + index++;

            Optional<RoommateChattingChat> lastChat = room.getChattingChatList().stream()
                    .max(Comparator.comparing(RoommateChattingChat::getCreatedDate));

            String lastMessage = lastChat.map(RoommateChattingChat::getContent).orElse("");
            LocalDateTime lastMessageTime = lastChat.map(RoommateChattingChat::getCreatedDate).orElse(null);

            User host = room.getHost();
            User guest = room.getGuest();
            boolean iAmHost = host.getId().equals(user.getId());
            User partner = iAmHost ? guest : host;
            boolean opponentLeft = iAmHost ? room.isGuestLeft() : room.isHostLeft();
            boolean isBlockedByPartner = blockService.isBlockedBy(partner.getId(), user.getId());
            boolean isRoommate = myRoommateRepository
                    .findByUserIdAndRoommateIdAndYearAndSemester(user.getId(), partner.getId(), current.year(), current.semester()).isPresent();

            String hostBoardTitle = room.getRoommateBoard() != null ? room.getRoommateBoard().getTitle() : null;
            String guestBoardTitle = roommateBoardRepository
                    .findByUserIdAndYearAndSemester(guest.getId(), current.year(), current.semester())
                    .map(RoommateBoard::getTitle).orElse(null);
            String myBoardTitle = iAmHost ? hostBoardTitle : guestBoardTitle;
            String opponentBoardTitle = iAmHost ? guestBoardTitle : hostBoardTitle;

            // ImageService의 정적 리소스 URL(fileName)을 사용
            String partnerProfileImageUrl = null;
            try {
                partnerProfileImageUrl =
                        imageService.findImage(ImageType.USER, partner.getId(), request).getImageUrl();
            } catch (Exception e) {
                // 기본이미지 초기화 로직이 있으므로 실패 시 null 허용
                log.warn("partner image url resolve failed. userId={}", partner.getId(), e);
            }

            result.add(ResponseRoommateChatRoomDto.builder()
                    .chatRoomId(room.getId())
                    .opponentNickname(opponentNickname)
                    .lastMessage(lastMessage)
                    .lastMessageTime(lastMessageTime)
                    .partnerName(partner.getName())
                    .partnerId(partner.getId())
                    .partnerProfileImageUrl(partnerProfileImageUrl)
                    .isOpponentLeft(opponentLeft)
                    .isRoommate(isRoommate)
                    .isBlockedByPartner(isBlockedByPartner)
                    .myBoardTitle(myBoardTitle)
                    .opponentBoardTitle(opponentBoardTitle)
                    .build());
        }

        result.sort(Comparator
                .comparing(ResponseRoommateChatRoomDto::isRoommate, Comparator.reverseOrder())
                .thenComparing(
                        ResponseRoommateChatRoomDto::getLastMessageTime,
                        Comparator.nullsLast(Comparator.reverseOrder())));

        return result;
    }


    @Transactional(readOnly = true)
    public ResponseRoommateChatRoomDetailDto getRoommateChatRoomDetail(Long userId, Long chatRoomId, HttpServletRequest request) {
        RoommateChattingRoom room = roommateChattingRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ROOMMATE_CHAT_ROOM_NOT_FOUND));

        boolean isHost = room.getHost().getId().equals(userId);
        boolean isGuest = room.getGuest().getId().equals(userId);
        if (!isHost && !isGuest) {
            throw new CustomException(ROOMMATE_FORBIDDEN_ACCESS);
        }

        User partner = isHost ? room.getGuest() : room.getHost();
        boolean opponentLeft = isHost ? room.isGuestLeft() : room.isHostLeft();
        boolean isBlockedByPartner = blockService.isBlockedBy(partner.getId(), userId);

        MatchingPeriod current = periodResolver.resolveCurrent(LocalDate.now());
        String hostBoardTitle = room.getRoommateBoard() != null ? room.getRoommateBoard().getTitle() : null;
        String guestBoardTitle = roommateBoardRepository
                .findByUserIdAndYearAndSemester(room.getGuest().getId(), current.year(), current.semester())
                .map(RoommateBoard::getTitle).orElse(null);
        String myBoardTitle = isHost ? hostBoardTitle : guestBoardTitle;
        String opponentBoardTitle = isHost ? guestBoardTitle : hostBoardTitle;

        String partnerProfileImageUrl = null;
        try {
            partnerProfileImageUrl = imageService.findImage(ImageType.USER, partner.getId(), request).getImageUrl();
        } catch (Exception e) {
            log.warn("partner image url resolve failed. userId={}", partner.getId(), e);
        }

        return ResponseRoommateChatRoomDetailDto.builder()
                .chatRoomId(room.getId())
                .partnerName(partner.getName())
                .partnerProfileImageUrl(partnerProfileImageUrl)
                .isOpponentLeft(opponentLeft)
                .isBlockedByPartner(isBlockedByPartner)
                .myBoardTitle(myBoardTitle)
                .opponentBoardTitle(opponentBoardTitle)
                .build();
    }

    @Transactional(readOnly = true)
    public RoommateCheckList getOpponentChecklist(Long userId, Long chatRoomId) {
        RoommateChattingRoom chatRoom = roommateChattingRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ROOMMATE_CHAT_ROOM_NOT_FOUND));

        // 본인이 참여 중인지 검증 (나간 유저 포함)
        boolean isHost = chatRoom.getHost().getId().equals(userId);
        boolean isGuest = chatRoom.getGuest().getId().equals(userId);
        if (!isHost && !isGuest) {
            throw new CustomException(ROOMMATE_FORBIDDEN_ACCESS);
        }
        if ((isHost && chatRoom.isHostLeft()) || (isGuest && chatRoom.isGuestLeft())) {
            throw new CustomException(ROOMMATE_FORBIDDEN_ACCESS);
        }

        // 상대방의 현재 학기 체크리스트를 동적으로 조회
        Long opponentId = isHost ? chatRoom.getGuest().getId() : chatRoom.getHost().getId();
        MatchingPeriod current = periodResolver.resolveCurrent(LocalDate.now());
        return roommateCheckListRepository
                .findFirstByUserIdAndYearAndSemester(opponentId, current.year(), current.semester())
                .orElse(null);
    }
}

