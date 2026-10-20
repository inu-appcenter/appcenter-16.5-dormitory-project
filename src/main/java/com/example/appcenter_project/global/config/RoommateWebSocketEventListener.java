package com.example.appcenter_project.global.config;

import com.example.appcenter_project.domain.roommate.service.RoommateChattingChatService;
import com.example.appcenter_project.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoommateWebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;
    private final RoommateChattingChatService chatService;
    private final JwtTokenProvider jwtTokenProvider;

    // 세션 → 채팅방 ID, 유저 ID
    public static final Map<String, String> roommateChatRoomMap = new ConcurrentHashMap<>();
    public static final Map<String, String> roommateChatRoomUserMap = new ConcurrentHashMap<>();
    public static final Map<String, List<String>> roommateChatRoomInUserMap = new ConcurrentHashMap<>();
    // "sessionId:subscriptionId" → roomId (UNSUBSCRIBE 시 역추적용)
    public static final Map<String, String> roommateSubscriptionRoomMap = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        // sessionAttributes null 체크
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            log.warn("SessionAttributes is null for sessionId: {}", accessor.getSessionId());
            return;
        }

        String userId = (String) sessionAttributes.get("userId");
        log.info("Roommate WebSocket 연결됨. sessionId: {}, userId: {}",
                accessor.getSessionId(), userId);
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();

        if (destination != null && destination.startsWith("/sub/roommate/chat/")) {
            // /sub/roommate/chat/{roomId} 구독만 채팅방 입장으로 처리
            String[] parts = destination.split("/");

            log.info("구독 경로 분석: destination={}, parts={}", destination, Arrays.toString(parts));

            // 배열 길이 검증: ["", "sub", "roommate", "chat", "roomId"] 최소 5개
            if (parts.length < 5) {
                log.warn("Invalid destination format: {}", destination);
                return;
            }

            // parts[0] = "", parts[1] = "sub", parts[2] = "roommate", parts[3] = "chat", parts[4] = roomId
            String roomIdStr = parts[4];

            // roomId가 숫자인지 확인
            Long roomId;
            try {
                roomId = Long.parseLong(roomIdStr);
            } catch (NumberFormatException e) {
                log.warn("Invalid roomId format in destination: {}, roomIdStr: {}", destination, roomIdStr);
                return;
            }

            String userId = null;

            // sessionAttributes null 체크
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                userId = (String) sessionAttributes.get("userId");
            }

            // Authorization 헤더에서 JWT 토큰으로 사용자 ID 추출 (fallback)
            if (userId == null) {
                String authToken = accessor.getFirstNativeHeader("Authorization");
                if (authToken != null && authToken.startsWith("Bearer ")) {
                    try {
                        String token = authToken.substring(7); // "Bearer " 제거
                        userId = jwtTokenProvider.getUserId(token);
                    } catch (Exception e) {
                        log.warn("Failed to extract userId from JWT token: {}", e.getMessage());
                    }
                }
            }

            // userId가 없으면 처리 중단
            if (userId == null) {
                log.warn("UserId not found in destination: {} or session", destination);
                return;
            }

            log.info("WebSocket 구독: sessionId={}, roomId={}, userId={}, destination={}",
                    sessionId, roomId, userId, destination);

            // 기본 채팅방 토픽 구독 시 입장 및 읽음 처리
            roommateChatRoomMap.put(sessionId, roomId.toString());
            roommateChatRoomUserMap.put(sessionId, userId);
            roommateChatRoomInUserMap
                    .computeIfAbsent(roomId.toString(), k -> new ArrayList<>())
                    .add(userId);

            String subscriptionId = accessor.getSubscriptionId();
            if (subscriptionId != null) {
                roommateSubscriptionRoomMap.put(sessionId + ":" + subscriptionId, roomId.toString());
            }

            try {
                chatService.markAsRead(roomId, Long.parseLong(userId));
            } catch (Exception e) {
                log.error("읽음 처리 중 오류 발생: roomId={}, userId={}, error={}", roomId, userId, e.getMessage());
            }
        }
    }

    @EventListener
    public void handleWebSocketUnsubscribeListener(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();

        if (subscriptionId == null) return;

        String roomId = roommateSubscriptionRoomMap.remove(sessionId + ":" + subscriptionId);
        if (roomId == null) return;

        String userId = roommateChatRoomUserMap.get(sessionId);
        if (userId != null) {
            List<String> users = roommateChatRoomInUserMap.get(roomId);
            if (users != null) {
                users.remove(userId);
                if (users.isEmpty()) {
                    roommateChatRoomInUserMap.remove(roomId);
                }
            }
        }

        log.info("Roommate 채팅방 구독 해제: roomId={}, userId={}", roomId, userId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();

        if (roommateChatRoomMap.containsKey(sessionId)) {
            String roomId = roommateChatRoomMap.get(sessionId);
            String userId = roommateChatRoomUserMap.get(sessionId);

            List<String> users = roommateChatRoomInUserMap.get(roomId);
            if (users != null) {
                users.remove(userId);
                if (users.isEmpty()) {
                    roommateChatRoomInUserMap.remove(roomId);
                }
            }

            roommateChatRoomMap.remove(sessionId);
            roommateChatRoomUserMap.remove(sessionId);
            roommateSubscriptionRoomMap.entrySet().removeIf(e -> e.getKey().startsWith(sessionId + ":"));

            log.info("Roommate 채팅방 퇴장: roomId={}, userId={}", roomId, userId);
        }
    }
}
