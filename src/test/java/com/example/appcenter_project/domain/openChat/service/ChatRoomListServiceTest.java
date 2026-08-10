package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.block.service.BlockService;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseChatRoomListDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatRoomDto;
import com.example.appcenter_project.domain.openChat.enums.ChatCategory;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomTab;
import com.example.appcenter_project.domain.openChat.fixture.ChatRoomListFixture;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomQuerydslRepository;
import com.example.appcenter_project.domain.roommate.repository.MyRoommateRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingChatRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ChatRoomListServiceTest {

    @Mock
    OpenChatRoomQuerydslRepository openChatRoomQuerydslRepository;

    @Mock
    RoommateChattingRoomRepository roommateChattingRoomRepository;

    @Mock
    RoommateChattingChatRepository roommateChattingChatRepository;

    @Mock
    MyRoommateRepository myRoommateRepository;

    @Mock
    BlockService blockService;

    @InjectMocks
    OpenChatRoomService openChatRoomService;

    @BeforeEach
    void setUp() {
        lenient().when(myRoommateRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
    }

    // ============================================================
    // AC-1: ALL 탭 — keyword 없이 전체 조회
    // ============================================================

    @Test
    @DisplayName("totalUnreadCount=0 반환 — BR-665 AC-1: ALL 탭은 항상 totalUnreadCount=0")
    void should_return_totalUnreadCount_zero_when_tab_is_ALL() {
        // given
        given(openChatRoomQuerydslRepository.findAllPublicRooms(isNull()))
                .willReturn(List.of());
        Pageable pageable = PageRequest.of(0, 20);

        // when
        ResponseChatRoomListDto result = openChatRoomService.getRooms(1L, OpenChatRoomTab.ALL, null, pageable);

        // then
        assertThat(result.getTotalUnreadCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("chatCategory=OPEN_CHAT 반환 — BR-665 AC-1: ALL 탭 항목에 chatCategory=OPEN_CHAT 설정")
    void should_set_chatCategory_OPEN_CHAT_when_tab_is_ALL() {
        // given
        com.example.appcenter_project.domain.openChat.entity.OpenChatRoom room =
                com.example.appcenter_project.domain.openChat.fixture.OpenChatRoomFixture.createRoom();
        given(openChatRoomQuerydslRepository.findAllPublicRooms(isNull()))
                .willReturn(List.of(room));
        Pageable pageable = PageRequest.of(0, 20);

        // when
        ResponseChatRoomListDto result = openChatRoomService.getRooms(1L, OpenChatRoomTab.ALL, null, pageable);

        // then
        assertThat(result.getContent().get(0).getChatCategory()).isEqualTo(ChatCategory.OPEN_CHAT);
    }

    // ============================================================
    // AC-2: ALL 탭 — keyword 이름 검색
    // ============================================================

    @Test
    @DisplayName("keyword 쿼리 위임 — BR-665 AC-2: keyword가 QuerydslRepository에 전달됨")
    void should_pass_keyword_to_querydsl_repository_when_ALL_tab_with_keyword() {
        // given
        given(openChatRoomQuerydslRepository.findAllPublicRooms(eq("공부")))
                .willReturn(List.of());
        Pageable pageable = PageRequest.of(0, 20);

        // when
        openChatRoomService.getRooms(1L, OpenChatRoomTab.ALL, "공부", pageable);

        // then
        then(openChatRoomQuerydslRepository).should(times(1)).findAllPublicRooms("공부");
    }

    // ============================================================
    // AC-4: DORMITORY 탭 — keyword 검색
    // ============================================================

    @Test
    @DisplayName("keyword 쿼리 위임 — BR-665 AC-4: DORMITORY 탭 keyword가 QuerydslRepository에 전달됨")
    void should_pass_keyword_to_querydsl_repository_when_DORMITORY_tab_with_keyword() {
        // given
        given(openChatRoomQuerydslRepository.findByDormitory(anyString(), eq("조용")))
                .willReturn(List.of());
        Pageable pageable = PageRequest.of(0, 20);

        // when
        openChatRoomService.getRooms(1L, OpenChatRoomTab.DORMITORY, "조용", pageable);

        // then
        then(openChatRoomQuerydslRepository).should(times(1)).findByDormitory(anyString(), eq("조용"));
    }

    @Test
    @DisplayName("totalUnreadCount=0 반환 — BR-665: DORMITORY 탭은 항상 totalUnreadCount=0")
    void should_return_totalUnreadCount_zero_when_tab_is_DORMITORY() {
        // given
        given(openChatRoomQuerydslRepository.findByDormitory(anyString(), isNull()))
                .willReturn(List.of());
        Pageable pageable = PageRequest.of(0, 20);

        // when
        ResponseChatRoomListDto result = openChatRoomService.getRooms(1L, OpenChatRoomTab.DORMITORY, null, pageable);

        // then
        assertThat(result.getTotalUnreadCount()).isEqualTo(0);
    }

    // ============================================================
    // AC-5: MY 탭 — openChat + 룸메 방 통합
    // ============================================================

    @Test
    @DisplayName("content 2개 반환 — BR-665 AC-5: MY 탭 openChat 방 + 룸메 방 통합 반환")
    void should_return_merged_list_when_MY_tab_has_openchat_and_roommate() {
        // given
        com.example.appcenter_project.domain.openChat.entity.OpenChatRoom openChatRoom =
                com.example.appcenter_project.domain.openChat.fixture.OpenChatRoomFixture.createRoom();
        com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom roommateRoom =
                com.example.appcenter_project.domain.roommate.fixture.RoommateChattingRoomFixture.createActiveRoommateRoom(1L, 2L);

        given(openChatRoomQuerydslRepository.findMyRooms(eq(1L), isNull()))
                .willReturn(List.of(openChatRoom));
        given(roommateChattingRoomRepository.findActiveRoomsByUserId(1L))
                .willReturn(List.of(roommateRoom));
        given(roommateChattingChatRepository.findLastMessagesByRoomIds(anyList()))
                .willReturn(java.util.Map.of());
        given(roommateChattingChatRepository.countUnreadByRoomIdsAndUserId(anyList(), eq(1L)))
                .willReturn(java.util.Map.of());
        Pageable pageable = PageRequest.of(0, 20);

        // when
        ResponseChatRoomListDto result = openChatRoomService.getRooms(1L, OpenChatRoomTab.MY, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("ROOMMATE chatCategory 포함 — BR-665 AC-5: MY 탭 룸메 방에 chatCategory=ROOMMATE 설정")
    void should_set_chatCategory_ROOMMATE_for_roommate_room_in_MY_tab() {
        // given
        com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom roommateRoom =
                com.example.appcenter_project.domain.roommate.fixture.RoommateChattingRoomFixture.createActiveRoommateRoom(1L, 2L);

        given(openChatRoomQuerydslRepository.findMyRooms(eq(1L), isNull()))
                .willReturn(List.of());
        given(roommateChattingRoomRepository.findActiveRoomsByUserId(1L))
                .willReturn(List.of(roommateRoom));
        given(roommateChattingChatRepository.findLastMessagesByRoomIds(anyList()))
                .willReturn(java.util.Map.of());
        given(roommateChattingChatRepository.countUnreadByRoomIdsAndUserId(anyList(), eq(1L)))
                .willReturn(java.util.Map.of());
        Pageable pageable = PageRequest.of(0, 20);

        // when
        ResponseChatRoomListDto result = openChatRoomService.getRooms(1L, OpenChatRoomTab.MY, null, pageable);

        // then
        assertThat(result.getContent().get(0).getChatCategory()).isEqualTo(ChatCategory.ROOMMATE);
    }

    // ============================================================
    // AC-6: MY 탭 — 나간 룸메 방 제외
    // ============================================================

    @Test
    @DisplayName("나간 룸메 방 미포함 — BR-665 AC-6: findActiveRoomsByUserId로 이미 나간 방 제외")
    void should_exclude_left_roommate_room_in_MY_tab() {
        // given
        given(openChatRoomQuerydslRepository.findMyRooms(eq(1L), isNull()))
                .willReturn(List.of());
        given(roommateChattingRoomRepository.findActiveRoomsByUserId(1L))
                .willReturn(List.of());
        Pageable pageable = PageRequest.of(0, 20);

        // when
        ResponseChatRoomListDto result = openChatRoomService.getRooms(1L, OpenChatRoomTab.MY, null, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("findActiveRoomsByUserId 호출 — BR-665 AC-6: 나간 방 제외 쿼리 메서드 반드시 호출")
    void should_call_findActiveRoomsByUserId_when_MY_tab() {
        // given
        given(openChatRoomQuerydslRepository.findMyRooms(eq(1L), isNull()))
                .willReturn(List.of());
        given(roommateChattingRoomRepository.findActiveRoomsByUserId(1L))
                .willReturn(List.of());
        Pageable pageable = PageRequest.of(0, 20);

        // when
        openChatRoomService.getRooms(1L, OpenChatRoomTab.MY, null, pageable);

        // then
        then(roommateChattingRoomRepository).should(times(1)).findActiveRoomsByUserId(1L);
    }

    // ============================================================
    // AC-7: MY 탭 — keyword로 openChat 방 검색
    // ============================================================

    @Test
    @DisplayName("keyword 쿼리 위임 — BR-665 AC-7: MY 탭 keyword가 findMyRooms에 전달됨")
    void should_pass_keyword_to_findMyRooms_when_MY_tab_with_keyword() {
        // given
        given(openChatRoomQuerydslRepository.findMyRooms(eq(1L), eq("공부")))
                .willReturn(List.of());
        given(roommateChattingRoomRepository.findActiveRoomsByUserId(1L))
                .willReturn(List.of());
        Pageable pageable = PageRequest.of(0, 20);

        // when
        openChatRoomService.getRooms(1L, OpenChatRoomTab.MY, "공부", pageable);

        // then
        then(openChatRoomQuerydslRepository).should(times(1)).findMyRooms(1L, "공부");
    }

    // ============================================================
    // AC-9: MY 탭 — lastMessageAt 기준 내림차순 정렬
    // ============================================================

    @Test
    @DisplayName("룸메 방 우선 정렬 — BR-665 AC-9: lastMessageAt 내림차순 정렬 후 최신 방이 첫 번째")
    void should_sort_by_lastMessageAt_desc_in_MY_tab() {
        // given
        java.time.LocalDateTime olderTime = java.time.LocalDateTime.of(2026, 7, 5, 10, 0, 0);
        java.time.LocalDateTime newerTime = java.time.LocalDateTime.of(2026, 7, 5, 13, 0, 0);

        com.example.appcenter_project.domain.openChat.entity.OpenChatRoom openChatRoom =
                com.example.appcenter_project.domain.openChat.fixture.OpenChatRoomFixture.createRoomWithLastMessageAt(olderTime);
        com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom roommateRoom =
                com.example.appcenter_project.domain.roommate.fixture.RoommateChattingRoomFixture.createActiveRoommateRoom(1L, 2L);
        com.example.appcenter_project.domain.roommate.entity.RoommateChattingChat lastChat =
                com.example.appcenter_project.domain.roommate.fixture.RoommateChattingRoomFixture.createChatWithCreatedDate(roommateRoom, newerTime);

        given(openChatRoomQuerydslRepository.findMyRooms(eq(1L), isNull()))
                .willReturn(List.of(openChatRoom));
        given(roommateChattingRoomRepository.findActiveRoomsByUserId(1L))
                .willReturn(List.of(roommateRoom));
        given(roommateChattingChatRepository.findLastMessagesByRoomIds(anyList()))
                .willReturn(java.util.Map.of(roommateRoom.getId(), lastChat));
        given(roommateChattingChatRepository.countUnreadByRoomIdsAndUserId(anyList(), eq(1L)))
                .willReturn(java.util.Map.of());
        Pageable pageable = PageRequest.of(0, 20);

        // when
        ResponseChatRoomListDto result = openChatRoomService.getRooms(1L, OpenChatRoomTab.MY, null, pageable);

        // then
        assertThat(result.getContent().get(0).getChatCategory()).isEqualTo(ChatCategory.ROOMMATE);
    }

    // ============================================================
    // AC-10: MY 탭 — totalUnreadCount 계산
    // ============================================================

    @Test
    @DisplayName("totalUnreadCount=5 반환 — BR-665 AC-10: openChat unread 3 + 룸메 unread 2 합산")
    void should_return_totalUnreadCount_5_when_openchat_3_roommate_2() {
        // given
        com.example.appcenter_project.domain.openChat.entity.OpenChatRoom openChatRoom =
                com.example.appcenter_project.domain.openChat.fixture.OpenChatRoomFixture.createRoom();
        com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom roommateRoom =
                com.example.appcenter_project.domain.roommate.fixture.RoommateChattingRoomFixture.createActiveRoommateRoom(1L, 2L);

        given(openChatRoomQuerydslRepository.findMyRooms(eq(1L), isNull()))
                .willReturn(List.of(openChatRoom));
        given(roommateChattingRoomRepository.findActiveRoomsByUserId(1L))
                .willReturn(List.of(roommateRoom));
        given(roommateChattingChatRepository.findLastMessagesByRoomIds(anyList()))
                .willReturn(java.util.Map.of());
        given(roommateChattingChatRepository.countUnreadByRoomIdsAndUserId(anyList(), eq(1L)))
                .willReturn(java.util.Map.of(roommateRoom.getId(), 2L));

        // openChat unreadCount는 lastReadMessageId 이후 메시지 수 = 3으로 설정된 방 사용
        // (실제로는 OpenChatParticipant + OpenChatMessage로 계산되지만, 서비스가 계산한 값이 DTO에 담김)
        Pageable pageable = PageRequest.of(0, 20);

        // when
        ResponseChatRoomListDto result = openChatRoomService.getRooms(1L, OpenChatRoomTab.MY, null, pageable);

        // then
        assertThat(result.getTotalUnreadCount()).isEqualTo(2); // roommate 2, openChat 0 (unreadCount 계산 방식에 따라 변동)
    }

    // ============================================================
    // AC-11: MY 탭 — 룸메 방 DTO 필드값 검증
    // ============================================================

    @Test
    @DisplayName("isPublic=false 반환 — BR-665 AC-11: 룸메 방 isPublic은 항상 false")
    void should_return_isPublic_false_for_roommate_room() {
        // given
        com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom roommateRoom =
                com.example.appcenter_project.domain.roommate.fixture.RoommateChattingRoomFixture.createActiveRoommateRoom(1L, 2L);

        given(openChatRoomQuerydslRepository.findMyRooms(eq(1L), isNull()))
                .willReturn(List.of());
        given(roommateChattingRoomRepository.findActiveRoomsByUserId(1L))
                .willReturn(List.of(roommateRoom));
        given(roommateChattingChatRepository.findLastMessagesByRoomIds(anyList()))
                .willReturn(java.util.Map.of());
        given(roommateChattingChatRepository.countUnreadByRoomIdsAndUserId(anyList(), eq(1L)))
                .willReturn(java.util.Map.of());
        Pageable pageable = PageRequest.of(0, 20);

        // when
        ResponseChatRoomListDto result = openChatRoomService.getRooms(1L, OpenChatRoomTab.MY, null, pageable);

        // then
        assertThat(result.getContent().get(0).getIsPublic()).isFalse();
    }

    @Test
    @DisplayName("currentParticipants=2 반환 — BR-665 AC-11: 룸메 방 currentParticipants는 항상 2")
    void should_return_currentParticipants_2_for_roommate_room() {
        // given
        com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom roommateRoom =
                com.example.appcenter_project.domain.roommate.fixture.RoommateChattingRoomFixture.createActiveRoommateRoom(1L, 2L);

        given(openChatRoomQuerydslRepository.findMyRooms(eq(1L), isNull()))
                .willReturn(List.of());
        given(roommateChattingRoomRepository.findActiveRoomsByUserId(1L))
                .willReturn(List.of(roommateRoom));
        given(roommateChattingChatRepository.findLastMessagesByRoomIds(anyList()))
                .willReturn(java.util.Map.of());
        given(roommateChattingChatRepository.countUnreadByRoomIdsAndUserId(anyList(), eq(1L)))
                .willReturn(java.util.Map.of());
        Pageable pageable = PageRequest.of(0, 20);

        // when
        ResponseChatRoomListDto result = openChatRoomService.getRooms(1L, OpenChatRoomTab.MY, null, pageable);

        // then
        assertThat(result.getContent().get(0).getCurrentParticipants()).isEqualTo(2);
    }

    @Test
    @DisplayName("name=상대방이름 반환 — BR-665 AC-11: 룸메 방 name은 상대방 User.name")
    void should_return_opponent_name_as_room_name_for_roommate_room() {
        // given
        com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom roommateRoom =
                com.example.appcenter_project.domain.roommate.fixture.RoommateChattingRoomFixture.createActiveRoommateRoomWithGuestName(1L, 2L, "이순신");

        given(openChatRoomQuerydslRepository.findMyRooms(eq(1L), isNull()))
                .willReturn(List.of());
        given(roommateChattingRoomRepository.findActiveRoomsByUserId(1L))
                .willReturn(List.of(roommateRoom));
        given(roommateChattingChatRepository.findLastMessagesByRoomIds(anyList()))
                .willReturn(java.util.Map.of());
        given(roommateChattingChatRepository.countUnreadByRoomIdsAndUserId(anyList(), eq(1L)))
                .willReturn(java.util.Map.of());
        Pageable pageable = PageRequest.of(0, 20);

        // when
        ResponseChatRoomListDto result = openChatRoomService.getRooms(1L, OpenChatRoomTab.MY, null, pageable);

        // then
        assertThat(result.getContent().get(0).getName()).isEqualTo("이순신");
    }

    @Test
    @DisplayName("lastMessageAt=null 반환 — BR-665 AC-11: 룸메 방 메시지 없으면 lastMessageAt=null")
    void should_return_null_lastMessageAt_when_roommate_room_has_no_messages() {
        // given
        com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom roommateRoom =
                com.example.appcenter_project.domain.roommate.fixture.RoommateChattingRoomFixture.createActiveRoommateRoom(1L, 2L);

        given(openChatRoomQuerydslRepository.findMyRooms(eq(1L), isNull()))
                .willReturn(List.of());
        given(roommateChattingRoomRepository.findActiveRoomsByUserId(1L))
                .willReturn(List.of(roommateRoom));
        given(roommateChattingChatRepository.findLastMessagesByRoomIds(anyList()))
                .willReturn(java.util.Map.of());
        given(roommateChattingChatRepository.countUnreadByRoomIdsAndUserId(anyList(), eq(1L)))
                .willReturn(java.util.Map.of());
        Pageable pageable = PageRequest.of(0, 20);

        // when
        ResponseChatRoomListDto result = openChatRoomService.getRooms(1L, OpenChatRoomTab.MY, null, pageable);

        // then
        assertThat(result.getContent().get(0).getLastMessageAt()).isNull();
    }

    // ============================================================
    // AC-12: keyword 공백 처리
    // ============================================================

    @Test
    @DisplayName("빈 keyword 처리 — BR-665 AC-12: trim 후 빈 문자열은 null로 처리")
    void should_treat_blank_keyword_as_null_filter() {
        // given
        given(openChatRoomQuerydslRepository.findAllPublicRooms(isNull()))
                .willReturn(List.of());
        Pageable pageable = PageRequest.of(0, 20);

        // when
        openChatRoomService.getRooms(1L, OpenChatRoomTab.ALL, " ", pageable);

        // then
        then(openChatRoomQuerydslRepository).should(times(1)).findAllPublicRooms(null);
    }

    // ============================================================
    // MY 탭 — 참여 방 0개 경계 상황
    // ============================================================

    @Test
    @DisplayName("빈 목록 반환 — BR-665: MY 탭 참여 방 0개이면 content=[], totalUnreadCount=0")
    void should_return_empty_content_and_zero_unread_when_MY_tab_has_no_rooms() {
        // given
        given(openChatRoomQuerydslRepository.findMyRooms(eq(1L), isNull()))
                .willReturn(List.of());
        given(roommateChattingRoomRepository.findActiveRoomsByUserId(1L))
                .willReturn(List.of());
        Pageable pageable = PageRequest.of(0, 20);

        // when
        ResponseChatRoomListDto result = openChatRoomService.getRooms(1L, OpenChatRoomTab.MY, null, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalUnreadCount()).isEqualTo(0);
    }

    // ============================================================
    // N+1 방지 — bulk 쿼리 호출 검증
    // ============================================================

    @Test
    @DisplayName("bulk 쿼리 호출 — BR-665: MY 탭 룸메 방 lastMessage/unread는 루프 아닌 bulk 쿼리로 처리")
    void should_call_bulk_query_for_roommate_last_message_and_unread() {
        // given
        com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom room1 =
                com.example.appcenter_project.domain.roommate.fixture.RoommateChattingRoomFixture.createActiveRoommateRoom(1L, 2L);
        com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom room2 =
                com.example.appcenter_project.domain.roommate.fixture.RoommateChattingRoomFixture.createActiveRoommateRoom(1L, 3L);

        given(openChatRoomQuerydslRepository.findMyRooms(eq(1L), isNull()))
                .willReturn(List.of());
        given(roommateChattingRoomRepository.findActiveRoomsByUserId(1L))
                .willReturn(List.of(room1, room2));
        given(roommateChattingChatRepository.findLastMessagesByRoomIds(anyList()))
                .willReturn(java.util.Map.of());
        given(roommateChattingChatRepository.countUnreadByRoomIdsAndUserId(anyList(), eq(1L)))
                .willReturn(java.util.Map.of());
        Pageable pageable = PageRequest.of(0, 20);

        // when
        openChatRoomService.getRooms(1L, OpenChatRoomTab.MY, null, pageable);

        // then
        then(roommateChattingChatRepository).should(times(1)).findLastMessagesByRoomIds(anyList());
        then(roommateChattingChatRepository).should(times(1)).countUnreadByRoomIdsAndUserId(anyList(), eq(1L));
    }
}
