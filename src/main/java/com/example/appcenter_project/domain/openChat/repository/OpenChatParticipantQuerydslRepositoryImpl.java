package com.example.appcenter_project.domain.openChat.repository;

import com.example.appcenter_project.domain.openChat.dto.UnreadNotificationInfo;
import com.example.appcenter_project.domain.openChat.entity.OpenChatParticipant;
import com.example.appcenter_project.domain.openChat.entity.QOpenChatMessage;
import com.example.appcenter_project.domain.openChat.entity.QOpenChatParticipant;
import com.example.appcenter_project.domain.openChat.enums.ChatNotificationMode;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OpenChatParticipantQuerydslRepositoryImpl implements OpenChatParticipantQuerydslRepository {

    private final JPAQueryFactory queryFactory;

    public OpenChatParticipantQuerydslRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    private static final QOpenChatParticipant openChatParticipant = QOpenChatParticipant.openChatParticipant;

    @Override
    public Map<Long, Long> countByRoomIds(List<Long> roomIds) {
        List<Tuple> results = queryFactory
                .select(openChatParticipant.roomId, openChatParticipant.count())
                .from(openChatParticipant)
                .where(openChatParticipant.roomId.in(roomIds))
                .groupBy(openChatParticipant.roomId)
                .fetch();

        Map<Long, Long> countMap = new HashMap<>();
        for (Tuple tuple : results) {
            Long roomId = tuple.get(openChatParticipant.roomId);
            Long count = tuple.get(openChatParticipant.count());
            if (roomId != null && count != null) {
                countMap.put(roomId, count);
            }
        }
        return countMap;
    }

    @Override
    public Set<Long> findJoinedRoomIds(Long userId, List<Long> roomIds) {
        List<Long> results = queryFactory
                .select(openChatParticipant.roomId)
                .from(openChatParticipant)
                .where(
                        openChatParticipant.roomId.in(roomIds),
                        openChatParticipant.userId.eq(userId)
                )
                .fetch();

        return new HashSet<>(results);
    }

    @Override
    public Map<Long, Long> findLastReadMessageIdsByUserId(Long userId, List<Long> roomIds) {
        List<OpenChatParticipant> participants = queryFactory
                .selectFrom(openChatParticipant)
                .where(
                        openChatParticipant.userId.eq(userId),
                        openChatParticipant.roomId.in(roomIds)
                )
                .fetch();

        Map<Long, Long> result = new HashMap<>();
        for (OpenChatParticipant participant : participants) {
            result.put(participant.getRoomId(), participant.getLastReadMessageId());
        }
        return result;
    }

    @Override
    public long countReadByRoomIdAndMessageId(Long roomId, Long messageId) {
        Long count = queryFactory
                .select(openChatParticipant.count())
                .from(openChatParticipant)
                .where(
                        openChatParticipant.roomId.eq(roomId),
                        openChatParticipant.lastReadMessageId.isNotNull(),
                        openChatParticipant.lastReadMessageId.goe(messageId)
                )
                .fetchOne();
        return count != null ? count : 0L;
    }

    @Override
    public List<UnreadNotificationInfo> findUnreadCountsForNotification() {
        QOpenChatMessage message = QOpenChatMessage.openChatMessage;

        List<Tuple> results = queryFactory
                .select(
                        openChatParticipant.roomId,
                        openChatParticipant.userId,
                        message.id.count()
                )
                .from(openChatParticipant)
                .join(message).on(
                        message.roomId.eq(openChatParticipant.roomId),
                        openChatParticipant.lastBundledNotifiedAt.isNull()
                                .or(message.createdDate.gt(openChatParticipant.lastBundledNotifiedAt))
                )
                .where(openChatParticipant.notificationMode.eq(ChatNotificationMode.BUNDLED))
                .groupBy(openChatParticipant.roomId, openChatParticipant.userId)
                .fetch();

        return results.stream()
                .map(t -> new UnreadNotificationInfo(
                        t.get(openChatParticipant.userId),
                        t.get(openChatParticipant.roomId),
                        t.get(message.id.count()) != null ? t.get(message.id.count()) : 0L
                ))
                .toList();
    }
}
