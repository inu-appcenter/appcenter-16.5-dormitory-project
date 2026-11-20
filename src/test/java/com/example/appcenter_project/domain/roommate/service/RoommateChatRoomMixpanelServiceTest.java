package com.example.appcenter_project.domain.roommate.service;

import com.example.appcenter_project.common.image.service.ImageService;
import com.example.appcenter_project.domain.block.service.BlockService;
import com.example.appcenter_project.domain.roommate.entity.RoommateBoard;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom;
import com.example.appcenter_project.domain.roommate.enums.RoommateMatchingStatus;
import com.example.appcenter_project.domain.roommate.enums.SemesterType;
import com.example.appcenter_project.domain.roommate.fixture.RoommateChattingImageFixture;
import com.example.appcenter_project.domain.roommate.repository.RoommateBoardRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingRoomRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateCheckListRepository;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.mixpanel.ChatMixpanelTracker;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoommateChatRoomMixpanelServiceTest {

    @Mock RoommateChattingRoomRepository roommateChattingRoomRepository;
    @Mock RoommateBoardRepository roommateBoardRepository;
    @Mock UserRepository userRepository;
    @Mock ImageService imageService;
    @Mock RoommateChattingChatService roommateChattingChatService;
    @Mock RoommateMatchingPeriodResolver periodResolver;
    @Mock RoommateCheckListRepository roommateCheckListRepository;
    @Mock BlockService blockService;
    @Mock HttpServletRequest request;
    @Mock ChatMixpanelTracker chatMixpanelTracker;

    @InjectMocks RoommateChattingRoomService roommateChattingRoomService;

    private static final Long HOST_ID = 1L;
    private static final Long GUEST_ID = 2L;

    private void givenBoardAndUsers(User host, User guest, RoommateBoard board) {
        given(board.getUser()).willReturn(host);
        given(board.getYear()).willReturn(2026);
        given(board.getSemester()).willReturn(SemesterType.FIRST);
        given(roommateBoardRepository.findById(10L)).willReturn(Optional.of(board));
        given(userRepository.findById(GUEST_ID)).willReturn(Optional.of(guest));
        given(periodResolver.resolveCurrent(any(LocalDate.class))).willReturn(
                new MatchingPeriod(2026, SemesterType.FIRST, RoommateMatchingStatus.OPEN));
        given(roommateCheckListRepository.findFirstByUserIdAndYearAndSemester(anyLong(), anyInt(), any()))
                .willReturn(Optional.empty());
    }

    @Test
    @DisplayName("[BR-TBD] 채팅방이 새로 생성될 때 chat_room_joined 발화")
    void newRoom_fires() {
        User host = User.createForTest(HOST_ID, "호스트");
        User guest = User.createForTest(GUEST_ID, "게스트");
        givenBoardAndUsers(host, guest, mock(RoommateBoard.class));
        given(roommateChattingRoomRepository.findByGuestAndHostAndBoardYearAndSemester(any(), any(), any(), any()))
                .willReturn(Optional.empty());

        roommateChattingRoomService.createChatRoom(GUEST_ID, 10L);

        then(chatMixpanelTracker).should().trackRoomJoined(any(), any(), org.mockito.ArgumentMatchers.eq("ROOMMATE"));
    }

    @Test
    @DisplayName("[BR-TBD] 같은 쌍의 기존 방을 반환(재요청)하면 발화하지 않는다")
    void existingRoom_noEvent() {
        User host = User.createForTest(HOST_ID, "호스트");
        User guest = User.createForTest(GUEST_ID, "게스트");
        givenBoardAndUsers(host, guest, mock(RoommateBoard.class));
        RoommateChattingRoom existing = RoommateChattingImageFixture.createRoom(host, guest);
        given(roommateChattingRoomRepository.findByGuestAndHostAndBoardYearAndSemester(guest, host, 2026, SemesterType.FIRST))
                .willReturn(Optional.of(existing));

        roommateChattingRoomService.createChatRoom(GUEST_ID, 10L);

        then(chatMixpanelTracker).should(never()).trackRoomJoined(any(), any(), any());
    }

    @Test
    @DisplayName("[BR-TBD] 방 상세 조회 성공 시 chat_room_viewed 발화")
    void detail_fires() {
        User host = User.createForTest(HOST_ID, "호스트");
        User guest = User.createForTest(GUEST_ID, "게스트");
        RoommateChattingRoom room = RoommateChattingImageFixture.createRoom(host, guest);
        given(roommateChattingRoomRepository.findById(room.getId())).willReturn(Optional.of(room));
        given(periodResolver.resolveCurrent(any(LocalDate.class))).willReturn(
                new MatchingPeriod(2026, SemesterType.FIRST, RoommateMatchingStatus.OPEN));

        roommateChattingRoomService.getRoommateChatRoomDetail(GUEST_ID, room.getId(), request);

        then(chatMixpanelTracker).should().trackRoomViewed(GUEST_ID, room.getId(), "ROOMMATE");
    }

    @Test
    @DisplayName("[BR-TBD] 참여자가 아니면 예외이며 chat_room_viewed 를 발화하지 않는다")
    void detail_forbidden_noEvent() {
        User host = User.createForTest(HOST_ID, "호스트");
        User guest = User.createForTest(GUEST_ID, "게스트");
        RoommateChattingRoom room = RoommateChattingImageFixture.createRoom(host, guest);
        given(roommateChattingRoomRepository.findById(room.getId())).willReturn(Optional.of(room));

        assertThatThrownBy(() -> roommateChattingRoomService.getRoommateChatRoomDetail(99L, room.getId(), request))
                .isInstanceOf(CustomException.class);

        then(chatMixpanelTracker).should(never()).trackRoomViewed(any(), any(), any());
    }
}
