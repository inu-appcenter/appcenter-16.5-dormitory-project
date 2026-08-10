package com.example.appcenter_project.domain.roommate.repository;

import com.example.appcenter_project.domain.roommate.dto.RoommateUnreadNotificationInfo;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingChat;

import java.util.List;
import java.util.Map;

public interface RoommateChattingChatQuerydslRepository {

    Map<Long, RoommateChattingChat> findLastMessagesByRoomIds(List<Long> roomIds);

    Map<Long, Long> countUnreadByRoomIdsAndUserId(List<Long> roomIds, Long userId);

    List<RoommateUnreadNotificationInfo> findUnreadCountsForBundled();
}
