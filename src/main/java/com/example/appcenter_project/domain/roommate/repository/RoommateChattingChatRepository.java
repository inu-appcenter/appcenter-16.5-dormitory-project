package com.example.appcenter_project.domain.roommate.repository;

import com.example.appcenter_project.domain.roommate.entity.RoommateChattingChat;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom;
import com.example.appcenter_project.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoommateChattingChatRepository extends JpaRepository<RoommateChattingChat, Long>,
        RoommateChattingChatQuerydslRepository {
    List<RoommateChattingChat> findAllByRoommateChattingRoom_Id(Long roomId);
    List<RoommateChattingChat> findByRoommateChattingRoom(RoommateChattingRoom chatRoom);

    List<RoommateChattingChat> findByRoommateChattingRoomAndReadByReceiverFalse(RoommateChattingRoom chatRoom);

    List<RoommateChattingChat> findByRoommateChattingRoomAndMemberNotAndReadByReceiverFalse(RoommateChattingRoom chatRoom, User member);

}
