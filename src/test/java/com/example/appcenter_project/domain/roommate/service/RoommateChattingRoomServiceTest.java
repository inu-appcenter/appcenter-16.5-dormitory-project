package com.example.appcenter_project.domain.roommate.service;

import com.example.appcenter_project.global.mixpanel.ChatMixpanelTracker;

import com.example.appcenter_project.common.image.service.ImageService;
import com.example.appcenter_project.domain.roommate.entity.RoommateBoard;
import com.example.appcenter_project.domain.roommate.service.RoommateChattingChatService;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom;
import com.example.appcenter_project.domain.roommate.entity.RoommateCheckList;
import com.example.appcenter_project.domain.roommate.enums.RoommateMatchingStatus;
import com.example.appcenter_project.domain.roommate.enums.SemesterType;
import com.example.appcenter_project.domain.roommate.repository.MyRoommateRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateBoardRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateCheckListRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingRoomRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateCheckListRepository;
import com.example.appcenter_project.domain.roommate.enums.SemesterType;
import com.example.appcenter_project.domain.roommate.enums.RoommateMatchingStatus;
import java.time.LocalDate;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static com.example.appcenter_project.global.exception.ErrorCode.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoommateChattingRoomServiceTest {

    @Mock
    RoommateChattingRoomRepository roommateChattingRoomRepository;

    @Mock
    RoommateBoardRepository roommateBoardRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    ImageService imageService;

    @Mock
    RoommateChattingChatService roommateChattingChatService;

    @Mock

    RoommateMatchingPeriodResolver periodResolver;

    @Mock
    RoommateCheckListRepository roommateCheckListRepository;

    @Mock
    ChatMixpanelTracker chatMixpanelTracker;

    @InjectMocks
    RoommateChattingRoomService roommateChattingRoomService;

    private User buildMockUser(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getName()).thenReturn("사용자" + id);
        return user;
    }

    @Test
    @DisplayName("채팅방 생성 - 정상 생성")
    void createChatRoom_정상_생성() throws Exception {
        User host = buildMockUser(1L);
        User guest = buildMockUser(2L);

        RoommateBoard roommateBoard = mock(RoommateBoard.class);
        when(roommateBoard.getId()).thenReturn(10L);
        when(roommateBoard.getUser()).thenReturn(host);
        when(roommateBoardRepository.findById(10L)).thenReturn(Optional.of(roommateBoard));
        when(userRepository.findById(2L)).thenReturn(Optional.of(guest));

        when(roommateBoard.getYear()).thenReturn(2026);
        when(roommateBoard.getSemester()).thenReturn(SemesterType.FIRST);
        when(roommateChattingRoomRepository.findByGuestAndHostAndBoardYearAndSemester(guest, host, 2026, SemesterType.FIRST))
                .thenReturn(Optional.empty());
        when(roommateChattingRoomRepository.findByGuestAndHostAndBoardYearAndSemester(host, guest, 2026, SemesterType.FIRST))
                .thenReturn(Optional.empty());
        when(roommateChattingRoomRepository.existsByRoommateBoardAndGuest(roommateBoard, guest)).thenReturn(false);

        when(periodResolver.resolveCurrent(any())).thenReturn(
                new MatchingPeriod(2026, SemesterType.FIRST, RoommateMatchingStatus.OPEN));
        when(roommateCheckListRepository.findFirstByUserIdAndYearAndSemester(anyLong(), anyInt(), any()))
                .thenReturn(Optional.empty());

        RoommateChattingRoom savedRoom = mock(RoommateChattingRoom.class);
        when(savedRoom.getId()).thenReturn(100L);
        when(roommateChattingRoomRepository.save(any(RoommateChattingRoom.class))).thenReturn(savedRoom);

        roommateChattingRoomService.createChatRoom(2L, 10L);

        verify(roommateChattingRoomRepository).save(any(RoommateChattingRoom.class));
    }

    @Test
    @DisplayName("채팅방 생성 - 게시글 없으면 예외")
    void createChatRoom_게시글없으면_예외() {
        when(roommateBoardRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roommateChattingRoomService.createChatRoom(2L, 99L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ROOMMATE_BOARD_NOT_FOUND);
    }

    @Test
    @DisplayName("채팅방 생성 - 게스트 유저 없으면 예외")
    void createChatRoom_게스트없으면_예외() {
        User host = buildMockUser(1L);
        RoommateBoard roommateBoard = mock(RoommateBoard.class);
        when(roommateBoard.getUser()).thenReturn(host);
        when(roommateBoardRepository.findById(10L)).thenReturn(Optional.of(roommateBoard));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roommateChattingRoomService.createChatRoom(99L, 10L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", USER_NOT_FOUND);
    }

    @Test
    @DisplayName("채팅방 생성 - 자기 자신과 채팅 방지")
    void createChatRoom_자기자신채팅_예외() {
        User host = buildMockUser(1L);

        RoommateBoard roommateBoard = mock(RoommateBoard.class);
        when(roommateBoard.getUser()).thenReturn(host);
        when(roommateBoard.getYear()).thenReturn(2026);
        when(roommateBoard.getSemester()).thenReturn(SemesterType.FIRST);
        when(roommateBoardRepository.findById(10L)).thenReturn(Optional.of(roommateBoard));
        when(userRepository.findById(1L)).thenReturn(Optional.of(host)); // guest = host (같은 사용자)

        when(roommateChattingRoomRepository.findByGuestAndHostAndBoardYearAndSemester(any(), any(), anyInt(), any()))
                .thenReturn(Optional.empty());
        when(roommateChattingRoomRepository.existsByRoommateBoardAndGuest(roommateBoard, host)).thenReturn(false);

        assertThatThrownBy(() -> roommateChattingRoomService.createChatRoom(1L, 10L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ROOMMATE_CHAT_CANNOT_CHAT_WITH_SELF);
    }

    @Test
    @DisplayName("채팅방 생성 - 같은 학기 채팅방 존재하면 기존 채팅방 ID 반환")
    void createChatRoom_이미존재시_기존방ID반환() throws Exception {
        User host = buildMockUser(1L);
        User guest = buildMockUser(2L);

        RoommateBoard roommateBoard = mock(RoommateBoard.class);
        when(roommateBoard.getUser()).thenReturn(host);
        when(roommateBoard.getYear()).thenReturn(2026);
        when(roommateBoard.getSemester()).thenReturn(SemesterType.FIRST);
        when(roommateBoardRepository.findById(10L)).thenReturn(Optional.of(roommateBoard));
        when(userRepository.findById(2L)).thenReturn(Optional.of(guest));

        RoommateChattingRoom existingRoom = mock(RoommateChattingRoom.class);
        when(existingRoom.getId()).thenReturn(100L);
        when(roommateChattingRoomRepository.findByGuestAndHostAndBoardYearAndSemester(guest, host, 2026, SemesterType.FIRST))
                .thenReturn(Optional.of(existingRoom));

        Long result = roommateChattingRoomService.createChatRoom(2L, 10L);

        assertThat(result).isEqualTo(100L);
        verify(roommateChattingRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("채팅방 생성 - 이전 학기 채팅방이 있어도 새 학기에는 신규 채팅방 생성")
    void createChatRoom_이전학기채팅방있어도_새학기에_신규생성() throws Exception {
        User host = buildMockUser(1L);
        User guest = buildMockUser(2L);

        RoommateBoard newBoard = mock(RoommateBoard.class);
        when(newBoard.getUser()).thenReturn(host);
        when(newBoard.getYear()).thenReturn(2026);
        when(newBoard.getSemester()).thenReturn(SemesterType.SECOND);
        when(newBoard.getRoommateCheckList()).thenReturn(null);
        when(roommateBoardRepository.findById(20L)).thenReturn(Optional.of(newBoard));
        when(userRepository.findById(2L)).thenReturn(Optional.of(guest));

        // 현재 학기(2026-2)에는 채팅방 없음
        when(roommateChattingRoomRepository.findByGuestAndHostAndBoardYearAndSemester(guest, host, 2026, SemesterType.SECOND))
                .thenReturn(Optional.empty());
        when(roommateChattingRoomRepository.findByGuestAndHostAndBoardYearAndSemester(host, guest, 2026, SemesterType.SECOND))
                .thenReturn(Optional.empty());
        when(roommateChattingRoomRepository.existsByRoommateBoardAndGuest(newBoard, guest)).thenReturn(false);

        when(periodResolver.resolveCurrent(any())).thenReturn(
                new MatchingPeriod(2026, SemesterType.SECOND, RoommateMatchingStatus.OPEN));
        when(roommateCheckListRepository.findFirstByUserIdAndYearAndSemester(anyLong(), anyInt(), any()))
                .thenReturn(Optional.empty());

        RoommateChattingRoom savedRoom = mock(RoommateChattingRoom.class);
        when(savedRoom.getId()).thenReturn(200L);
        when(roommateChattingRoomRepository.save(any(RoommateChattingRoom.class))).thenReturn(savedRoom);

        roommateChattingRoomService.createChatRoom(2L, 20L);

        verify(roommateChattingRoomRepository).save(any(RoommateChattingRoom.class));
    }

    @Test
    @DisplayName("채팅방 나가기 - 한 명만 나가면 플래그 처리, 삭제 안 함")
    void leaveChatRoom_정상_나가기() {
        User user = buildMockUser(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        User host = buildMockUser(1L);
        RoommateChattingRoom chatRoom = mock(RoommateChattingRoom.class);
        when(chatRoom.getGuest()).thenReturn(user); // user는 guest
        when(chatRoom.getHost()).thenReturn(host);
        when(chatRoom.isBothLeft()).thenReturn(false); // 아직 host는 안 나감
        when(roommateChattingRoomRepository.findById(100L)).thenReturn(Optional.of(chatRoom));

        roommateChattingRoomService.leaveChatRoom(2L, 100L);

        verify(chatRoom).leaveAsGuest();
        verify(roommateChattingRoomRepository, never()).delete(any());
    }

    @Test
    @DisplayName("채팅방 나가기 - 둘 다 나가면 채팅방 삭제")
    void leaveChatRoom_둘다나가면_삭제() {
        User user = buildMockUser(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        User host = buildMockUser(1L);
        RoommateChattingRoom chatRoom = mock(RoommateChattingRoom.class);
        when(chatRoom.getGuest()).thenReturn(user); // user는 guest
        when(chatRoom.getHost()).thenReturn(host);
        when(chatRoom.isBothLeft()).thenReturn(true); // host도 이미 나간 상태
        when(roommateChattingRoomRepository.findById(100L)).thenReturn(Optional.of(chatRoom));

        roommateChattingRoomService.leaveChatRoom(2L, 100L);

        verify(chatRoom).leaveAsGuest();
        verify(roommateChattingRoomRepository).delete(chatRoom);
    }

    @Test
    @DisplayName("채팅방 나가기 - 채팅방 없으면 예외")
    void leaveChatRoom_채팅방없으면_예외() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(mock(User.class)));
        when(roommateChattingRoomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roommateChattingRoomService.leaveChatRoom(2L, 99L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ROOMMATE_CHAT_ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("채팅방 나가기 - 채팅방 참여자가 아니면 예외")
    void leaveChatRoom_참여자아님_예외() {
        User user = buildMockUser(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        User host = buildMockUser(1L);
        User guest = buildMockUser(2L);
        RoommateChattingRoom chatRoom = mock(RoommateChattingRoom.class);
        when(chatRoom.getHost()).thenReturn(host);
        when(chatRoom.getGuest()).thenReturn(guest);
        when(roommateChattingRoomRepository.findById(100L)).thenReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> roommateChattingRoomService.leaveChatRoom(3L, 100L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ROOMMATE_FORBIDDEN_ACCESS);
    }

    @Test
    @DisplayName("상대방 체크리스트 조회 - host 조회 시 guest의 현재 학기 체크리스트 반환")
    void getOpponentChecklist_host가조회시_guest현재학기체크리스트반환() {
        User host = buildMockUser(1L);
        User guest = buildMockUser(2L);
        RoommateCheckList guestChecklist = mock(RoommateCheckList.class);

        RoommateChattingRoom chatRoom = mock(RoommateChattingRoom.class);
        when(chatRoom.getHost()).thenReturn(host);
        when(chatRoom.getGuest()).thenReturn(guest);
        when(roommateChattingRoomRepository.findById(100L)).thenReturn(Optional.of(chatRoom));

        when(periodResolver.resolveCurrent(any(LocalDate.class)))
                .thenReturn(new MatchingPeriod(2026, SemesterType.FIRST, RoommateMatchingStatus.OPEN));
        when(roommateCheckListRepository.findFirstByUserIdAndYearAndSemester(2L, 2026, SemesterType.FIRST))
                .thenReturn(Optional.of(guestChecklist));

        RoommateCheckList result = roommateChattingRoomService.getOpponentChecklist(1L, 100L);

        assertThat(result).isEqualTo(guestChecklist);
    }

    @Test
    @DisplayName("상대방 체크리스트 조회 - 상대가 현재 학기 체크리스트 없으면 null 반환")
    void getOpponentChecklist_현재학기없으면_null반환() {
        User host = buildMockUser(1L);
        User guest = buildMockUser(2L);

        RoommateChattingRoom chatRoom = mock(RoommateChattingRoom.class);
        when(chatRoom.getHost()).thenReturn(host);
        when(chatRoom.getGuest()).thenReturn(guest);
        when(roommateChattingRoomRepository.findById(100L)).thenReturn(Optional.of(chatRoom));

        when(periodResolver.resolveCurrent(any(LocalDate.class)))
                .thenReturn(new MatchingPeriod(2026, SemesterType.FIRST, RoommateMatchingStatus.OPEN));
        when(roommateCheckListRepository.findFirstByUserIdAndYearAndSemester(2L, 2026, SemesterType.FIRST))
                .thenReturn(Optional.empty());

        RoommateCheckList result = roommateChattingRoomService.getOpponentChecklist(1L, 100L);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("상대방 체크리스트 조회 - 참여자가 아니면 예외")
    void getOpponentChecklist_참여자아님_예외() {
        User host = buildMockUser(1L);
        User guest = buildMockUser(2L);

        RoommateChattingRoom chatRoom = mock(RoommateChattingRoom.class);
        when(chatRoom.getHost()).thenReturn(host);
        when(chatRoom.getGuest()).thenReturn(guest);
        when(roommateChattingRoomRepository.findById(100L)).thenReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> roommateChattingRoomService.getOpponentChecklist(3L, 100L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ROOMMATE_FORBIDDEN_ACCESS);
    }
}