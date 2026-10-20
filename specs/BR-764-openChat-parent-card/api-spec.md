# BR-764 부모 오픈채팅 카드 관리 API 명세서

> Base URL: `/open-chat-rooms`
> 인증: 모든 엔드포인트에 Bearer Token 필요 (`@AuthenticationPrincipal`)

이번 BR은 새 엔드포인트를 추가하지 않는다. 기존 3개 API의 동작·응답 필드가 확장되며, WebSocket 토픽이 1개 추가된다.

---

## 1. 파생 톡방 생성

| 항목 | 내용 |
|------|------|
| **메서드** | `POST` |
| **경로** | `/open-chat-rooms/derived` |
| **인증** | Bearer Token |
| **변경 내용** | `originRoomId`를 생성된 파생 방 엔티티에 저장. 응답 스키마는 그대로. |

### Request

#### Request Body
Content-Type: `application/json`

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `originRoomId` | `Long` | ✅ | 파생 방이 속할 부모 방 ID |
| `name` | `String` | ✅ | 방 이름 (1–30자) |
| `description` | `String` | ❌ | 방 설명 (최대 100자) |
| `maxParticipants` | `Int` | ✅ | 최대 인원 (2–100) |
| `isPublic` | `Boolean` | ✅ | 탭 노출 여부 |
| `password` | `String` | ❌ | 비밀번호 (최대 50자) |

```json
{
  "originRoomId": 10,
  "name": "1관 같이 공부해요",
  "description": "밤 11시까지 스터디",
  "maxParticipants": 20,
  "isPublic": true
}
```

### Response

#### 성공 응답 — `201 Created`

| 필드 | 타입 | 설명 |
|------|------|------|
| `roomId` | `Long` | 생성된 파생 톡방 ID |

```json
{
  "roomId": 42
}
```

#### 에러 응답

| 상태 코드 | 에러 코드 | 발생 조건 |
|-----------|-----------|-----------|
| `400 Bad Request` | — | `@Valid` 실패 (필드 누락, 범위 초과) |
| `404 Not Found` | `OPEN_CHAT_ROOM_NOT_FOUND` | `originRoomId` 방 없음 |
| `404 Not Found` | `USER_NOT_FOUND` | 요청자 유저 없음 |
| `409 Conflict` | `OPEN_CHAT_PARTICIPANT_NOT_FOUND` | 요청자가 부모 방 참여자 아님 |

---

## 2. 파생 톡방 모집 상태 변경

| 항목 | 내용 |
|------|------|
| **메서드** | `PATCH` |
| **경로** | `/open-chat-rooms/{roomId}/recruitment-status` |
| **인증** | Bearer Token |
| **변경 내용** | 상태 변경 성공 후 부모 방에 WebSocket 이벤트 전송. CLOSED→OPEN 시 재모집 카드 메시지 자동 생성. |

### Request

#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `roomId` | `Long` | ✅ | 모집 상태를 변경할 파생 톡방 ID |

#### Request Body
Content-Type: `application/json`

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `status` | `OpenChatRoomRecruitmentStatus` | ✅ | `"OPEN"` 또는 `"CLOSED"` |

```json
{
  "status": "OPEN"
}
```

### Response

#### 성공 응답 — `204 No Content`

응답 바디 없음.

**성공 후 사이드이펙트 (클라이언트가 인지해야 할 비동기 동작):**
- `originRoomId`가 있는 파생 방이면 부모 방 WebSocket 토픽으로 이벤트 전송 (§4 참조)
- CLOSED→OPEN 이면 부모 방에 `REOPEN_CARD` 메시지 생성 및 브로드캐스트

#### 에러 응답

| 상태 코드 | 에러 코드 | 발생 조건 |
|-----------|-----------|-----------|
| `400 Bad Request` | — | `status` 필드 누락 또는 잘못된 enum 값 |
| `400 Bad Request` | `OPEN_CHAT_ROOM_NOT_DERIVED` | `roomType != DERIVED` |
| `403 Forbidden` | `OPEN_CHAT_ROOM_FORBIDDEN` | 방장도 관리자도 아닌 사용자 |
| `404 Not Found` | `OPEN_CHAT_ROOM_NOT_FOUND` | 방 없음 |
| `404 Not Found` | `USER_NOT_FOUND` | 요청자 유저 없음 |
| `409 Conflict` | `OPEN_CHAT_ROOM_ALREADY_OPEN` | 이미 OPEN인 방을 OPEN으로 요청 |
| `409 Conflict` | `OPEN_CHAT_ROOM_ALREADY_CLOSED` | 이미 CLOSED인 방을 CLOSED로 요청 |

---

## 3. 메시지 목록 조회 — ROOM_LINK / REOPEN_CARD 응답 변경

| 항목 | 내용 |
|------|------|
| **메서드** | `GET` |
| **경로** | `/open-chat-rooms/{roomId}/messages` |
| **인증** | Bearer Token |
| **변경 내용** | `ROOM_LINK`, `REOPEN_CARD` 타입 메시지에 `linkedRoomRecruitmentStatus` 필드 추가 |

### Request

#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `roomId` | `Long` | ✅ | 메시지를 조회할 방 ID |

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| `lastMessageId` | `Long` | ❌ | — | 커서 기반 페이징. 이 ID 이전 메시지를 반환 |
| `size` | `Int` | ❌ | `30` | 페이지 크기 |

### Response

#### 성공 응답 — `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| `messages` | `List<ResponseOpenChatMessageDto>` | 메시지 목록 |
| `hasNext` | `Boolean` | 다음 페이지 존재 여부 |
| `nextCursor` | `Long` (nullable) | 다음 페이지 커서 |

**ResponseOpenChatMessageDto 공통 필드:**

| 필드 | 타입 | 설명 |
|------|------|------|
| `messageId` | `Long` | 메시지 ID |
| `roomId` | `Long` | 방 ID |
| `senderId` | `Long` | 발신자 유저 ID |
| `senderNickname` | `String` (nullable) | 발신자 닉네임 (SYSTEM 타입은 null) |
| `content` | `String` | 메시지 내용 (ROOM_LINK / REOPEN_CARD는 JSON 원문) |
| `type` | `OpenChatMessageType` | `TEXT` \| `IMAGE` \| `SYSTEM` \| `ROOM_LINK` \| `STUDENT_ID_REQUEST` \| `BOT` \| `REOPEN_CARD` |
| `imageUrls` | `List<String>` | 이미지 URL 목록 (IMAGE 타입만 비어있지 않음) |
| `unreadCount` | `Int` | 안 읽은 수 |
| `createdAt` | `LocalDateTime` | 생성 시각 |
| `isBot` | `Boolean` | BOT 메시지 여부 |

**ROOM_LINK / REOPEN_CARD 타입 추가 필드:**

| 필드 | 타입 | 설명 |
|------|------|------|
| `linkedRoomId` | `Long` | 연결된 파생 톡방 ID |
| `linkedRoomName` | `String` | 파생 톡방 이름 |
| `linkedRoomDescription` | `String` (nullable) | 파생 톡방 설명 |
| `linkedRoomMaxParticipants` | `Int` | 최대 인원 |
| `linkedRoomRecruitmentClosed` | `Boolean` | 모집 마감 여부 (기존 필드, 하위 호환) |
| `linkedRoomRecruitmentStatus` | `OpenChatRoomRecruitmentStatus` | **[신규]** `"OPEN"` 또는 `"CLOSED"` |

```json
{
  "messages": [
    {
      "messageId": 201,
      "roomId": 10,
      "senderId": 5,
      "senderNickname": "김기숙",
      "content": "{\"derivedRoomId\":42,...}",
      "type": "REOPEN_CARD",
      "imageUrls": [],
      "unreadCount": 3,
      "createdAt": "2026-10-03T14:00:00",
      "isBot": false,
      "linkedRoomId": 42,
      "linkedRoomName": "1관 같이 공부해요",
      "linkedRoomDescription": "밤 11시까지 스터디",
      "linkedRoomMaxParticipants": 20,
      "linkedRoomRecruitmentClosed": false,
      "linkedRoomRecruitmentStatus": "OPEN"
    }
  ],
  "hasNext": false,
  "nextCursor": null
}
```

---

## 4. WebSocket 이벤트 — 모집 상태 변경 알림 (신규)

| 항목 | 내용 |
|------|------|
| **프로토콜** | WebSocket / STOMP |
| **토픽** | `/sub/openchat/{parentRoomId}/recruitment-status` |
| **발행 시점** | 파생 톡방의 모집 상태 변경 성공 직후 (OPEN→CLOSED, CLOSED→OPEN 모두) |
| **발행 주체** | 서버 (`OpenChatMessageService.sendRecruitmentStatusEvent`) |

### 이벤트 페이로드

| 필드 | 타입 | 설명 |
|------|------|------|
| `derivedRoomId` | `Long` | 모집 상태가 변경된 파생 톡방 ID |
| `recruitmentStatus` | `OpenChatRoomRecruitmentStatus` | 변경 후 상태 `"OPEN"` 또는 `"CLOSED"` |

```json
{
  "derivedRoomId": 42,
  "recruitmentStatus": "CLOSED"
}
```

> 이 이벤트를 구독한 클라이언트는 부모 방에 표시된 카드의 모집 상태를 즉시 갱신할 수 있다.

---

## 5. WebSocket 이벤트 — 재모집 카드 메시지 (기존 토픽 활용)

| 항목 | 내용 |
|------|------|
| **프로토콜** | WebSocket / STOMP |
| **토픽** | `/sub/openchat/{parentRoomId}` |
| **발행 시점** | CLOSED→OPEN 재개 성공 후 (중복 방지 키 통과 시에만) |
| **발행 주체** | 서버 (`OpenChatMessageService.sendReopenCardMessage`) |

### 이벤트 페이로드

`ResponseOpenChatMessageDto` 구조와 동일 (`type: "REOPEN_CARD"`).

```json
{
  "messageId": 201,
  "roomId": 10,
  "senderId": 5,
  "senderNickname": "김기숙",
  "content": "{\"derivedRoomId\":42,\"roomName\":\"1관 같이 공부해요\",\"description\":\"밤 11시까지 스터디\",\"maxParticipants\":20}",
  "type": "REOPEN_CARD",
  "imageUrls": [],
  "unreadCount": 0,
  "createdAt": "2026-10-03T14:05:00",
  "isBot": false,
  "linkedRoomId": 42,
  "linkedRoomName": "1관 같이 공부해요",
  "linkedRoomDescription": "밤 11시까지 스터디",
  "linkedRoomMaxParticipants": 20,
  "linkedRoomRecruitmentClosed": false,
  "linkedRoomRecruitmentStatus": "OPEN"
}
```
