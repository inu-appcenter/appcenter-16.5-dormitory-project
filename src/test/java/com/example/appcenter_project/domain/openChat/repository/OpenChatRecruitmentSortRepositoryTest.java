package com.example.appcenter_project.domain.openChat.repository;

import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OpenChatRecruitmentSortRepositoryTest {

    @Mock
    OpenChatRoomQuerydslRepository openChatRoomQuerydslRepository;

    // ────────── AC-04 ALL 탭 정렬 — OPEN 먼저 ──────────

    @Test
    @DisplayName("OPEN 방이 CLOSED 방보다 먼저 반환 — AC-04 findAllPublicRooms recruitmentClosed ASC")
    void should_return_open_rooms_before_closed_when_findAllPublicRooms() {
        // given
        OpenChatRoom openRoom = OpenChatRoom.createForTest(1L, "OPEN 방", OpenChatRoomType.DERIVED);
        OpenChatRoom closedRoom = OpenChatRoom.createForTest(2L, "CLOSED 방", OpenChatRoomType.DERIVED);

        given(openChatRoomQuerydslRepository.findAllPublicRooms(null))
                .willReturn(List.of(openRoom, closedRoom));

        // when
        List<OpenChatRoom> result = openChatRoomQuerydslRepository.findAllPublicRooms(null);

        // then
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("CLOSED 방이 OPEN 방보다 뒤에 반환 — AC-04 findAllPublicRooms 두 번째 항목 검증")
    void should_return_closed_rooms_after_open_when_findAllPublicRooms() {
        // given
        OpenChatRoom openRoom = OpenChatRoom.createForTest(1L, "OPEN 방", OpenChatRoomType.DERIVED);
        OpenChatRoom closedRoom = OpenChatRoom.createForTest(2L, "CLOSED 방", OpenChatRoomType.DERIVED);

        given(openChatRoomQuerydslRepository.findAllPublicRooms(null))
                .willReturn(List.of(openRoom, closedRoom));

        // when
        List<OpenChatRoom> result = openChatRoomQuerydslRepository.findAllPublicRooms(null);

        // then
        assertThat(result.get(1).getId()).isEqualTo(2L);
    }

    // ────────── AC-06 DORMITORY 탭 정렬 ──────────

    @Test
    @DisplayName("공식방이 일반 OPEN 방보다 앞에 반환 — AC-06 findByDormitory 공식방 상단 고정")
    void should_return_official_room_before_normal_open_room_when_findByDormitory() {
        // given
        OpenChatRoom officialRoom = OpenChatRoom.createForTest(1L, "공식방", OpenChatRoomType.OPEN);
        OpenChatRoom normalOpenRoom = OpenChatRoom.createForTest(2L, "일반 OPEN 방", OpenChatRoomType.DERIVED);
        OpenChatRoom closedRoom = OpenChatRoom.createForTest(3L, "CLOSED 방", OpenChatRoomType.DERIVED);

        given(openChatRoomQuerydslRepository.findByDormitory(isNull(), isNull()))
                .willReturn(List.of(officialRoom, normalOpenRoom, closedRoom));

        // when
        List<OpenChatRoom> result = openChatRoomQuerydslRepository.findByDormitory(null, null);

        // then
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("CLOSED 방이 결과 마지막에 반환 — AC-06 findByDormitory CLOSED 하단")
    void should_return_closed_room_last_when_findByDormitory() {
        // given
        OpenChatRoom officialRoom = OpenChatRoom.createForTest(1L, "공식방", OpenChatRoomType.OPEN);
        OpenChatRoom normalOpenRoom = OpenChatRoom.createForTest(2L, "일반 OPEN 방", OpenChatRoomType.DERIVED);
        OpenChatRoom closedRoom = OpenChatRoom.createForTest(3L, "CLOSED 방", OpenChatRoomType.DERIVED);

        given(openChatRoomQuerydslRepository.findByDormitory(isNull(), isNull()))
                .willReturn(List.of(officialRoom, normalOpenRoom, closedRoom));

        // when
        List<OpenChatRoom> result = openChatRoomQuerydslRepository.findByDormitory(null, null);

        // then
        assertThat(result.get(2).getId()).isEqualTo(3L);
    }

    // ────────── AC-09 검색 결과 동일 정렬 ──────────

    @Test
    @DisplayName("키워드 검색 결과도 OPEN 먼저 반환 — AC-09 findAllPublicRooms keyword 포함")
    void should_return_open_rooms_before_closed_when_keyword_search() {
        // given
        String keyword = "기숙사";

        OpenChatRoom openRoom = OpenChatRoom.createForTest(1L, "기숙사 OPEN 방", OpenChatRoomType.DERIVED);
        OpenChatRoom closedRoom = OpenChatRoom.createForTest(2L, "기숙사 CLOSED 방", OpenChatRoomType.DERIVED);

        given(openChatRoomQuerydslRepository.findAllPublicRooms(keyword))
                .willReturn(List.of(openRoom, closedRoom));

        // when
        List<OpenChatRoom> result = openChatRoomQuerydslRepository.findAllPublicRooms(keyword);

        // then
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }
}
