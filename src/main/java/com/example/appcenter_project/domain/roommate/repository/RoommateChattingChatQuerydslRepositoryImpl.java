package com.example.appcenter_project.domain.roommate.repository;

import com.example.appcenter_project.domain.openChat.enums.ChatNotificationMode;
import com.example.appcenter_project.domain.roommate.dto.RoommateUnreadNotificationInfo;
import com.example.appcenter_project.domain.roommate.entity.QRoommateChattingChat;
import com.example.appcenter_project.domain.roommate.entity.QRoommateChattingRoom;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingChat;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoommateChattingChatQuerydslRepositoryImpl implements RoommateChattingChatQuerydslRepository {

    private final JPAQueryFactory queryFactory;

    public RoommateChattingChatQuerydslRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    private static final QRoommateChattingChat chat = QRoommateChattingChat.roommateChattingChat;
    private static final QRoommateChattingRoom room = QRoommateChattingRoom.roommateChattingRoom;

    @Override
    public Map<Long, RoommateChattingChat> findLastMessagesByRoomIds(List<Long> roomIds) {
        if (roomIds.isEmpty()) return Map.of();

        JPQLQuery<Long> maxIdSubquery = JPAExpressions
                .select(chat.id.max())
                .from(chat)
                .where(chat.roommateChattingRoom.id.in(roomIds))
                .groupBy(chat.roommateChattingRoom.id);

        List<RoommateChattingChat> results = queryFactory
                .selectFrom(chat)
                .where(chat.id.in(maxIdSubquery))
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        c -> c.getRoommateChattingRoom().getId(),
                        c -> c));
    }

    @Override
    public Map<Long, Long> countUnreadByRoomIdsAndUserId(List<Long> roomIds, Long userId) {
        if (roomIds.isEmpty()) return Map.of();

        List<Tuple> results = queryFactory
                .select(chat.roommateChattingRoom.id, chat.count())
                .from(chat)
                .where(
                        chat.roommateChattingRoom.id.in(roomIds),
                        chat.member.id.ne(userId),
                        chat.readByReceiver.isFalse()
                )
                .groupBy(chat.roommateChattingRoom.id)
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        t -> t.get(chat.roommateChattingRoom.id),
                        t -> t.get(chat.count())));
    }

    @Override
    public List<RoommateUnreadNotificationInfo> findUnreadCountsForBundled() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        List<Tuple> hostResults = queryFactory
                .select(room.id, room.host.id, chat.count())
                .from(chat)
                .join(chat.roommateChattingRoom, room)
                .where(
                        chat.readByReceiver.isFalse(),
                        chat.member.id.eq(room.guest.id),
                        room.hostNotificationMode.eq(ChatNotificationMode.BUNDLED),
                        chat.createdDate.goe(oneHourAgo)
                )
                .groupBy(room.id, room.host.id)
                .fetch();

        List<Tuple> guestResults = queryFactory
                .select(room.id, room.guest.id, chat.count())
                .from(chat)
                .join(chat.roommateChattingRoom, room)
                .where(
                        chat.readByReceiver.isFalse(),
                        chat.member.id.eq(room.host.id),
                        room.guestNotificationMode.eq(ChatNotificationMode.BUNDLED),
                        chat.createdDate.goe(oneHourAgo)
                )
                .groupBy(room.id, room.guest.id)
                .fetch();

        List<RoommateUnreadNotificationInfo> infos = new java.util.ArrayList<>();
        for (Tuple t : hostResults) {
            infos.add(new RoommateUnreadNotificationInfo(
                    t.get(room.host.id), t.get(room.id), t.get(chat.count())));
        }
        for (Tuple t : guestResults) {
            infos.add(new RoommateUnreadNotificationInfo(
                    t.get(room.guest.id), t.get(room.id), t.get(chat.count())));
        }
        return infos;
    }
}
