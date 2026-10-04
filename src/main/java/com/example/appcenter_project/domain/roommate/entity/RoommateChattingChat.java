package com.example.appcenter_project.domain.roommate.entity;

import com.example.appcenter_project.common.BaseTimeEntity;
import com.example.appcenter_project.common.DeletedMessageState;
import com.example.appcenter_project.domain.roommate.enums.RoommateChattingMessageType;
import com.example.appcenter_project.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class RoommateChattingChat extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roommate_chatting_room_id", nullable = false)
    private RoommateChattingRoom roommateChattingRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private User member;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private boolean readByReceiver = false;

    @Column(nullable = false)
    private boolean isSystem = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private RoommateChattingMessageType messageType;

    @Column(nullable = true)
    private Long disclosureRequestId;

    @Embedded
    private DeletedMessageState deletedState = new DeletedMessageState();

    @Builder
    public RoommateChattingChat(RoommateChattingRoom roommateChattingRoom, User member, String content, boolean readByReceiver) {
        this.roommateChattingRoom = roommateChattingRoom;
        this.member = member;
        this.content = content;
        this.readByReceiver = readByReceiver;
    }

    public static RoommateChattingChat createSystemMessage(RoommateChattingRoom room, String content) {
        RoommateChattingChat chat = new RoommateChattingChat();
        chat.roommateChattingRoom = room;
        chat.content = content;
        chat.isSystem = true;
        chat.readByReceiver = true;
        return chat;
    }

    public static RoommateChattingChat create(RoommateChattingRoom room, User sender, String content) {
        RoommateChattingChat chat = new RoommateChattingChat();
        chat.roommateChattingRoom = room;
        chat.member = sender;
        chat.content = content;
        chat.readByReceiver = false;
        return chat;
    }

    public static RoommateChattingChat create(
            RoommateChattingRoom room, User sender, String content, java.time.LocalDateTime createdDate) {
        RoommateChattingChat chat = new RoommateChattingChat();
        chat.roommateChattingRoom = room;
        chat.member = sender;
        chat.content = content;
        chat.readByReceiver = false;
        chat.createdDate = createdDate;
        return chat;
    }

    public static RoommateChattingChat createStudentIdRequestMessage(
            RoommateChattingRoom room, User requester, String content, Long requestId) {
        RoommateChattingChat chat = new RoommateChattingChat();
        chat.roommateChattingRoom = room;
        chat.member = requester;
        chat.content = content;
        chat.isSystem = true;
        chat.messageType = RoommateChattingMessageType.STUDENT_ID_REQUEST;
        chat.disclosureRequestId = requestId;
        chat.readByReceiver = true;
        return chat;
    }

    public void markAsRead() {
        this.readByReceiver = true;
    }

    public void delete() {
        this.deletedState.delete();
    }
}

