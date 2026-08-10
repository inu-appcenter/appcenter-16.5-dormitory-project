package com.example.appcenter_project.domain.roommate.entity;

import com.example.appcenter_project.common.BaseTimeEntity;
import com.example.appcenter_project.domain.openChat.enums.ChatNotificationMode;
import com.example.appcenter_project.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class RoommateChattingRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 채팅이 생성된 게시글
    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "roommate_board_id", nullable = true)
    private RoommateBoard roommateBoard;

    // 게시글 작성자 (고정, 모든 채팅방에 동일)
    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    private User guest;

    // 채팅 요청자
    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "guest_roommate_checklist_id", nullable = true)
    private RoommateCheckList guestChecklist;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "host_roommate_checklist_id", nullable = true)
    private RoommateCheckList hostChecklist;

    @OneToMany(mappedBy = "roommateChattingRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoommateChattingChat> chattingChatList = new ArrayList<>();

    @Column(nullable = false)
    private boolean hostLeft = false;

    @Column(nullable = false)
    private boolean guestLeft = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChatNotificationMode hostNotificationMode = ChatNotificationMode.EVERY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChatNotificationMode guestNotificationMode = ChatNotificationMode.EVERY;

    @Builder
    public RoommateChattingRoom(RoommateBoard roommateBoard, User guest, User host,
                                RoommateCheckList guestChecklist, RoommateCheckList hostChecklist) {
        this.roommateBoard = roommateBoard;
        this.guest = guest;
        this.host = host;
        this.guestChecklist = guestChecklist;
        this.hostChecklist = hostChecklist;
    }

    public static RoommateChattingRoom create(User host, User guest) {
        RoommateChattingRoom room = new RoommateChattingRoom();
        room.host = host;
        room.guest = guest;
        if (host != null && host.getId() != null && guest != null && guest.getId() != null) {
            room.id = host.getId() * 1000L + guest.getId();
        }
        return room;
    }

    public void leaveAsHost() {
        this.hostLeft = true;
    }

    public void hostLeave() {
        this.hostLeft = true;
    }

    public void leaveAsGuest() {
        this.guestLeft = true;
    }

    public boolean isBothLeft() {
        return this.hostLeft && this.guestLeft;
    }

    public void rejoinAsHost() {
        this.hostLeft = false;
    }

    public void rejoinAsGuest() {
        this.guestLeft = false;
    }

    public void updateHostNotificationMode(ChatNotificationMode mode) {
        this.hostNotificationMode = mode;
    }

    public void updateGuestNotificationMode(ChatNotificationMode mode) {
        this.guestNotificationMode = mode;
    }
}
