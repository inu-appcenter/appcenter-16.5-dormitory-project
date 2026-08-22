# BR-747 — 어드민 챗봇 메시지 전송 도메인 설계

---

## 엔티티 / 값 객체

### OpenChatMessage (기존 — 변경 없음)
| 필드 | 타입 | 제약 |
|---|---|---|
| id | Long | PK |
| roomId | Long | NOT NULL |
| senderId | Long | NOT NULL |
| content | TEXT | NOT NULL |
| type | OpenChatMessageType | NOT NULL |

챗봇 메시지는 `type = BOT`, `senderId = 어드민 userId`로 저장된다.

### OpenChatRoom (기존 — 변경 없음)
Bot 메시지 전송 후 `updateLastMessage()` 호출로 `lastMessage`, `lastMessageAt` 갱신.

---

## 애그리거트 경계

- `OpenChatRoom` 단독 애그리거트 루트 (기존 구조 유지)
- `OpenChatMessage`는 `roomId`(ID 참조)로 방과 연결 (기존 구조 유지)
- Bot 메시지는 기존 메시지와 동일한 애그리거트 경계 내에서 처리

---

## 연관관계

기존 구조와 동일. 신규 연관관계 없음.

- `OpenChatMessage.roomId` → `OpenChatRoom.id` (ID 참조, FK 아님)
- `OpenChatMessage.senderId` → `User.id` (ID 참조, FK 아님)

---

## DB 스키마 변경

**없음.**

`open_chat_message.type` 컬럼은 `@Enumerated(EnumType.STRING)` → VARCHAR 저장.  
`OpenChatMessageType` Java enum에 `BOT` 추가만으로 충분하며 DDL 변경은 필요 없다.

---

## 도메인 계층 구조

```
domain/openChat/
├── controller/
│   └── OpenChatRoomAdminController.java       ← 수정: bot 엔드포인트 2개 추가
├── service/
│   └── OpenChatMessageService.java            ← 수정: sendBotMessage() 추가
├── repository/
│   └── OpenChatRoomRepository.java            ← 수정: findByRoomTypeNot() 추가
├── dto/
│   ├── request/
│   │   └── RequestAdminBotMessageDto.java     ← 신규
│   └── response/
│       ├── ResponseAdminChatRoomDto.java       ← 신규
│       └── ResponseOpenChatMessageDto.java    ← 수정: isBot 필드 추가
└── enums/
    └── OpenChatMessageType.java               ← 수정: BOT 추가

global/
├── config/
│   └── SecurityConfig.java                    ← 수정: /admin/open-chat-rooms/bot/** ADMIN 인가 추가
└── exception/
    └── ErrorCode.java                         ← 수정: OPEN_CHAT_BOT_TARGET_PERSONAL 추가
```

### 신규 클래스

**`RequestAdminBotMessageDto`**
```java
@NotBlank
@Size(max = 500)
private String content;
```

**`ResponseAdminChatRoomDto`**
```java
private Long roomId;
private String roomName;

public static ResponseAdminChatRoomDto from(OpenChatRoom room) { ... }
```

### 수정 클래스 요약

| 클래스 | 변경 내용 |
|---|---|
| `OpenChatMessageType` | `BOT` 값 추가 |
| `ResponseOpenChatMessageDto` | `isBot` 필드 추가, `from()` 메서드에서 `type == BOT`으로 파생 |
| `OpenChatRoomRepository` | `findByRoomTypeNot(OpenChatRoomType type)` JPA 파생 메서드 추가 |
| `OpenChatMessageService` | `sendBotMessage(Long adminId, Long roomId, String content)` 추가 |
| `OpenChatRoomAdminController` | `GET /bot`, `POST /bot/{roomId}` 엔드포인트 추가 |
| `SecurityConfig` | `/admin/open-chat-rooms/bot/**` → `hasRole("ADMIN")` 추가 |
| `ErrorCode` | `OPEN_CHAT_BOT_TARGET_PERSONAL(BAD_REQUEST, 22028, ...)` 추가 |

---

## sendBotMessage 처리 흐름

```
OpenChatRoomAdminController.sendBotMessage(adminId, roomId, request)
  └── OpenChatMessageService.sendBotMessage(adminId, roomId, content)
        1. openChatRoomRepository.findById(roomId) → 없으면 OPEN_CHAT_ROOM_NOT_FOUND(404)
        2. room.getRoomType() == PERSONAL → OPEN_CHAT_BOT_TARGET_PERSONAL(400)
        3. userRepository.findById(adminId) → sender
        4. OpenChatMessage.create(roomId, adminId, content, BOT) 저장
        5. room.updateLastMessage(content, message.createdDate)
        6. sessionRegistry로 구독 중인 유저 lastReadMessageId 갱신
        7. messagingTemplate → /sub/openchat/{roomId} 브로드캐스트
        8. openChatNotificationService.sendImmediateNotifications(...) FCM 발송
```

참여자 조회(`OpenChatParticipantRepository`) 없이 바로 메시지 저장. FCM 발송은 기존 `sendImmediateNotifications` 재사용 (EVERY 모드 참여자 대상, 온라인 유저 제외).

---

## 비목표

- 이미지·룸링크·학번 요청 타입의 챗봇 메시지 — 제외
- 챗봇 전용 유저 엔티티·고정 계정 — 어드민 계정 재사용
- 챗봇 발송 감사 로그 — 제외
- 목록 페이지네이션 — 전체 단순 반환
- 목록에 인원수·마지막 메시지 등 추가 정보 — roomId + roomName만
- 기존 `/admin/open-chat-rooms/dorm` 엔드포인트의 인가 수정 — 이번 범위 아님
