package com.example.appcenter_project.domain.openChat.entity;

import com.example.appcenter_project.common.BaseTimeEntity;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomScope;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomType;
import com.example.appcenter_project.domain.user.enums.DormType;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "open_chat_room")
public class OpenChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(length = 100)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpenChatRoomScope scope;

    @Column(nullable = false)
    private int maxParticipants;

    private String creatorDormitory;

    private LocalDateTime lastMessageAt;

    @Column(length = 500)
    private String lastMessage;

    @Column(nullable = false)
    private boolean isOfficial;

    private Long createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpenChatRoomType roomType;

    @Column(length = 50)
    private String password;

    @Column(nullable = false)
    private boolean isPublic = true;

    @Enumerated(EnumType.STRING)
    private DormType targetDorm;

    @Column(nullable = false)
    private boolean recruitmentClosed = false;

    private LocalDateTime closedAt;

    private Long closedBy;

    public boolean matchesPassword(String input) {
        return this.password == null || this.password.equals(input);
    }

    public static OpenChatRoom create(String name, String description, OpenChatRoomScope scope,
                                       int maxParticipants, Long createdBy,
                                       String creatorDormitory, boolean isOfficial) {
        OpenChatRoom room = new OpenChatRoom();
        room.name = name;
        room.description = description;
        room.scope = scope;
        room.maxParticipants = maxParticipants;
        room.creatorDormitory = creatorDormitory;
        room.isOfficial = isOfficial;
        room.createdBy = createdBy;
        room.roomType = OpenChatRoomType.OPEN;
        room.isPublic = true;
        return room;
    }

    public static OpenChatRoom create(String name, String description, OpenChatRoomScope scope,
                                       int maxParticipants, Long createdBy,
                                       String creatorDormitory, boolean isOfficial,
                                       String password, boolean isPublic) {
        OpenChatRoom room = new OpenChatRoom();
        room.name = name;
        room.description = description;
        room.scope = scope;
        room.maxParticipants = maxParticipants;
        room.creatorDormitory = creatorDormitory;
        room.isOfficial = isOfficial;
        room.createdBy = createdBy;
        room.roomType = OpenChatRoomType.OPEN;
        room.password = password;
        room.isPublic = isPublic;
        return room;
    }

    public static OpenChatRoom createPersonal(String name, Long createdBy, String password) {
        OpenChatRoom room = new OpenChatRoom();
        room.name = name;
        room.scope = OpenChatRoomScope.ALL;
        room.maxParticipants = 2;
        room.isOfficial = false;
        room.createdBy = createdBy;
        room.roomType = OpenChatRoomType.PERSONAL;
        room.isPublic = false;
        room.password = (password != null && !password.isBlank()) ? password : null;
        return room;
    }

    public static OpenChatRoom createOfficial(String name, String description, OpenChatRoomScope scope,
                                               int maxParticipants, Long createdBy,
                                               String creatorDormitory) {
        OpenChatRoom room = new OpenChatRoom();
        room.name = name;
        room.description = description;
        room.scope = scope != null ? scope : OpenChatRoomScope.ALL;
        room.maxParticipants = maxParticipants;
        room.creatorDormitory = creatorDormitory;
        room.isOfficial = true;
        room.createdBy = createdBy;
        room.roomType = OpenChatRoomType.OPEN;
        return room;
    }

    public static OpenChatRoom createDerived(String name, String description, int maxParticipants,
                                             Long createdBy, String password, boolean isPublic,
                                             String creatorDormitory) {
        OpenChatRoom room = new OpenChatRoom();
        room.name = name;
        room.description = description;
        room.scope = creatorDormitory != null            // 부모 기숙사 상속
                ? OpenChatRoomScope.DORMITORY
                : OpenChatRoomScope.ALL;
        room.maxParticipants = maxParticipants;
        room.creatorDormitory = creatorDormitory;
        room.isOfficial = false;
        room.createdBy = createdBy;
        room.roomType = OpenChatRoomType.DERIVED;
        room.password = password;
        room.isPublic = isPublic;
        return room;
    }

    public static OpenChatRoom createDerived(String name, String description, int maxParticipants,
                                              Long createdBy, String password, boolean isPublic,
                                              String creatorDormitory, OpenChatRoomScope scope) {
        OpenChatRoom room = new OpenChatRoom();
        room.name = name;
        room.description = description;
        room.scope = scope;
        room.maxParticipants = maxParticipants;
        room.creatorDormitory = creatorDormitory;
        room.isOfficial = false;
        room.createdBy = createdBy;
        room.roomType = OpenChatRoomType.DERIVED;
        room.password = password;
        room.isPublic = isPublic;
        return room;
    }

    public static OpenChatRoom createDormOfficial(String name, String description, Long createdBy, DormType targetDorm) {
        OpenChatRoom room = new OpenChatRoom();
        room.name = name;
        room.description = description;
        room.scope = OpenChatRoomScope.ALL;
        room.maxParticipants = Integer.MAX_VALUE;
        room.isOfficial = true;
        room.createdBy = createdBy;
        room.roomType = OpenChatRoomType.OPEN;
        room.isPublic = true;
        room.targetDorm = targetDorm;
        return room;
    }

    public static OpenChatRoom createDormOfficialForTest(Long id, String name, DormType targetDorm) {
        OpenChatRoom room = new OpenChatRoom();
        room.id = id;
        room.name = name;
        room.scope = OpenChatRoomScope.ALL;
        room.maxParticipants = Integer.MAX_VALUE;
        room.isOfficial = true;
        room.roomType = OpenChatRoomType.OPEN;
        room.isPublic = true;
        room.targetDorm = targetDorm;
        return room;
    }

    public static OpenChatRoom createForTest(Long id, String name) {
        OpenChatRoom room = new OpenChatRoom();
        room.id = id;
        room.name = name;
        room.scope = OpenChatRoomScope.ALL;
        room.maxParticipants = 10;
        room.isOfficial = false;
        room.roomType = OpenChatRoomType.OPEN;
        room.isPublic = true;
        return room;
    }

    public static OpenChatRoom createForTest(Long id, String name, OpenChatRoomType roomType) {
        OpenChatRoom room = new OpenChatRoom();
        room.id = id;
        room.name = name;
        room.scope = OpenChatRoomScope.ALL;
        room.maxParticipants = 10;
        room.isOfficial = false;
        room.roomType = roomType;
        room.isPublic = true;
        return room;
    }

    public void update(String name, String description, OpenChatRoomScope scope,
                       Integer maxParticipants, String password, Boolean isPublic) {
        if (name != null)            this.name = name;
        if (description != null)     this.description = description;
        if (scope != null)           this.scope = scope;
        if (maxParticipants != null) this.maxParticipants = maxParticipants;
        if (password != null)        this.password = password.isBlank() ? null : password;
        if (isPublic != null)        this.isPublic = isPublic;
    }

    public void updateLastMessage(String content, LocalDateTime at) {
        this.lastMessage = content != null && content.length() > 500 ? content.substring(0, 500) : content;
        this.lastMessageAt = at;
    }

    public void closeRecruitment(Long actorId) {
        if (this.roomType != OpenChatRoomType.DERIVED) {
            throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_DERIVED);
        }
        if (this.recruitmentClosed) {
            throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_ALREADY_CLOSED);
        }
        this.recruitmentClosed = true;
        this.closedAt = LocalDateTime.now();
        this.closedBy = actorId;
    }
}
