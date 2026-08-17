package com.example.appcenter_project.domain.roommate.repository;

import com.example.appcenter_project.domain.roommate.entity.RoommateBoard;
import com.example.appcenter_project.domain.roommate.enums.SemesterType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoommateBoardRepository extends JpaRepository<RoommateBoard, Long>, RoommateBoardQuerydslRepository {
    List<RoommateBoard> findAllByOrderByCreatedDateDesc();
    Optional<RoommateBoard> findByUserId(Long userId);
    List<RoommateBoard> findAllByUserIdOrderByCreatedDateDesc(Long userId);

    // 가장 최근 10개의 게시글 조회
    List<RoommateBoard> findTop10ByOrderByCreatedDateDesc();

    List<RoommateBoard> findAllByOrderByIdDesc(Pageable pageable);

    List<RoommateBoard> findByIdLessThanOrderByIdDesc(Long lastId, Pageable pageable);
    // 자기 자신 제외 전체 가져오기 (유사도 계산용)
    List<RoommateBoard> findAllByIdNot(Long boardId);

    // 필터링용: RoommateCheckList와 User를 함께 조회
    @Query("SELECT DISTINCT b FROM RoommateBoard b " +
            "JOIN FETCH b.roommateCheckList c " +
            "LEFT JOIN FETCH c.dormPeriod " +
            "JOIN FETCH b.user u " +
            "ORDER BY b.createdDate DESC")
    List<RoommateBoard> findAllWithCheckListAndUserOrderByCreatedDateDesc();

    Optional<RoommateBoard> findByUserIdAndYearAndSemester(Long userId, Integer year, SemesterType semester);
}