package com.example.appcenter_project.domain.roommate.service;

import com.example.appcenter_project.domain.roommate.dto.response.ResponseRoommatePostDto;
import com.example.appcenter_project.domain.roommate.repository.RoommateBoardLikeRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateBoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoommateQueryService {

    private final RoommateBoardLikeRepository roommateBoardLikeRepository;
    private final RoommateBoardRepository roommateBoardRepository;
    private final RoommateMatchingPeriodResolver periodResolver;

    public List<ResponseRoommatePostDto> findByUser(Long userId) {
        MatchingPeriod current = periodResolver.resolveCurrent(LocalDate.now());
        return roommateBoardRepository.findAllByUserIdOrderByCreatedDateDesc(userId)
                .stream()
                .map(board -> {
                    ResponseRoommatePostDto dto = ResponseRoommatePostDto.entityToDto(board, board.isMatched(), null);
                    dto.updateIsMyPost(true);
                    dto.updateIsCurrentPeriod(
                            board.getSemester() == current.semester()
                            && board.getYear() != null
                            && board.getYear() == current.year()
                    );
                    return dto;
                })
                .toList();
    }

    public List<ResponseRoommatePostDto> findLikedByUser(Long userId) {
        return roommateBoardLikeRepository.findByUserIdWithRoommateBoardAndRoommateCheckListAndUser(userId)
                .stream().map(roommateBoardLike -> ResponseRoommatePostDto.entityToDto(roommateBoardLike.getRoommateBoard(), roommateBoardLike.getRoommateBoard().isMatched(), null)).toList();
    }

}