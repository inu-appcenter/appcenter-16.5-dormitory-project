package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.openChat.dto.request.RequestCreateDerivedRoomDto;
import com.example.appcenter_project.domain.openChat.dto.request.RequestCreateOpenChatRoomDto;
import com.example.appcenter_project.domain.openChat.dto.request.RequestCreatePersonalRoomDto;
import com.example.appcenter_project.domain.openChat.dto.request.RequestUpdateOpenChatRoomDto;
import com.example.appcenter_project.domain.openChat.enums.ChatNotificationMode;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseChatRoomListDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseDerivedRoomCreatedDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseLeaveOpenChatRoomDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatParticipantDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatParticipantListDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatRoomDetailDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatRoomDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponsePersonalRoomCreatedDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseSimpleParticipantDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseSimpleParticipantListDto;
import com.example.appcenter_project.domain.openChat.entity.OpenChatParticipant;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.KickReason;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomScope;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomTab;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomType;
import com.example.appcenter_project.domain.openChat.repository.OpenChatMessageRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatParticipantRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomQuerydslRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomRepository;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseNotificationModeDto;
import org.springframework.beans.factory.annotation.Qualifier;
import com.example.appcenter_project.domain.block.service.BlockService;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingChat;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom;
import com.example.appcenter_project.domain.roommate.repository.MyRoommateRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingChatRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingRoomRepository;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.enums.Role;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OpenChatRoomService {

    private final OpenChatRoomRepository openChatRoomRepository;
    private final OpenChatParticipantRepository openChatParticipantRepository;
    private final OpenChatMessageRepository openChatMessageRepository;
    private final UserRepository userRepository;
    private final OpenChatMessageService openChatMessageService;
    private final OpenChatRoomQuerydslRepository openChatRoomQuerydslRepository;
    private final RoommateChattingRoomRepository roommateChattingRoomRepository;
    private final RoommateChattingChatRepository roommateChattingChatRepository;
    private final MyRoommateRepository myRoommateRepository;
    private final BlockService blockService;
    private final OpenChatNotificationService openChatNotificationService;

    @Autowired
    public OpenChatRoomService(
            OpenChatRoomRepository openChatRoomRepository,
            OpenChatParticipantRepository openChatParticipantRepository,
            OpenChatMessageRepository openChatMessageRepository,
            UserRepository userRepository,
            @Lazy OpenChatMessageService openChatMessageService,
            @Qualifier("openChatRoomQuerydslRepositoryImpl") OpenChatRoomQuerydslRepository openChatRoomQuerydslRepository,
            RoommateChattingRoomRepository roommateChattingRoomRepository,
            RoommateChattingChatRepository roommateChattingChatRepository,
            MyRoommateRepository myRoommateRepository,
            BlockService blockService,
            OpenChatNotificationService openChatNotificationService) {
        this.openChatRoomRepository = openChatRoomRepository;
        this.openChatParticipantRepository = openChatParticipantRepository;
        this.openChatMessageRepository = openChatMessageRepository;
        this.userRepository = userRepository;
        this.openChatMessageService = openChatMessageService;
        this.openChatRoomQuerydslRepository = openChatRoomQuerydslRepository;
        this.roommateChattingRoomRepository = roommateChattingRoomRepository;
        this.roommateChattingChatRepository = roommateChattingChatRepository;
        this.myRoommateRepository = myRoommateRepository;
        this.blockService = blockService;
        this.openChatNotificationService = openChatNotificationService;
    }

    @Transactional
    public ResponseDerivedRoomCreatedDto createDerivedRoom(Long userId, RequestCreateDerivedRoomDto request) {
        OpenChatRoom originRoom = openChatRoomRepository.findById(request.getOriginRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND));

        if (!openChatParticipantRepository.existsByRoomIdAndUserId(request.getOriginRoomId(), userId)) {
            throw new CustomException(ErrorCode.OPEN_CHAT_PARTICIPANT_NOT_FOUND);
        }

        OpenChatRoom derivedRoom = OpenChatRoom.createDerived(
                request.getName(),
                request.getDescription(),
                request.getMaxParticipants(),
                userId,
                request.getPassword(),
                request.getIsPublic(),
                originRoom.getCreatorDormitory(),
                originRoom.getScope()
        );
        OpenChatRoom savedRoom = openChatRoomRepository.save(derivedRoom);
        openChatParticipantRepository.save(OpenChatParticipant.create(savedRoom.getId(), userId, true));

        openChatMessageService.sendRoomLinkMessage(
                request.getOriginRoomId(), userId,
                savedRoom.getId(), request.getName(), request.getDescription(), request.getMaxParticipants());

        return ResponseDerivedRoomCreatedDto.of(savedRoom.getId());
    }

    @Transactional
    public Long createRoom(RequestCreateOpenChatRoomDto request, Long userId) {
        String creatorDormitory = null;
        if (request.getScope() == OpenChatRoomScope.DORMITORY) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            creatorDormitory = user.getDormType() != null ? user.getDormType().name() : null;
        }

        boolean pub = !Boolean.FALSE.equals(request.getIsPublic());
        String pwd = (request.getPassword() != null && !request.getPassword().isBlank())
                ? request.getPassword() : null;

        OpenChatRoom room = OpenChatRoom.create(
                request.getName(),
                request.getDescription(),
                request.getScope(),
                request.getMaxParticipants(),
                userId,
                creatorDormitory,
                false,
                pwd,
                pub
        );
        OpenChatRoom savedRoom = openChatRoomRepository.save(room);

        OpenChatParticipant participant = OpenChatParticipant.create(savedRoom.getId(), userId, true);
        openChatParticipantRepository.save(participant);

        return savedRoom.getId();
    }

    @Transactional
    public ResponsePersonalRoomCreatedDto createPersonalRoom(Long userId, RequestCreatePersonalRoomDto request) {
        if (userId.equals(request.getTargetUserId())) {
            throw new CustomException(ErrorCode.OPEN_CHAT_SELF_PERSONAL_FORBIDDEN);
        }
        userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (blockService.isBlockedBy(request.getTargetUserId(), userId)) {
            throw new CustomException(ErrorCode.USER_BLOCKED_BY_TARGET);
        }

        OpenChatRoom room = OpenChatRoom.createPersonal(request.getName(), userId, request.getPassword());
        OpenChatRoom savedRoom = openChatRoomRepository.save(room);
        openChatParticipantRepository.save(OpenChatParticipant.create(savedRoom.getId(), userId, true));
        openChatParticipantRepository.save(OpenChatParticipant.create(savedRoom.getId(), request.getTargetUserId(), false));
        return ResponsePersonalRoomCreatedDto.of(savedRoom.getId());
    }

    @Transactional(readOnly = true)
    public ResponseChatRoomListDto getRooms(Long userId, OpenChatRoomTab tab, String keyword, Pageable pageable) {
        String k = (keyword == null || keyword.isBlank()) ? null : keyword;

        if (tab == OpenChatRoomTab.MY) {
            return getMyRooms(userId, k, pageable);
        } else if (tab == OpenChatRoomTab.ALL) {
            List<OpenChatRoom> rooms = openChatRoomQuerydslRepository.findAllPublicRooms(k);
            List<ResponseOpenChatRoomDto> dtos = buildOpenChatDtos(rooms, userId, false);
            return ResponseChatRoomListDto.of(dtos, pageable, 0);
        } else {
            String dormType = Optional.ofNullable(userRepository)
                    .flatMap(r -> r.findById(userId))
                    .map(u -> u.getDormType() != null ? u.getDormType().name() : "NONE")
                    .orElse("NONE");
            List<OpenChatRoom> rooms = openChatRoomQuerydslRepository.findByDormitory(dormType, k);
            List<ResponseOpenChatRoomDto> dtos = buildOpenChatDtos(rooms, userId, false);
            return ResponseChatRoomListDto.of(dtos, pageable, 0);
        }
    }

    @Transactional(readOnly = true)
    public Page<ResponseOpenChatRoomDto> getRoomsForDormitory(Long userId, String dormType, Pageable pageable) {
        if ("NONE".equals(dormType)) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        List<OpenChatRoom> rooms = openChatRoomQuerydslRepository.findByDormitory(dormType, null);
        return toPageDto(rooms, userId, pageable);
    }

    private ResponseChatRoomListDto getMyRooms(Long userId, String keyword, Pageable pageable) {
        List<OpenChatRoom> openChatRooms = openChatRoomQuerydslRepository.findMyRooms(userId, keyword);

        List<RoommateChattingRoom> roommateRooms = roommateChattingRoomRepository.findActiveRoomsByUserId(userId);
        if (keyword != null) {
            String lowerKeyword = keyword.toLowerCase();
            roommateRooms = roommateRooms.stream()
                    .filter(r -> getOpponentName(r, userId).toLowerCase().contains(lowerKeyword))
                    .toList();
        }

        List<Long> roommateRoomIds = roommateRooms.stream().map(RoommateChattingRoom::getId).toList();
        Map<Long, RoommateChattingChat> lastMsgMap = roommateRoomIds.isEmpty()
                ? Map.of()
                : roommateChattingChatRepository.findLastMessagesByRoomIds(roommateRoomIds);
        Map<Long, Long> unreadMap = roommateRoomIds.isEmpty()
                ? Map.of()
                : roommateChattingChatRepository.countUnreadByRoomIdsAndUserId(roommateRoomIds, userId);

        Long myRoommateId = myRoommateRepository.findByUserId(userId)
                .map(mr -> mr.getRoommate().getId())
                .orElse(null);

        List<ResponseOpenChatRoomDto> openChatDtos = buildOpenChatDtosWithUnread(openChatRooms, userId);

        List<Long> personalRoomIds = openChatRooms.stream()
                .filter(r -> r.getRoomType() == OpenChatRoomType.PERSONAL)
                .map(OpenChatRoom::getId)
                .toList();
        if (!personalRoomIds.isEmpty()) {
            List<OpenChatParticipant> personalParticipants =
                    openChatParticipantRepository.findAllByRoomIdIn(personalRoomIds);
            Map<Long, Long> partnerIdByRoomId = personalParticipants.stream()
                    .filter(p -> !p.getUserId().equals(userId))
                    .collect(Collectors.toMap(
                            OpenChatParticipant::getRoomId,
                            OpenChatParticipant::getUserId,
                            (a, b) -> a));
            Map<Long, ResponseOpenChatRoomDto> dtoByRoomId = openChatDtos.stream()
                    .collect(Collectors.toMap(ResponseOpenChatRoomDto::getRoomId, r -> r));
            partnerIdByRoomId.forEach((roomId, partnerId) -> {
                if (blockService.isBlockedBy(partnerId, userId)) {
                    ResponseOpenChatRoomDto dto = dtoByRoomId.get(roomId);
                    if (dto != null) dto.updateIsBlockedByPartner(true);
                }
            });
        }

        List<ResponseOpenChatRoomDto> roommateDtos = roommateRooms.stream()
                .map(r -> {
                    RoommateChattingChat lastChat = lastMsgMap.get(r.getId());
                    int unread = unreadMap.getOrDefault(r.getId(), 0L).intValue();
                    Long opponentId = r.getHost().getId().equals(userId) ? r.getGuest().getId() : r.getHost().getId();
                    boolean isMyRoommate = myRoommateId != null && myRoommateId.equals(opponentId);
                    ResponseOpenChatRoomDto dto = ResponseOpenChatRoomDto.fromRoommate(
                            r.getId(),
                            getOpponentName(r, userId),
                            lastChat != null ? lastChat.getCreatedDate() : null,
                            lastChat != null ? lastChat.getContent() : null,
                            unread,
                            isMyRoommate);
                    if (blockService.isBlockedBy(opponentId, userId)) {
                        dto.updateIsBlockedByPartner(true);
                    }
                    return dto;
                })
                .toList();

        List<ResponseOpenChatRoomDto> merged = new ArrayList<>();
        merged.addAll(openChatDtos);
        merged.addAll(roommateDtos);
        merged.sort(Comparator
                .comparing(ResponseOpenChatRoomDto::isMyRoommate, Comparator.reverseOrder())
                .thenComparing(ResponseOpenChatRoomDto::getLastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())));

        int openChatUnread = openChatDtos.stream().mapToInt(ResponseOpenChatRoomDto::getUnreadCount).sum();
        int roommateUnread = (int) unreadMap.values().stream().mapToLong(Long::longValue).sum();

        return ResponseChatRoomListDto.of(merged, pageable, openChatUnread + roommateUnread);
    }

    private String getOpponentName(RoommateChattingRoom room, Long userId) {
        if (room.getHost().getId().equals(userId)) {
            return room.getGuest().getName();
        }
        return room.getHost().getName();
    }

    @Transactional
    public ResponseOpenChatRoomDetailDto joinRoom(Long userId, Long roomId, String password) {
        OpenChatRoom room = openChatRoomRepository.findByIdWithLock(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND));

        if (openChatParticipantRepository.existsByRoomIdAndUserId(roomId, userId)) {
            return toDetailDtoWithBlockedCheck(room, roomId, userId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (room.getRoomType() != OpenChatRoomType.PERSONAL) {
            if (!room.matchesPassword(password)) {
                throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN);
            }
        }

        if (room.getScope() == OpenChatRoomScope.DORMITORY) {
            String userDorm = user.getDormType() != null ? user.getDormType().name() : null;
            String roomDorm = room.getCreatorDormitory();
            if (roomDorm == null || !roomDorm.equals(userDorm)) {
                throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN);
            }
        }

        long currentCount = openChatParticipantRepository.countByRoomId(roomId);
        if (currentCount >= room.getMaxParticipants()) {
            throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_FULL);
        }

        ChatNotificationMode defaultMode = ChatNotificationMode.EVERY;
        openChatParticipantRepository.save(
                OpenChatParticipant.create(roomId, userId, LocalDateTime.now(), defaultMode));
        openChatMessageService.sendSystemMessage(roomId, user.getName() + "님이 입장했습니다.");

        return toDetailDtoWithBlockedCheck(room, roomId, userId);
    }

    @Transactional
    public ResponseLeaveOpenChatRoomDto leaveRoom(Long roomId, Long userId, Long newHostUserId) {
        List<OpenChatParticipant> lockedParticipants =
                openChatParticipantRepository.findAllByRoomIdWithLock(roomId);

        OpenChatParticipant self = lockedParticipants.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_PARTICIPANT_NOT_FOUND));

        if (newHostUserId != null) {
            if (!self.isHost()) {
                throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN);
            }

            if (newHostUserId.equals(userId)) {
                throw new CustomException(ErrorCode.OPEN_CHAT_ALREADY_HOST);
            }

            OpenChatParticipant newHostParticipant = lockedParticipants.stream()
                    .filter(p -> p.getUserId().equals(newHostUserId))
                    .findFirst()
                    .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_PARTICIPANT_NOT_FOUND));

            newHostParticipant.grantHost();
            openChatParticipantRepository.delete(self);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            openChatMessageService.sendSystemMessage(roomId, user.getName() + "님이 퇴장했습니다.");

            log.info("[OpenChat-Exit] exitType=VOLUNTARY roomId={} targetUserId={} actorId={} processedAt={}",
                    roomId, userId, userId, Instant.now());
            return ResponseLeaveOpenChatRoomDto.builder().roomDeleted(false).build();
        }

        if (self.isHost()) {
            long hostCount = lockedParticipants.stream().filter(OpenChatParticipant::isHost).count();
            if (hostCount == 1) {
                if (lockedParticipants.size() == 1) {
                    OpenChatRoom room = openChatRoomRepository.findByIdWithLock(roomId)
                            .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND));
                    if (!room.isOfficial()) {
                        openChatParticipantRepository.deleteAll(lockedParticipants);
                        openChatRoomRepository.delete(room);
                        log.info("[OpenChat-Exit] exitType=VOLUNTARY roomId={} targetUserId={} actorId={} processedAt={}",
                                roomId, userId, userId, Instant.now());
                        return ResponseLeaveOpenChatRoomDto.builder().roomDeleted(true).build();
                    }
                }
                throw new CustomException(ErrorCode.OPEN_CHAT_SOLE_HOST_CANNOT_LEAVE);
            }
        }

        openChatParticipantRepository.delete(self);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        openChatMessageService.sendSystemMessage(roomId, user.getName() + "님이 퇴장했습니다.");

        log.info("[OpenChat-Exit] exitType=VOLUNTARY roomId={} targetUserId={} actorId={} processedAt={}",
                roomId, userId, userId, Instant.now());
        return ResponseLeaveOpenChatRoomDto.builder().roomDeleted(false).build();
    }

    @Transactional
    public void updateRoom(Long userId, Long roomId, RequestUpdateOpenChatRoomDto request) {
        OpenChatRoom room = openChatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND));

        OpenChatParticipant participant = openChatParticipantRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_PARTICIPANT_NOT_FOUND));

        if (!participant.isHost()) {
            throw new CustomException(ErrorCode.OPEN_CHAT_NOT_HOST);
        }

        if (room.isOfficial() || room.getRoomType() == OpenChatRoomType.PERSONAL) {
            throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN);
        }

        if (request.getMaxParticipants() != null) {
            long currentCount = openChatParticipantRepository.countByRoomId(roomId);
            if (currentCount > request.getMaxParticipants()) {
                throw new CustomException(ErrorCode.OPEN_CHAT_MAX_PARTICIPANTS_TOO_SMALL);
            }
        }

        room.update(request.getName(), request.getDescription(), request.getScope(),
                request.getMaxParticipants(), request.getPassword(), request.getIsPublic());
    }

    @Transactional
    public void updateNotificationMode(Long userId, Long roomId, ChatNotificationMode mode) {
        OpenChatParticipant participant = openChatParticipantRepository
                .findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_PARTICIPANT_NOT_FOUND));
        participant.updateNotificationMode(mode);
        if (mode == ChatNotificationMode.OFF) {
            openChatNotificationService.cancelPendingChatNotifications(userId, roomId);
        }
    }

    @Transactional(readOnly = true)
    public ResponseNotificationModeDto getNotificationMode(Long userId, Long roomId) {
        OpenChatParticipant participant = openChatParticipantRepository
                .findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_PARTICIPANT_NOT_FOUND));
        return ResponseNotificationModeDto.of(participant.getNotificationMode());
    }

    @Transactional
    public void kickParticipant(Long actorId, Long roomId, Long targetUserId, KickReason reason, Long newHostUserId) {
        OpenChatRoom room = openChatRoomRepository.findByIdWithLock(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND));

        if (actorId.equals(targetUserId)) {
            throw new CustomException(ErrorCode.OPEN_CHAT_KICK_FORBIDDEN);
        }

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        boolean isAdmin = actor.getRole() == Role.ROLE_ADMIN;

        OpenChatParticipant targetParticipant = openChatParticipantRepository
                .findByRoomIdAndUserId(roomId, targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_PARTICIPANT_NOT_FOUND));

        if (!isAdmin) {
            boolean actorIsHost = openChatParticipantRepository
                    .existsByRoomIdAndUserIdAndIsHost(roomId, actorId, true);
            if (!actorIsHost) {
                throw new CustomException(ErrorCode.OPEN_CHAT_KICK_FORBIDDEN);
            }

            if (targetParticipant.isHost()) {
                throw new CustomException(ErrorCode.OPEN_CHAT_KICK_FORBIDDEN);
            }
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (targetUser.getRole() == Role.ROLE_ADMIN) {
            throw new CustomException(ErrorCode.OPEN_CHAT_KICK_FORBIDDEN);
        }

        if (isAdmin && targetParticipant.isHost()) {
            List<OpenChatParticipant> allParticipants = openChatParticipantRepository.findAllByRoomId(roomId);
            boolean othersExist = allParticipants.size() > 1;
            if (othersExist) {
                if (newHostUserId == null) {
                    throw new CustomException(ErrorCode.OPEN_CHAT_NEW_HOST_REQUIRED);
                }
                OpenChatParticipant newHost = openChatParticipantRepository
                        .findByRoomIdAndUserId(roomId, newHostUserId)
                        .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_PARTICIPANT_NOT_FOUND));
                if (newHost.isHost()) {
                    throw new CustomException(ErrorCode.OPEN_CHAT_ALREADY_HOST);
                }
                newHost.grantHost();
            }
            openChatParticipantRepository.delete(targetParticipant);
            if (!othersExist) {
                openChatRoomRepository.delete(room);
            } else {
                openChatMessageService.sendSystemMessage(roomId, targetUser.getName() + "님이 강제퇴장되었습니다.");
            }
        } else {
            openChatParticipantRepository.delete(targetParticipant);
            openChatMessageService.sendSystemMessage(roomId, targetUser.getName() + "님이 강제퇴장되었습니다.");
        }

        log.info("[OpenChat-Exit] exitType={} roomId={} targetUserId={} actorId={} reason={} processedAt={}",
                isAdmin ? "ADMIN_KICK" : "HOST_KICK", roomId, targetUserId, actorId, reason, Instant.now());
    }

    @Transactional
    public void deleteRoom(Long roomId, Long userId) {
        OpenChatRoom room = openChatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND));

        boolean isHost = openChatParticipantRepository.existsByRoomIdAndUserIdAndIsHost(roomId, userId, true);

        if (!isHost) {
            throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN);
        }

        if (room.isOfficial()) {
            throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN);
        }

        List<OpenChatParticipant> participants = openChatParticipantRepository.findAllByRoomId(roomId);
        openChatParticipantRepository.deleteAll(participants);
        openChatRoomRepository.delete(room);
    }

    @Transactional
    public void grantHost(Long roomId, Long requesterId, Long targetUserId) {
        openChatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND));

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        boolean isAdmin = requester.getRole() == Role.ROLE_ADMIN;

        if (!isAdmin) {
            boolean requesterIsHost = openChatParticipantRepository.existsByRoomIdAndUserIdAndIsHost(roomId, requesterId, true);
            if (!requesterIsHost) {
                throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN);
            }
        }

        OpenChatParticipant target = openChatParticipantRepository
                .findByRoomIdAndUserId(roomId, targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_PARTICIPANT_NOT_FOUND));

        if (target.isHost()) {
            throw new CustomException(ErrorCode.OPEN_CHAT_ALREADY_HOST);
        }

        target.grantHost();
    }

    @Transactional
    public void transferHost(Long roomId, Long requesterId, Long targetUserId) {
        openChatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND));

        if (requesterId.equals(targetUserId)) {
            throw new CustomException(ErrorCode.OPEN_CHAT_ALREADY_HOST);
        }

        OpenChatParticipant requester = openChatParticipantRepository
                .findByRoomIdAndUserId(roomId, requesterId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_PARTICIPANT_NOT_FOUND));

        if (!requester.isHost()) {
            throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN);
        }

        OpenChatParticipant target = openChatParticipantRepository
                .findByRoomIdAndUserId(roomId, targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_PARTICIPANT_NOT_FOUND));

        target.grantHost();
        requester.revokeHost();
    }

    @Transactional
    public void revokeHostByAdmin(Long roomId, Long actorId, Long targetUserId) {
        openChatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND));

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (actor.getRole() != Role.ROLE_ADMIN) {
            throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN);
        }

        OpenChatParticipant target = openChatParticipantRepository
                .findByRoomIdAndUserId(roomId, targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_PARTICIPANT_NOT_FOUND));

        if (!target.isHost()) {
            throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN);
        }

        target.revokeHost();
    }

    @Transactional(readOnly = true)
    public ResponseOpenChatParticipantListDto getParticipants(Long roomId, Long requesterId) {
        openChatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND));

        if (!openChatParticipantRepository.existsByRoomIdAndUserId(roomId, requesterId)) {
            throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN);
        }

        List<OpenChatParticipant> participants = openChatParticipantRepository.findAllByRoomId(roomId);

        List<Long> userIds = participants.stream().map(OpenChatParticipant::getUserId).toList();
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, String> nicknameMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u.getName() != null ? u.getName() : ""));
        Map<Long, Boolean> adminMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u.getRole() == Role.ROLE_ADMIN));

        List<ResponseOpenChatParticipantDto> dtos = participants.stream()
                .sorted((a, b) -> a.getJoinedAt().compareTo(b.getJoinedAt()))
                .map(p -> ResponseOpenChatParticipantDto.of(
                        p,
                        nicknameMap.getOrDefault(p.getUserId(), ""),
                        p.isHost(),
                        adminMap.getOrDefault(p.getUserId(), false)))
                .toList();

        int hostCount = (int) participants.stream().filter(OpenChatParticipant::isHost).count();
        return ResponseOpenChatParticipantListDto.of(roomId, dtos, hostCount);
    }

    @Transactional(readOnly = true)
    public ResponseSimpleParticipantListDto getSimpleParticipants(Long roomId, Long requesterId) {
        openChatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND));

        if (!openChatParticipantRepository.existsByRoomIdAndUserId(roomId, requesterId)) {
            throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN);
        }

        List<OpenChatParticipant> participants = openChatParticipantRepository.findAllByRoomId(roomId);

        List<Long> userIds = participants.stream().map(OpenChatParticipant::getUserId).toList();
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, String> nameMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u.getName() != null ? u.getName() : ""));

        List<ResponseSimpleParticipantDto> dtos = participants.stream()
                .map(p -> ResponseSimpleParticipantDto.of(p.getUserId(), nameMap.getOrDefault(p.getUserId(), "")))
                .toList();

        return ResponseSimpleParticipantListDto.of(roomId, dtos);
    }

    private List<ResponseOpenChatRoomDto> buildOpenChatDtos(List<OpenChatRoom> rooms, Long userId, boolean withUnread) {
        if (rooms.isEmpty()) return Collections.emptyList();
        List<Long> roomIds = rooms.stream().map(OpenChatRoom::getId).toList();

        Map<Long, Long> countMap = openChatParticipantRepository != null
                ? openChatParticipantRepository.countByRoomIds(roomIds)
                : Collections.emptyMap();
        Set<Long> joinedRoomIds = openChatParticipantRepository != null
                ? openChatParticipantRepository.findJoinedRoomIds(userId, roomIds)
                : Collections.emptySet();

        if (withUnread && openChatParticipantRepository != null && openChatMessageRepository != null) {
            Map<Long, Long> lastReadMap = openChatParticipantRepository.findLastReadMessageIdsByUserId(userId, roomIds);
            return rooms.stream()
                    .map(room -> {
                        Long lastReadMessageId = lastReadMap.get(room.getId());
                        int unread = (int) openChatMessageRepository.countByRoomIdAndIdGreaterThan(room.getId(), lastReadMessageId);
                        return ResponseOpenChatRoomDto.from(room,
                                countMap.getOrDefault(room.getId(), 0L).intValue(),
                                joinedRoomIds.contains(room.getId()),
                                unread);
                    }).toList();
        }

        return rooms.stream()
                .map(room -> ResponseOpenChatRoomDto.from(
                        room,
                        countMap.getOrDefault(room.getId(), 0L).intValue(),
                        joinedRoomIds.contains(room.getId())))
                .toList();
    }

    private List<ResponseOpenChatRoomDto> buildOpenChatDtosWithUnread(List<OpenChatRoom> rooms, Long userId) {
        return buildOpenChatDtos(rooms, userId, true);
    }

    private Page<ResponseOpenChatRoomDto> toPageDto(List<OpenChatRoom> rooms, Long userId, Pageable pageable) {
        if (rooms.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        List<Long> roomIds = rooms.stream().map(OpenChatRoom::getId).toList();
        Map<Long, Long> countMap = openChatParticipantRepository.countByRoomIds(roomIds);
        Set<Long> joinedRoomIds = openChatParticipantRepository.findJoinedRoomIds(userId, roomIds);
        List<ResponseOpenChatRoomDto> dtos = rooms.stream()
                .map(room -> ResponseOpenChatRoomDto.from(
                        room,
                        countMap.getOrDefault(room.getId(), 0L).intValue(),
                        joinedRoomIds.contains(room.getId())))
                .toList();
        return new PageImpl<>(dtos, pageable, dtos.size());
    }

    private ResponseOpenChatRoomDetailDto toDetailDto(OpenChatRoom room, Long roomId) {
        long count = openChatParticipantRepository.countByRoomId(roomId);
        return ResponseOpenChatRoomDetailDto.builder()
                .roomId(room.getId())
                .name(room.getName())
                .description(room.getDescription())
                .scope(room.getScope())
                .currentParticipants((int) count)
                .maxParticipants(room.getMaxParticipants())
                .isOfficial(room.isOfficial())
                .createdAt(room.getCreatedDate())
                .build();
    }

    private ResponseOpenChatRoomDetailDto toDetailDtoWithBlockedCheck(OpenChatRoom room, Long roomId, Long userId) {
        ResponseOpenChatRoomDetailDto dto = toDetailDto(room, roomId);
        if (room.getRoomType() == OpenChatRoomType.PERSONAL) {
            openChatParticipantRepository.findAllByRoomId(roomId).stream()
                    .filter(p -> !p.getUserId().equals(userId))
                    .findFirst()
                    .ifPresent(p -> {
                        if (blockService.isBlockedBy(p.getUserId(), userId)) {
                            dto.updateIsBlockedByPartner(true);
                        }
                    });
        }
        return dto;
    }
}
