package com.example.appcenter_project.domain.openChat.repository;

import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.entity.QOpenChatParticipant;
import com.example.appcenter_project.domain.openChat.entity.QOpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomScope;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomType;
import com.example.appcenter_project.domain.user.enums.DormType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OpenChatRoomQuerydslRepositoryImpl implements OpenChatRoomQuerydslRepository {

    private final JPAQueryFactory queryFactory;

    public OpenChatRoomQuerydslRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    private static final QOpenChatRoom openChatRoom = QOpenChatRoom.openChatRoom;
    private static final QOpenChatParticipant openChatParticipant = QOpenChatParticipant.openChatParticipant;

    @Override
    public List<OpenChatRoom> findMyRooms(Long userId, String keyword) {
        return queryFactory
                .selectFrom(openChatRoom)
                .join(openChatParticipant).on(
                        openChatRoom.id.eq(openChatParticipant.roomId),
                        openChatParticipant.userId.eq(userId)
                )
                .where(keywordContains(keyword))
                .orderBy(
                        openChatRoom.lastMessageAt.desc().nullsLast(),
                        openChatRoom.createdDate.desc()
                )
                .fetch();
    }

    @Override
    public List<OpenChatRoom> findByDormitory(String dormType, String keyword) {
        BooleanExpression regularRooms = openChatRoom.scope.eq(OpenChatRoomScope.DORMITORY)
                .and(creatorDormitoryEq(dormType))
                .and(
                        openChatRoom.roomType.ne(OpenChatRoomType.DERIVED)
                                .or(openChatRoom.isPublic.isTrue())
                );
        BooleanExpression officialRoom = targetDormEq(dormType);
        BooleanExpression condition = officialRoom != null
                ? regularRooms.or(officialRoom)
                : regularRooms;
        condition = condition.and(openChatRoom.isPublic.isTrue());   // 비노출 방 제외
        return queryFactory
                .selectFrom(openChatRoom)
                .where(condition, keywordContains(keyword))
                .orderBy(
                        openChatRoom.recruitmentClosed.asc(),
                        new CaseBuilder()
                                .when(openChatRoom.targetDorm.isNotNull()).then(1)
                                .otherwise(0).desc(),
                        openChatRoom.lastMessageAt.desc().nullsLast(),
                        openChatRoom.createdDate.desc()
                )
                .fetch();
    }

    @Override
    public List<OpenChatRoom> findAllPublicRooms(String keyword) {
        return queryFactory
                .selectFrom(openChatRoom)
                .where(
                        openChatRoom.isPublic.isTrue()
                                .and(openChatRoom.scope.eq(OpenChatRoomScope.ALL))
                                .and(openChatRoom.targetDorm.isNull())
                                .and(
                                        openChatRoom.roomType.eq(OpenChatRoomType.OPEN)
                                                .or(openChatRoom.roomType.eq(OpenChatRoomType.DERIVED))
                                ),
                        keywordContains(keyword)
                )
                .orderBy(
                        openChatRoom.recruitmentClosed.asc(),
                        openChatRoom.lastMessageAt.desc().nullsLast(),
                        openChatRoom.createdDate.desc()
                )
                .fetch();
    }

    private BooleanExpression scopeEq(OpenChatRoomScope scope) {
        return scope != null ? openChatRoom.scope.eq(scope) : null;
    }

    private BooleanExpression creatorDormitoryEq(String dormType) {
        return dormType != null ? openChatRoom.creatorDormitory.eq(dormType) : null;
    }

    private BooleanExpression targetDormEq(String dormType) {
        if (dormType == null) return null;
        try {
            DormType dorm = DormType.valueOf(dormType);
            return dorm == DormType.NONE ? null : openChatRoom.targetDorm.eq(dorm);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private BooleanExpression keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return openChatRoom.name.containsIgnoreCase(keyword)
                .or(openChatRoom.description.containsIgnoreCase(keyword));
    }
}