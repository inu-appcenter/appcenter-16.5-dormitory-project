package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatRoomDto;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomRecruitmentStatus;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatRecruitmentSortFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenChatRecruitmentSortServiceTest {

    // ────────── AC-01 isJoinable 계산 — OPEN + 정원 여유 ──────────

    @Test
    @DisplayName("isJoinable=true 반환 — AC-01 DERIVED 방: recruitmentClosed=false, 정원 여유 있음")
    void should_return_isJoinable_true_when_open_and_has_capacity() {
        // given
        ResponseOpenChatRoomDto roomDto = OpenChatRecruitmentSortFixture.createJoinableOpenRoomDto(1L);

        // when
        boolean isJoinable = roomDto.isJoinable();

        // then
        assertThat(isJoinable).isTrue();
    }

    // ────────── AC-02 isJoinable 계산 — CLOSED ──────────

    @Test
    @DisplayName("isJoinable=false 반환 — AC-02 DERIVED 방: recruitmentClosed=true")
    void should_return_isJoinable_false_when_recruitment_closed() {
        // given
        ResponseOpenChatRoomDto roomDto = OpenChatRecruitmentSortFixture.createClosedRoomDto(2L, null);

        // when
        boolean isJoinable = roomDto.isJoinable();

        // then
        assertThat(isJoinable).isFalse();
    }

    // ────────── AC-03 isJoinable 계산 — 정원 초과 ──────────

    @Test
    @DisplayName("isJoinable=false 반환 — AC-03 OPEN 방: currentParticipants == maxParticipants")
    void should_return_isJoinable_false_when_at_max_capacity() {
        // given
        ResponseOpenChatRoomDto roomDto = OpenChatRecruitmentSortFixture.createFullRoomDto(3L);

        // when
        boolean isJoinable = roomDto.isJoinable();

        // then
        assertThat(isJoinable).isFalse();
    }

    // ────────── AC-07 MY 탭 정렬 — CLOSED 방 하단 ──────────

    @Test
    @DisplayName("OPEN 방이 CLOSED 방보다 앞에 위치 — AC-07 MY 탭 인메모리 정렬")
    void should_sort_open_before_closed_when_my_tab() {
        // given
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime yesterday = today.minusDays(1);

        ResponseOpenChatRoomDto closedRoom = OpenChatRecruitmentSortFixture.createClosedRoomDto(1L, today);
        ResponseOpenChatRoomDto openRoom = OpenChatRecruitmentSortFixture.createOpenRoomDto(2L, yesterday);

        List<ResponseOpenChatRoomDto> merged = new ArrayList<>(List.of(closedRoom, openRoom));

        // when
        merged.sort(Comparator
                .comparingInt((ResponseOpenChatRoomDto r) ->
                        r.getRecruitmentStatus() == OpenChatRoomRecruitmentStatus.OPEN ? 0 : 1)
                .thenComparing(ResponseOpenChatRoomDto::isMyRoommate, Comparator.reverseOrder())
                .thenComparing(ResponseOpenChatRoomDto::getLastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())));

        // then
        assertThat(merged.get(0).getRoomId()).isEqualTo(2L);
    }

    // ────────── AC-08 MY 탭 정렬 — OPEN 그룹 내 isMyRoommate 상단 ──────────

    @Test
    @DisplayName("isMyRoommate=true 방이 OPEN 그룹 내 상단 — AC-08 MY 탭 인메모리 정렬")
    void should_sort_my_roommate_before_open_rooms_in_open_group() {
        // given
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);

        ResponseOpenChatRoomDto roommateRoom = OpenChatRecruitmentSortFixture.createOpenRoomDtoIsMyRoommate(1L, twoDaysAgo);
        ResponseOpenChatRoomDto openRoom = OpenChatRecruitmentSortFixture.createOpenRoomDto(2L, yesterday);

        List<ResponseOpenChatRoomDto> merged = new ArrayList<>(List.of(openRoom, roommateRoom));

        // when
        merged.sort(Comparator
                .comparingInt((ResponseOpenChatRoomDto r) ->
                        r.getRecruitmentStatus() == OpenChatRoomRecruitmentStatus.OPEN ? 0 : 1)
                .thenComparing(ResponseOpenChatRoomDto::isMyRoommate, Comparator.reverseOrder())
                .thenComparing(ResponseOpenChatRoomDto::getLastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())));

        // then
        assertThat(merged.get(0).getRoomId()).isEqualTo(1L);
    }

    // ────────── AC-10 isJoinable — 이미 참여 중인 CLOSED 방 ──────────

    @Test
    @DisplayName("isJoined=true인 CLOSED 방 — AC-10 isJoined 값 확인")
    void should_have_isJoined_true_when_joined_closed_room() {
        // given
        ResponseOpenChatRoomDto joinedClosedRoom = OpenChatRecruitmentSortFixture.createJoinedClosedRoomDto(5L);

        // when
        boolean isJoined = joinedClosedRoom.isJoined();

        // then
        assertThat(isJoined).isTrue();
    }

    @Test
    @DisplayName("참여 중인 CLOSED 방의 isJoinable=false — AC-10 방 속성 기준 계산")
    void should_have_isJoinable_false_when_joined_but_closed() {
        // given
        ResponseOpenChatRoomDto joinedClosedRoom = OpenChatRecruitmentSortFixture.createJoinedClosedRoomDto(5L);

        // when
        boolean isJoinable = joinedClosedRoom.isJoinable();

        // then
        assertThat(isJoinable).isFalse();
    }

    // ────────── Edge Case: lastMessageAt null — NULLS LAST ──────────

    @Test
    @DisplayName("lastMessageAt null 방이 같은 OPEN 그룹 내 하단 — Edge Case NULLS LAST")
    void should_sort_null_lastMessageAt_to_bottom_within_open_group() {
        // given
        LocalDateTime recentAt = LocalDateTime.now().minusHours(1);

        ResponseOpenChatRoomDto roomWithMessage = OpenChatRecruitmentSortFixture.createOpenRoomDto(1L, recentAt);
        ResponseOpenChatRoomDto roomWithoutMessage = OpenChatRecruitmentSortFixture.createOpenRoomDto(2L, null);

        List<ResponseOpenChatRoomDto> merged = new ArrayList<>(List.of(roomWithoutMessage, roomWithMessage));

        // when
        merged.sort(Comparator
                .comparingInt((ResponseOpenChatRoomDto r) ->
                        r.getRecruitmentStatus() == OpenChatRoomRecruitmentStatus.OPEN ? 0 : 1)
                .thenComparing(ResponseOpenChatRoomDto::isMyRoommate, Comparator.reverseOrder())
                .thenComparing(ResponseOpenChatRoomDto::getLastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())));

        // then
        assertThat(merged.get(1).getRoomId()).isEqualTo(2L);
    }

    // ────────── AC-05 같은 OPEN 그룹 내 lastMessageAt DESC ──────────

    @Test
    @DisplayName("최신 lastMessageAt 방이 먼저 — AC-05 같은 OPEN 그룹 내 정렬")
    void should_sort_by_lastMessageAt_desc_within_same_status_group() {
        // given
        LocalDateTime jan2 = LocalDateTime.of(2025, 1, 2, 0, 0);
        LocalDateTime jan1 = LocalDateTime.of(2025, 1, 1, 0, 0);

        ResponseOpenChatRoomDto roomX = OpenChatRecruitmentSortFixture.createOpenRoomDto(1L, jan2);
        ResponseOpenChatRoomDto roomY = OpenChatRecruitmentSortFixture.createOpenRoomDto(2L, jan1);

        List<ResponseOpenChatRoomDto> merged = new ArrayList<>(List.of(roomY, roomX));

        // when
        merged.sort(Comparator
                .comparingInt((ResponseOpenChatRoomDto r) ->
                        r.getRecruitmentStatus() == OpenChatRoomRecruitmentStatus.OPEN ? 0 : 1)
                .thenComparing(ResponseOpenChatRoomDto::isMyRoommate, Comparator.reverseOrder())
                .thenComparing(ResponseOpenChatRoomDto::getLastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())));

        // then
        assertThat(merged.get(0).getRoomId()).isEqualTo(1L);
    }
}
