package com.example.appcenter_project.domain.openChat.repository;

import com.example.appcenter_project.domain.openChat.entity.OpenChatMessage;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.example.appcenter_project.domain.openChat.entity.QOpenChatMessage.openChatMessage;

@Repository
public class OpenChatMessageQuerydslRepositoryImpl implements OpenChatMessageQuerydslRepository {

    private final JPAQueryFactory queryFactory;

    public OpenChatMessageQuerydslRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<OpenChatMessage> findByRoomIdWithCursor(Long roomId, Long lastMessageId, int size) {
        List<OpenChatMessage> results = queryFactory
                .selectFrom(openChatMessage)
                .where(
                        openChatMessage.roomId.eq(roomId),
                        idLessThan(lastMessageId)
                )
                .orderBy(openChatMessage.id.desc())
                .limit(size)
                .fetch();

        List<OpenChatMessage> reversed = new ArrayList<>(results);
        java.util.Collections.reverse(reversed);
        return reversed;
    }

    @Override
    public Optional<Long> findLatestMessageIdByRoomId(Long roomId) {
        Long result = queryFactory
                .select(openChatMessage.id.max())
                .from(openChatMessage)
                .where(openChatMessage.roomId.eq(roomId))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public long countByRoomIdAndIdGreaterThan(Long roomId, Long lastReadMessageId) {
        if (lastReadMessageId == null) {
            Long count = queryFactory
                    .select(openChatMessage.count())
                    .from(openChatMessage)
                    .where(openChatMessage.roomId.eq(roomId))
                    .fetchOne();
            return count != null ? count : 0L;
        }
        Long count = queryFactory
                .select(openChatMessage.count())
                .from(openChatMessage)
                .where(
                        openChatMessage.roomId.eq(roomId),
                        openChatMessage.id.gt(lastReadMessageId)
                )
                .fetchOne();
        return count != null ? count : 0L;
    }

    @Override
    public List<Long> findMessageIdsAfterInRoom(Long roomId, Long afterIdExclusive, Long toIdInclusive) {
        return queryFactory
                .select(openChatMessage.id)
                .from(openChatMessage)
                .where(
                        openChatMessage.roomId.eq(roomId),
                        afterIdExclusive != null ? openChatMessage.id.gt(afterIdExclusive) : null,
                        openChatMessage.id.loe(toIdInclusive)
                )
                .fetch();
    }

    private BooleanExpression idLessThan(Long lastMessageId) {
        return lastMessageId != null ? openChatMessage.id.lt(lastMessageId) : null;
    }
}
