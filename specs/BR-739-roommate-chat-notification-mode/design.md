# BR-739 Design — 룸메이트 채팅방 알림 모드

---

## 엔티티 / 값 객체

### RoommateChattingRoom (기존 엔티티 수정)

| 추가 필드 | 타입 | nullable | 기본값 | 설명 |
|---|---|---|---|---|
| `hostNotificationMode` | `ChatNotificationMode` (enum) | false | `EVERY` | host 알림 모드 |
| `guestNotificationMode` | `ChatNotificationMode` (enum) | false | `EVERY` | guest 알림 모드 |

- `ChatNotificationMode` (EVERY / BUNDLED / OFF) — 오픈채팅의 기존 enum 재사용
  - 패키지: `com.example.appcenter_project.domain.openChat.enums.ChatNotificationMode`
- 엔티티에 두 개의 update 메서드 추가:
  - `updateHostNotificationMode(ChatNotificationMode mode)`
  - `updateGuestNotificationMode(ChatNotificationMode mode)`

### RoommateUnreadNotificationInfo (신규 record)

BUNDLED 스케줄러용 집계 결과 전달 record.

```java
package com.example.appcenter_project.domain.roommate.dto;

public record RoommateUnreadNotificationInfo(Long userId, Long roomId, long unreadCount) {}
```

---

## 애그리거트 경계

- `RoommateChattingRoom`이 알림 모드를 직접 소유. 별도 participant 엔티티 없음 (1:1 채팅방, 항상 host/guest 2명 고정).
- 알림 모드는 채팅방의 내부 상태이므로 `RoommateChattingRoom` 애그리거트 루트 안에서 관리.

---

## 연관관계

신규 연관관계 없음. `RoommateChattingRoom`에 enum 컬럼 2개만 추가.

---

## DB 스키마 변경

```sql
ALTER TABLE roommate_chatting_room
    ADD COLUMN host_notification_mode VARCHAR(10) NOT NULL DEFAULT 'EVERY',
    ADD COLUMN guest_notification_mode VARCHAR(10) NOT NULL DEFAULT 'EVERY';
```

- `FcmRoutingType` enum에 `CHAT_ROOMMATE` 상수 추가 (DB 스키마 변경 없음, enum 코드 변경).

---

## 도메인 계층 구조

### 신규 생성

```
domain/roommate/
├── controller/
│   ├── RoommateChattingNotificationController.java         # 신규
│   └── RoommateChattingNotificationApiSpecification.java   # 신규
├── service/
│   ├── RoommateChattingNotificationService.java            # 신규 (BUNDLED 스케줄러 로직)
│   └── RoommateChattingNotificationScheduler.java          # 신규 (@Scheduled wrapper)
├── repository/
│   ├── RoommateChattingChatQuerydslRepository.java         # 기존 인터페이스에 메서드 추가
│   └── RoommateChattingChatQuerydslRepositoryImpl.java     # 기존 Impl에 쿼리 추가
└── dto/
    └── RoommateUnreadNotificationInfo.java                  # 신규 record
```

### 기존 수정

| 파일 | 변경 내용 |
|---|---|
| `entity/RoommateChattingRoom.java` | `hostNotificationMode`, `guestNotificationMode` 필드 + update 메서드 2개 추가 |
| `service/RoommateChattingChatService.java` | `sendChatNotification()` 내부: EVERY 때만 즉시 FCM, OFF/BUNDLED 건너뜀 |
| `enums/FcmRoutingType.java` | `CHAT_ROOMMATE` 상수 추가 + `threadId`, `path`, `dataKey`, `dataType` switch 분기 추가 |

---

## 클래스별 상세 책임

### RoommateChattingNotificationController

- `PATCH /roommate-chatting-room/{roomId}/notification-mode` → `updateNotificationMode(userId, roomId, mode)` → 200 `ResponseNotificationModeDto`
- `GET  /roommate-chatting-room/{roomId}/notification-mode` → `getNotificationMode(userId, roomId)` → 200 `ResponseNotificationModeDto`
- Request DTO: 오픈채팅의 `RequestUpdateNotificationModeDto` 재사용
- Response DTO: 오픈채팅의 `ResponseNotificationModeDto` 재사용

### RoommateChattingNotificationService

```
updateNotificationMode(userId, roomId, mode):
  room = findById(roomId) or 404
  isHost = room.host.id == userId
  isGuest = room.guest.id == userId
  if (!isHost && !isGuest) → 403 ROOMMATE_CHAT_ROOM_FORBIDDEN
  isHost ? room.updateHostNotificationMode(mode)
          : room.updateGuestNotificationMode(mode)
  return ResponseNotificationModeDto.of(mode)

getNotificationMode(userId, roomId):
  room = findById(roomId) or 404
  isHost = room.host.id == userId
  isGuest = room.guest.id == userId
  if (!isHost && !isGuest) → 403 ROOMMATE_CHAT_ROOM_FORBIDDEN
  return ResponseNotificationModeDto.of(isHost ? room.hostNotificationMode : room.guestNotificationMode)

sendHourlyUnreadNotifications():
  unreadInfos = chatQuerydslRepo.findUnreadCountsForBundled()   // BUNDLED 모드 + unread > 0
  if empty → return
  userIds = unreadInfos.stream().map(userId)
  tokenMap = fcmTokenRepo.findAllByUserIdIn(userIds)
            .collect(toMap(userId → token, keepFirst))
  outboxes = unreadInfos
            .filter(tokenMap has userId)
            .map(info → FcmOutbox.create(token, roomName, "새 메시지 N개", CHAT_ROOMMATE, roomId))
  fcmOutboxRepo.saveAll(outboxes)
```

### RoommateChattingChatQuerydslRepositoryImpl — 신규 메서드

```
findUnreadCountsForBundled():
  SELECT room.id, receiver.id, COUNT(chat.id)
  FROM roommate_chatting_room room
  JOIN roommate_chatting_chat chat ON chat.roommate_chatting_room_id = room.id
  WHERE chat.read_by_receiver = false
    AND chat.member IS NOT NULL   -- 시스템 메시지 제외
    AND (
      (chat.member_id = room.host_id   AND room.guest_notification_mode = 'BUNDLED')
      OR
      (chat.member_id = room.guest_id  AND room.host_notification_mode  = 'BUNDLED')
    )
  GROUP BY room.id, receiverId
```

- `receiver`: sender가 host이면 guest가 수신자, sender가 guest이면 host가 수신자
- QueryDSL로 구현; `RoommateUnreadNotificationInfo(userId=receiverId, roomId, unreadCount)` 반환

### RoommateChattingChatService.sendChatNotification() 변경

```java
// Before
fcmMessageService.sendNotification(receiver, title, body);

// After
ChatNotificationMode mode = isReceiverHost
    ? room.getHostNotificationMode()
    : room.getGuestNotificationMode();

if (mode == ChatNotificationMode.EVERY) {
    // FcmOutbox 직접 저장 (fcmMessageService 경유)
    fcmMessageService.sendNotification(receiver, title, body);
}
// BUNDLED, OFF → 즉시 발송 안 함
```

- `isReceiverHost` 판단: `room.getHost().getId().equals(receiver.getId())`

### RoommateChattingNotificationScheduler

```java
@Scheduled(cron = "0 0 * * * *")
public void sendHourlyNotifications() {
    roommateChattingNotificationService.sendHourlyUnreadNotifications();
}
```

### FcmRoutingType.CHAT_ROOMMATE 추가

| 메서드 | 반환값 |
|---|---|
| `threadId(id)` | `"chat_room_" + id` |
| `path(id)` | `"/roommate/chat/" + id` |
| `dataKey()` | `"chatRoomId"` |
| `dataType()` | `"CHAT_ROOMMATE"` |

---

## 비목표

- `RoommateChattingRoom` 외 다른 엔티티 수정 없음
- 기존 `OpenChatNotificationService` / `OpenChatNotificationScheduler` 수정 없음
- 룸메이트 채팅방 FCM 라우팅 경로(`/roommate/chat/{id}`)는 신규 `CHAT_ROOMMATE`에서만 적용; 기존 `sendChatNotification` 내 `CHAT` 타입 레거시 코드는 건드리지 않음
- 알림 모드 변경 이력(audit log) 없음
- BUNDLED 주기 설정화 없음 (매 정각 고정)
