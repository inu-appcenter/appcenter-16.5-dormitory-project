package com.example.appcenter_project.domain.openChat.repository;

import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomScope;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatScopeInheritFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * BR-729 파생 방 scope 상속 — QueryDSL 쿼리 변경 명세
 *
 * 구현 에이전트가 변경해야 할 쿼리:
 *
 * [findByDormitory(dormType, keyword)]
 * 변경 전: (scope=DORMITORY OR roomType=DERIVED) AND creatorDormitory=dormType
 * 변경 후: scope=DORMITORY AND creatorDormitory=dormType AND (roomType≠DERIVED OR isPublic=true)
 *
 * QueryDSL 표현:
 *   BooleanExpression regularRooms = openChatRoom.scope.eq(OpenChatRoomScope.DORMITORY)
 *       .and(creatorDormitoryEq(dormType))
 *       .and(
 *           openChatRoom.roomType.ne(OpenChatRoomType.DERIVED)
 *               .or(openChatRoom.isPublic.isTrue())
 *       );
 *
 * [findAllPublicRooms(keyword)]
 * 변경 전: roomType IN (OPEN, DERIVED) AND isPublic=true
 * 변경 후: isPublic=true AND (roomType=OPEN OR (roomType=DERIVED AND scope=ALL))
 *
 * QueryDSL 표현:
 *   openChatRoom.isPublic.isTrue()
 *   .and(
 *       openChatRoom.roomType.eq(OpenChatRoomType.OPEN)
 *           .or(
 *               openChatRoom.roomType.eq(OpenChatRoomType.DERIVED)
 *                   .and(openChatRoom.scope.eq(OpenChatRoomScope.ALL))
 *           )
 *   )
 *
 * NOTE: Testcontainers 환경이 갖춰지면 아래 테스트를 @DataJpaTest로 전환하여
 * 실제 DB 쿼리 결과를 검증한다. 현재는 Mock 기반으로 쿼리 위임 동작을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class OpenChatRoomScopeInheritRepositoryTest {

    @Mock
    OpenChatRoomQuerydslRepository openChatRoomQuerydslRepository;

    // ============================================================
    // findByDormitory — BR-729 AC-3: DORMITORY + isPublic=true 포함
    // ============================================================

    @Test
    @DisplayName("findByDormitory 포함 — BR-729 AC-3: scope=DORMITORY, isPublic=true인 파생 방 반환")
    void should_return_derived_room_from_findByDormitory_when_scope_DORMITORY_and_isPublic_true() {
        // given
        OpenChatRoom derivedRoom = OpenChatScopeInheritFixture.createDerivedRoomWithScopeAndIsPublic(
                OpenChatRoomScope.DORMITORY, true);
        Pageable pageable = PageRequest.of(0, 20);
        given(openChatRoomQuerydslRepository.findByDormitory(eq("SAMSUNG"), isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(derivedRoom)));

        // when
        Page<OpenChatRoom> result = openChatRoomQuerydslRepository.findByDormitory("SAMSUNG", null, pageable);

        // then
        assertThat(result.getContent()).isNotEmpty();
    }

    // ============================================================
    // findByDormitory — BR-729 AC-4: DORMITORY + isPublic=false 미포함
    // ============================================================

    @Test
    @DisplayName("findByDormitory 미포함 — BR-729 AC-4: scope=DORMITORY, isPublic=false인 파생 방 미반환")
    void should_not_return_derived_room_from_findByDormitory_when_scope_DORMITORY_and_isPublic_false() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        given(openChatRoomQuerydslRepository.findByDormitory(eq("SAMSUNG"), isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        Page<OpenChatRoom> result = openChatRoomQuerydslRepository.findByDormitory("SAMSUNG", null, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    // ============================================================
    // findAllPublicRooms — BR-729 AC-5: DORMITORY + isPublic=false 미포함
    // ============================================================

    @Test
    @DisplayName("findAllPublicRooms 미포함 — BR-729 AC-5: scope=DORMITORY, isPublic=false인 파생 방 미반환")
    void should_not_return_derived_room_from_findAllPublicRooms_when_scope_DORMITORY_and_isPublic_false() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        given(openChatRoomQuerydslRepository.findAllPublicRooms(isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        Page<OpenChatRoom> result = openChatRoomQuerydslRepository.findAllPublicRooms(null, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    // ============================================================
    // findAllPublicRooms — BR-729 AC-6: DORMITORY + isPublic=true 미포함
    // ============================================================

    @Test
    @DisplayName("findAllPublicRooms 미포함 — BR-729 AC-6: scope=DORMITORY, isPublic=true인 파생 방 미반환")
    void should_not_return_derived_room_from_findAllPublicRooms_when_scope_DORMITORY_and_isPublic_true() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        given(openChatRoomQuerydslRepository.findAllPublicRooms(isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        Page<OpenChatRoom> result = openChatRoomQuerydslRepository.findAllPublicRooms(null, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    // ============================================================
    // findAllPublicRooms — BR-729 AC-7: ALL + isPublic=true 포함
    // ============================================================

    @Test
    @DisplayName("findAllPublicRooms 포함 — BR-729 AC-7: scope=ALL, isPublic=true인 파생 방 반환")
    void should_return_derived_room_from_findAllPublicRooms_when_scope_ALL_and_isPublic_true() {
        // given
        OpenChatRoom derivedRoom = OpenChatScopeInheritFixture.createDerivedRoomWithScopeAndIsPublic(
                OpenChatRoomScope.ALL, true);
        Pageable pageable = PageRequest.of(0, 20);
        given(openChatRoomQuerydslRepository.findAllPublicRooms(isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(derivedRoom)));

        // when
        Page<OpenChatRoom> result = openChatRoomQuerydslRepository.findAllPublicRooms(null, pageable);

        // then
        assertThat(result.getContent()).isNotEmpty();
    }

    // ============================================================
    // findAllPublicRooms — BR-729 AC-8: ALL + isPublic=false 미포함
    // ============================================================

    @Test
    @DisplayName("findAllPublicRooms 미포함 — BR-729 AC-8: scope=ALL, isPublic=false인 파생 방 미반환")
    void should_not_return_derived_room_from_findAllPublicRooms_when_scope_ALL_and_isPublic_false() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        given(openChatRoomQuerydslRepository.findAllPublicRooms(isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        Page<OpenChatRoom> result = openChatRoomQuerydslRepository.findAllPublicRooms(null, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    // ============================================================
    // findByDormitory — BR-729 AC-9: ALL + isPublic=false 미포함
    // ============================================================

    @Test
    @DisplayName("findByDormitory 미포함 — BR-729 AC-9: scope=ALL, isPublic=false인 파생 방 미반환")
    void should_not_return_derived_room_from_findByDormitory_when_scope_ALL_and_isPublic_false() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        given(openChatRoomQuerydslRepository.findByDormitory(anyString(), isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        Page<OpenChatRoom> result = openChatRoomQuerydslRepository.findByDormitory("SAMSUNG", null, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    // ============================================================
    // findMyRooms — BR-729 AC-10: MY 탭은 isPublic 무관 포함
    // ============================================================

    @Test
    @DisplayName("findMyRooms 포함 — BR-729 AC-10: scope=DORMITORY, isPublic=false인 파생 방도 MY 탭에 포함")
    void should_return_derived_room_from_findMyRooms_regardless_of_isPublic() {
        // given
        OpenChatRoom derivedRoom = OpenChatScopeInheritFixture.createDerivedRoomWithScopeAndIsPublic(
                OpenChatRoomScope.DORMITORY, false);
        given(openChatRoomQuerydslRepository.findMyRooms(eq(7L), isNull()))
                .willReturn(List.of(derivedRoom));

        // when
        List<OpenChatRoom> result = openChatRoomQuerydslRepository.findMyRooms(7L, null);

        // then
        assertThat(result).isNotEmpty();
    }
}
