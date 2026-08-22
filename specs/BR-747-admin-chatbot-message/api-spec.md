# BR-747 어드민 챗봇 메시지 API 명세서

> Base URL: `/admin/open-chat-rooms`  
> 인증: 모든 엔드포인트는 `ROLE_ADMIN` Bearer Token 필요

---

## 오픈채팅방 목록 조회 (어드민 전용)

| 항목 | 내용 |
|------|------|
| **메서드** | `GET` |
| **경로** | `/admin/open-chat-rooms` |
| **인증** | Bearer Token (ROLE_ADMIN) |
| **설명** | 챗봇 메시지 전송 대상 선택을 위한 PERSONAL 타입 제외 전체 채팅방 목록 반환 |

### Request

파라미터 없음.

### Response

#### 성공 응답 — `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| `roomId` | `Long` | 채팅방 ID |
| `roomName` | `String` | 채팅방 이름 |

```json
[
  {
    "roomId": 1,
    "roomName": "1기숙사 자유채팅"
  },
  {
    "roomId": 2,
    "roomName": "기숙사 공지방"
  }
]
```

#### 에러 응답

| 상태 코드 | 발생 조건 | 응답 예시 |
|-----------|-----------|-----------|
| `401 Unauthorized` | 토큰 없음 또는 만료 | `{"code": 10001, "name": "JWT_ENTRY_POINT", "message": "..."}` |
| `403 Forbidden` | ADMIN 권한 없음 | Spring Security 기본 403 |

---

## 챗봇 메시지 전송 (어드민 전용)

| 항목 | 내용 |
|------|------|
| **메서드** | `POST` |
| **경로** | `/admin/open-chat-rooms/{roomId}/bot` |
| **인증** | Bearer Token (ROLE_ADMIN) |
| **설명** | 지정 채팅방에 `type=BOT` 메시지를 전송한다. 어드민이 참여자가 아니어도 가능. 성공 시 WebSocket 브로드캐스트 및 FCM 푸시 발송. |

### Request

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `roomId` | `Long` | ✅ | 메시지를 보낼 채팅방 ID |

#### Request Body

Content-Type: `application/json`

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `content` | `String` | ✅ | NotBlank, 최대 500자 | 전송할 메시지 내용 |

```json
{
  "content": "안녕하세요, 기숙사 공지입니다."
}
```

### Response

#### 성공 응답 — `201 Created`

Body 없음.

#### 에러 응답

| 상태 코드 | 에러명 | 발생 조건 |
|-----------|--------|-----------|
| `400 Bad Request` | `VALIDATION_FAILED` | `content` 누락·공백·500자 초과 |
| `400 Bad Request` | `OPEN_CHAT_BOT_TARGET_PERSONAL` | `roomType = PERSONAL`인 개인 채팅방 |
| `401 Unauthorized` | `JWT_ENTRY_POINT` | 토큰 없음 또는 만료 |
| `403 Forbidden` | — | ADMIN 권한 없음 |
| `404 Not Found` | `OPEN_CHAT_ROOM_NOT_FOUND` | 존재하지 않는 `roomId` |

에러 응답 형식:
```json
{
  "code": 22028,
  "name": "OPEN_CHAT_BOT_TARGET_PERSONAL",
  "message": "[OpenChat] 개인 채팅방에는 챗봇 메시지를 전송할 수 없습니다."
}
```

`VALIDATION_FAILED` 시 `errors` 배열 추가:
```json
{
  "code": 10002,
  "name": "VALIDATION_FAILED",
  "message": "DTO에서 요청한 값이 올바르지 않습니다.",
  "errors": ["content: must not be blank"]
}
```

---

## WebSocket 브로드캐스트 페이로드

챗봇 메시지 전송 성공 시 `/sub/openchat/{roomId}` 채널로 아래 페이로드가 발행된다.

| 필드 | 타입 | 설명 |
|------|------|------|
| `messageId` | `Long` | 저장된 메시지 ID |
| `roomId` | `Long` | 채팅방 ID |
| `senderId` | `Long` | 어드민 유저 ID |
| `senderNickname` | `String` | 어드민 이름 |
| `content` | `String` | 메시지 내용 |
| `type` | `String` | `"BOT"` |
| `isBot` | `Boolean` | `true` |
| `imageUrls` | `String[]` | `[]` (항상 빈 배열) |
| `unreadCount` | `Int` | 미읽음 수 |
| `createdAt` | `String (ISO 8601)` | 메시지 생성 시각 |
| `linkedRoomId` | `Long \| null` | `null` |
| `linkedRoomName` | `String \| null` | `null` |
| `disclosureRequestId` | `Long \| null` | `null` |

```json
{
  "messageId": 1234,
  "roomId": 1,
  "senderId": 42,
  "senderNickname": "관리자",
  "content": "안녕하세요, 기숙사 공지입니다.",
  "type": "BOT",
  "isBot": true,
  "imageUrls": [],
  "unreadCount": 5,
  "createdAt": "2026-08-20T10:30:00"
}
```

---

## 변경되는 기존 응답 필드

### ResponseOpenChatMessageDto — `isBot` 필드 추가

기존 채팅 메시지 조회·수신 응답에 `isBot` 필드가 추가된다.

| 필드 | 타입 | 설명 |
|------|------|------|
| `isBot` | `Boolean` | `type == BOT`이면 `true`, 그 외 `false` |

기존 `TEXT`, `IMAGE`, `SYSTEM`, `ROOM_LINK`, `STUDENT_ID_REQUEST` 타입 메시지는 `isBot: false`로 반환된다.
