package com.example.appcenter_project.domain.openChat.entity;

import com.example.appcenter_project.common.BaseTimeEntity;
import com.example.appcenter_project.domain.openChat.enums.OpenChatMessageType;
import com.example.appcenter_project.shared.enums.ChatRoomType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "open_chat_message")
public class OpenChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpenChatMessageType type;

    @Column(length = 100, unique = true)
    private String duplKey;

    @Column(nullable = false)
    private boolean isDeleted = false;

    private Long replyToMessageId;

    @Enumerated(EnumType.STRING)
    private OpenChatMessageType replyToMessageType;

    private Long replyToSenderId;

    private Long replyToRoomId;

    @Enumerated(EnumType.STRING)
    private ChatRoomType replyToRoomType;

    private Long replyToDerivedRoomId;

    public static OpenChatMessage create(Long roomId, Long senderId, String content, OpenChatMessageType type) {
        OpenChatMessage message = new OpenChatMessage();
        message.roomId = roomId;
        message.senderId = senderId;
        message.content = content;
        message.type = type;
        return message;
    }

    public static OpenChatMessage createReopenCard(Long roomId, Long senderId, String content, String duplKey) {
        OpenChatMessage message = new OpenChatMessage();
        message.roomId = roomId;
        message.senderId = senderId;
        message.content = content;
        message.type = OpenChatMessageType.REOPEN_CARD;
        message.duplKey = duplKey;
        return message;
    }

    public static OpenChatMessage createForTest(Long id, Long roomId, Long senderId, String content,
                                                OpenChatMessageType type, boolean isDeleted, Long replyToMessageId) {
        OpenChatMessage message = new OpenChatMessage();
        message.id = id;
        message.roomId = roomId;
        message.senderId = senderId;
        message.content = content;
        message.type = type;
        message.isDeleted = isDeleted;
        message.replyToMessageId = replyToMessageId;
        return message;
    }

    public void softDelete() {
        this.isDeleted = true;
    }

    public void attachReply(Long replyToMessageId, OpenChatMessageType replyToMessageType,
                            Long replyToSenderId, Long replyToRoomId, ChatRoomType replyToRoomType,
                            Long replyToDerivedRoomId) {
        this.replyToMessageId = replyToMessageId;
        this.replyToMessageType = replyToMessageType;
        this.replyToSenderId = replyToSenderId;
        this.replyToRoomId = replyToRoomId;
        this.replyToRoomType = replyToRoomType;
        this.replyToDerivedRoomId = replyToDerivedRoomId;
    }
}
