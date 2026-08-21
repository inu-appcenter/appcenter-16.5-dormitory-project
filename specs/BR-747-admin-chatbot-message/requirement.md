# BR-747 — 어드민 챗봇 메시지 전송

## 기능 요약
어드민이 어떤 오픈채팅방에든 참여 없이 챗봇 타입(`BOT`) 메시지를 전송할 수 있다.
클라이언트는 `isBot: true` 필드로 해당 메시지를 챗봇 말풍선으로 렌더링한다.

---

## 동작 명세

### 1. 오픈채팅방 목록 조회 (어드민 전용)
- **입력**: 없음 (인증된 어드민)
- **처리**: `open_chat_room` 전체를 조회한다. `roomType = PERSONAL`인 개인 채팅방은 제외한다.
- **출력**: `[{ roomId, roomName }]` 목록

### 2. 챗봇 메시지 전송 (어드민 전용)
- **입력**: `roomId` (path), `content` (body, TEXT 한정)
- **처리**:
  1. `roomId`로 채팅방 존재 확인
  2. `OpenChatMessage` 저장 — `senderId = 어드민 userId`, `type = BOT`
  3. `room.updateLastMessage()` 갱신
  4. `lastReadMessageId` 업데이트 (현재 구독 중인 세션)
  5. WebSocket `/topic/chat/{roomId}` 채널로 메시지 브로드캐스트
  6. 채팅방 참여자(어드민 제외)에게 FCM 푸시 발송 — 기존 `openChatNotificationService` 재사용
- **출력**: HTTP 201, body 없음

---

## 도메인 데이터

### OpenChatMessageType 변경
기존 `TEXT, IMAGE, SYSTEM, ROOM_LINK, STUDENT_ID_REQUEST`에 `BOT` 추가.

### ResponseOpenChatMessageDto 변경
기존 필드 유지, `isBot` 필드 추가.

```
isBot = (message.type == BOT)
```

### 신규 DTO

**ResponseAdminChatRoomDto**
| 필드 | 타입 | 설명 |
|---|---|---|
| roomId | Long | 채팅방 ID |
| roomName | String | 채팅방 이름 |

**RequestAdminBotMessageDto**
| 필드 | 타입 | 제약 |
|---|---|---|
| content | String | NotBlank, 최대 500자 |

---

## 비즈니스 규칙 / 제약

1. 호출자는 `ROLE_ADMIN` 권한을 가진 유저여야 한다.
2. 어드민은 채팅방 참여자(`OpenChatParticipant`)가 아니어도 전송 가능하다.
3. 전송 타입은 `TEXT`에 해당하는 내용만 허용한다 (이미지·룸링크·학번 요청 불가).
4. `content`는 공백 불가, 최대 500자.
5. `PERSONAL` 타입 채팅방에는 전송할 수 없다.

---

## 예외 · 경계 상황

| 상황 | 기대 동작 |
|---|---|
| 존재하지 않는 `roomId` | 404 NOT_FOUND |
| `roomType = PERSONAL` 채팅방 | 400 BAD_REQUEST |
| `content` 공백 또는 초과 | 400 BAD_REQUEST (@Valid) |
| ADMIN 권한 없는 유저 | 403 FORBIDDEN (Security 레이어) |

---

## 비목표 (Non-goals)

- 이미지·룸링크·학번 요청 타입의 챗봇 메시지 — 이번 범위 아님
- 챗봇 고정 계정(별도 유저 엔티티) 생성 — 어드민 계정 그대로 사용
- 챗봇 발송 이력 별도 로깅/감사 로그 — 이번 범위 아님
- 채팅방 페이지네이션 — 전체 목록 단순 반환
- 오픈채팅방 목록에 인원수·마지막 메시지 등 추가 정보 — roomId + roomName만

---

## 수용 기준 (Acceptance Criteria)

### 목록 조회
- **Given** 어드민 유저 / OPEN 채팅방 3개 + PERSONAL 채팅방 1개 존재  
  **When** `GET /admin/open-chat-rooms/bot` 호출  
  **Then** PERSONAL 제외한 3개 반환, 각 항목에 `roomId`, `roomName` 포함

### 챗봇 메시지 전송 — 정상
- **Given** 어드민 유저, 존재하는 OPEN 채팅방, 참여자 아님  
  **When** `POST /admin/open-chat-rooms/bot/{roomId}` with `content`  
  **Then** 201, `OpenChatMessage` 저장(`type = BOT`, `senderId = 어드민 id`)

### 챗봇 메시지 — room 업데이트
- **Given** 위 동일  
  **When** 전송 성공  
  **Then** `OpenChatRoom.lastMessage`, `lastMessageAt` 갱신됨

### 챗봇 메시지 — PERSONAL 방 전송 불가
- **Given** 어드민 유저, `roomType = PERSONAL` 채팅방  
  **When** 동일 API 호출  
  **Then** 400 BAD_REQUEST

### 챗봇 메시지 — 존재하지 않는 방
- **Given** 어드민 유저, 없는 roomId  
  **When** 동일 API 호출  
  **Then** 404 NOT_FOUND

### isBot 필드
- **Given** `type = BOT`인 메시지  
  **When** `ResponseOpenChatMessageDto` 생성  
  **Then** `isBot = true`

- **Given** `type = TEXT`인 메시지  
  **When** `ResponseOpenChatMessageDto` 생성  
  **Then** `isBot = false`
