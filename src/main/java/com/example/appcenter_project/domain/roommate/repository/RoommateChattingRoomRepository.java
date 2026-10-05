package com.example.appcenter_project.domain.roommate.repository;

import com.example.appcenter_project.domain.roommate.entity.RoommateBoard;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom;
import com.example.appcenter_project.domain.roommate.enums.SemesterType;
import com.example.appcenter_project.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoommateChattingRoomRepository extends JpaRepository<RoommateChattingRoom, Long> {
    boolean existsByRoommateBoardAndGuest(RoommateBoard board, User guest);
    List<RoommateChattingRoom> findAllByHostOrGuest(User host, User guest);
    boolean existsRoommateChattingRoomByHostAndGuest(User host, User guest);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM RoommateChattingRoom r
            WHERE r.id = :roomId
              AND (
                  (r.host.id = :requesterId AND r.hostLeft = false)
                  OR
                  (r.guest.id = :requesterId AND r.guestLeft = false)
              )
            """)
    boolean existsParticipant(Long roomId, Long requesterId);

    Optional<RoommateChattingRoom> findByHostAndGuest(User guest, User host);

    @Query("""
            SELECT r FROM RoommateChattingRoom r
            WHERE r.guest = :guest AND r.host = :host
            AND r.roommateBoard.year = :year AND r.roommateBoard.semester = :semester
            """)
    Optional<RoommateChattingRoom> findByGuestAndHostAndBoardYearAndSemester(
            @Param("guest") User guest,
            @Param("host") User host,
            @Param("year") Integer year,
            @Param("semester") SemesterType semester
    );

    @Query("""
            SELECT r FROM RoommateChattingRoom r
            JOIN FETCH r.host JOIN FETCH r.guest
            WHERE (r.host.id = :userId AND r.hostLeft = false)
               OR (r.guest.id = :userId AND r.guestLeft = false)
            """)
    List<RoommateChattingRoom> findActiveRoomsByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RoommateChattingRoom r SET r.roommateBoard = null WHERE r.roommateBoard.id = :boardId")
    void detachBoard(@Param("boardId") Long boardId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RoommateChattingRoom r SET r.guestChecklist = null WHERE r.guestChecklist.id = :checkListId")
    void detachGuestChecklist(@Param("checkListId") Long checkListId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RoommateChattingRoom r SET r.hostChecklist = null WHERE r.hostChecklist.id = :checkListId")
    void detachHostChecklist(@Param("checkListId") Long checkListId);
}

