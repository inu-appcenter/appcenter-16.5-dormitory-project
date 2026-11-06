package com.example.appcenter_project.domain.fcm.fixture;

import com.example.appcenter_project.domain.fcm.entity.FcmOutbox;
import com.example.appcenter_project.domain.fcm.enums.FcmRoutingType;

import java.util.List;
import java.util.stream.IntStream;

public class FcmOutboxFixture {

    public static FcmOutbox createWithoutRouting() {
        return FcmOutbox.create("token-generic", "제목", "내용");
    }

    public static FcmOutbox createWithAnnouncementRouting(Long routingId) {
        return FcmOutbox.create("token-announcement", "공지 제목", "공지 내용", FcmRoutingType.ANNOUNCEMENT, routingId);
    }

    public static FcmOutbox createWithChatOpenRouting(Long routingId) {
        return FcmOutbox.create("token-chat-open", "채팅 제목", "채팅 내용", FcmRoutingType.CHAT_OPEN, routingId);
    }

    public static FcmOutbox createWithChatPersonalRouting(Long routingId) {
        return FcmOutbox.create("token-chat-personal", "채팅 제목", "채팅 내용", FcmRoutingType.CHAT_PERSONAL, routingId);
    }

    public static FcmOutbox createWithComplaintRouting(Long routingId) {
        return FcmOutbox.create("token-complaint", "민원 제목", "민원 내용", FcmRoutingType.COMPLAINT, routingId);
    }

    public static FcmOutbox createWithRoommatePostRouting(Long routingId) {
        return FcmOutbox.create("token-roommate", "룸메이트 제목", "룸메이트 내용", FcmRoutingType.ROOMMATE_POST, routingId);
    }

    public static List<FcmOutbox> createAnnouncementOutboxBatch(Long routingId, int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> FcmOutbox.create("token-" + i, "공지 제목", "공지 내용", FcmRoutingType.ANNOUNCEMENT, routingId))
                .toList();
    }

    public static List<FcmOutbox> createChatOpenOutboxBatch(Long routingId, int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> FcmOutbox.create("token-" + i, "채팅 제목", "채팅 내용", FcmRoutingType.CHAT_OPEN, routingId))
                .toList();
    }

    public static List<FcmOutbox> createChatPersonalOutboxBatch(Long routingId, int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> FcmOutbox.create("token-" + i, "채팅 제목", "채팅 내용", FcmRoutingType.CHAT_PERSONAL, routingId))
                .toList();
    }

    public static List<FcmOutbox> createComplaintOutboxBatch(Long routingId, int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> FcmOutbox.create("token-" + i, "민원 제목", "민원 내용", FcmRoutingType.COMPLAINT, routingId))
                .toList();
    }

    public static List<FcmOutbox> createRoommatePostOutboxBatch(Long routingId, int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> FcmOutbox.create("token-" + i, "룸메이트 제목", "룸메이트 내용", FcmRoutingType.ROOMMATE_POST, routingId))
                .toList();
    }

    public static List<FcmOutbox> createGenericOutboxBatch(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> FcmOutbox.create("token-" + i, "제목", "내용"))
                .toList();
    }

    // BR-792: userId 포함 헬퍼
    public static FcmOutbox createWithUserIdAndChatOpenImmediate(Long userId, Long routingId, Long messageId) {
        return FcmOutbox.create(userId, "token-chat-open", "채팅 제목", "채팅 내용",
                FcmRoutingType.CHAT_OPEN, routingId, messageId);
    }

    public static FcmOutbox createWithUserIdAndChatOpenBundle(Long userId, Long routingId) {
        return FcmOutbox.create(userId, "token-chat-open", "채팅 제목", "새 메시지 3개",
                FcmRoutingType.CHAT_OPEN, routingId, null);
    }

    public static FcmOutbox createWithNullUserIdAndChatOpenImmediate(Long routingId, Long messageId) {
        return FcmOutbox.create(null, "token-chat-open", "채팅 제목", "채팅 내용",
                FcmRoutingType.CHAT_OPEN, routingId, messageId);
    }

    public static List<FcmOutbox> createChatOpenOutboxBatchWithUserId(Long routingId, int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> FcmOutbox.create((long) (i + 100), "token-" + i, "채팅 제목", "채팅 내용",
                        FcmRoutingType.CHAT_OPEN, routingId))
                .toList();
    }
}
