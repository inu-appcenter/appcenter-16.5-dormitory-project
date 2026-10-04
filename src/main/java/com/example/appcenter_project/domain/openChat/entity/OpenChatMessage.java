package com.example.appcenter_project.domain.openChat.entity;

import com.example.appcenter_project.common.BaseTimeEntity;
import com.example.appcenter_project.domain.openChat.enums.OpenChatMessageType;
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

    @Embedded
    private DeletedMessageState deletedState = new DeletedMessageState();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpenChatMessageType type;

    public static OpenChatMessage create(Long roomId, Long senderId, String content, OpenChatMessageType type) {
        OpenChatMessage message = new OpenChatMessage();
        message.roomId = roomId;
        message.senderId = senderId;
        message.content = content;
        message.type = type;
        return message;
    }
}
