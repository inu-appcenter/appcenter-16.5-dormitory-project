package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.openChat.dto.response.ResponseChatRoomListDto;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomScope;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomTab;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatScopeInheritFixture;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OpenChatRoomScopeInheritQueryTest {

    @Mock
    OpenChatRoomQuerydslRepository openChatRoomQuerydslRepository;

    @Mock
    RoommateChattingRoomRepository roommateChattingRoomRepository;

    @Mock
    RoommateChattingChatRepository roommateChattingChatRepository;

    @Mock
    MyRoommateRepository myRoommateRepository;

    @InjectMocks
    OpenChatRoomService openChatRoomService;

    private final Pageable pageable = PageRequest.of(0, 20);

    @BeforeEach
    void setUp() {
        lenient().when(myRoommateRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
        lenient().when(roommateChattingRoomRepository.findActiveRoomsByUserId(anyLong()))
                .thenReturn(List.of());
    }

    // ============================================================
    // AC-3: DORMITORY + isPublic=true → 기숙사 탭 포함
    // ============================================================

    @Test
    @DisplayName("기숙사 탭 포함 — BR-729 AC-3: scope=DORMITORY, isPublic=true인 파생 방은 기숙사 탭에 포함")
    void should_include_derived_room_in_dormitory_tab_when_scope_DORMITORY_and_isPublic_true() {
        // given
        OpenChatRoom derivedRoom = OpenChatScopeInheritFixture.createDerivedRoomWithScopeAndIsPublic(
                OpenChatRoomScope.DORMITORY, true);
        given(openChatRoomQuerydslRepository.findByDormitory(anyString(), isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(derivedRoom)));

        // when
        ResponseChatRoomListDto result = openChatRoomService.getRooms(1L, OpenChatRoomTab.DORMITORY, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    // ============================================================
    // AC-4: DORMITORY + isPublic=false → 기숙사 탭 미포함
    // ============================================================

    @Test
    @DisplayName("기숙사 탭 미포함 — BR-729 AC-4: scope=DORMITORY, isPublic=false인 파생 방은 기숙사 탭에 미포함")
    void should_exclude_derived_room_from_dormitory_tab_when_scope_DORMITORY_and_isPublic_false() {
        // given
        given(openChatRoomQuerydslRepository.findByDormitory(anyString(), isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        ResponseChatRoomListDto result = openChatRoomService.getRooms(1L, OpenChatRoomTab.DORMITORY, null, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    // ============================================================
    // AC-5: DORMITORY + isPublic=false → 전체 탭 미포함
    // ============================================================

    @Test
    @DisplayName("전체 탭 미포함 — BR-729 AC-5: scope=DORMITORY, isPublic=false인 파생 방은 전체 탭에 미포함")
    void should_exclude_derived_room_from_all_tab_when_scope_DORMITORY_and_isPublic_false() {
        // given
        given(openChatRoomQuerydslRepository.findAllPublicRooms(isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        ResponseChatRoomListDto result = openChatRoomService.getRooms(1L, OpenChatRoomTab.ALL, null, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    // ============================================================
    // AC-6: DORMITORY + isPublic=true → 전체 탭 미포함
    // ============================================================

    @Test
    @DisplayName("전체 탭 미포함 — BR-729 AC-6: scope=DORMITORY, isPublic=true인 파생 방은 전체 탭에 미포함")
    void should_exclude_derived_room_from_all_tab_when_scope_DORMITORY_and_isPublic_true() {
        // given
        given(openChatRoomQuerydslRepository.findAllPublicRooms(isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        ResponseChatRoomListDto result = openChatRoomService.getRooms(1L, OpenChatRoomTab.ALL, null, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    // ============================================================
    // AC-7: ALL + isPublic=true → 전체 탭 포함
    // ============================================================

    @Test
    @DisplayName("전체 탭 포함 — BR-729 AC-7: scope=ALL, isPublic=true인 파생 방은 전체 탭에 포함")
    void should_include_derived_room_in_all_tab_when_scope_ALL_and_isPublic_true() {
        // given
        OpenChatRoom derivedRoom = OpenChatScopeInheritFixture.createDerivedRoomWithScopeAndIsPublic(
                OpenChatRoomScope.ALL, true);
        given(openChatRoomQuerydslRepository.findAllPublicRooms(isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(derivedRoom)));

        // when
        ResponseChatRoomListDto result = openChatRoomService.getRooms(1L, OpenChatRoomTab.ALL, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    // ============================================================
    // AC-8: ALL + isPublic=false → 전체 탭 미포함
    // ============================================================

    @Test
    @DisplayName("전체 탭 미포함 — BR-729 AC-8: scope=ALL, isPublic=false인 파생 방은 전체 탭에 미포함")
    void should_exclude_derived_room_from_all_tab_when_scope_ALL_and_isPublic_false() {
        // given
        given(openChatRoomQuerydslRepository.findAllPublicRooms(isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        ResponseChatRoomListDto result = openChatRoomService.getRooms(1L, OpenChatRoomTab.ALL, null, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    // ============================================================
    // AC-9: ALL + isPublic=false → 기숙사 탭 미포함
    // ============================================================

    @Test
    @DisplayName("기숙사 탭 미포함 — BR-729 AC-9: scope=ALL, isPublic=false인 파생 방은 기숙사 탭에 미포함")
    void should_exclude_derived_room_from_dormitory_tab_when_scope_ALL_and_isPublic_false() {
        // given
        given(openChatRoomQuerydslRepository.findByDormitory(anyString(), isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        ResponseChatRoomListDto result = openChatRoomService.getRooms(1L, OpenChatRoomTab.DORMITORY, null, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    // ============================================================
    // AC-10: MY 탭 — isPublic=false이어도 참여 방이면 포함
    // ============================================================

    @Test
    @DisplayName("MY 탭 포함 — BR-729 AC-10: scope=DORMITORY, isPublic=false인 파생 방도 MY 탭에 포함")
    void should_include_derived_room_in_my_tab_regardless_of_isPublic() {
        // given
        OpenChatRoom derivedRoom = OpenChatScopeInheritFixture.createDerivedRoomWithScopeAndIsPublic(
                OpenChatRoomScope.DORMITORY, false);
        given(openChatRoomQuerydslRepository.findMyRooms(eq(1L), isNull()))
                .willReturn(List.of(derivedRoom));

        // when
        ResponseChatRoomListDto result = openChatRoomService.getRooms(1L, OpenChatRoomTab.MY, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
    }
}
