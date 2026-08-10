# BR-739 룸메이트 채팅방 알림 모드 API 명세서

> Base URL: `https://{host}`
> 모든 엔드포인트는 JWT Bearer Token 인증 필요

---

## 알림 모드 변경

| 항목 | 내용 |
|------|------|
| **메서드** | `PATCH` |
| **경로** | `/roommate-chatting-room/{roomId}/notification-mode` |
| **인증** | Bearer Token |
| **설명** | 로그인 사용자의 해당 룸메이트 채팅방 알림 모드를 변경한다. host는 hostNotificationMode, guest는 guestNotificationMode만 변경 가능. |

### Request

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `roomId` | `Long` | ✅ | 룸메이트 채팅방 ID |

#### Request Body

Content-Type: `application/json`

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `mode` | `String (enum)` | ✅ | 알림 모드. `EVERY` / `BUNDLED` / `OFF` 중 하나 |

```json
{
  "mode": "BUNDLED"
}
```

### Response

#### 성공 응답 — `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| `mode` | `String (enum)` | 변경된 알림 모드. `EVERY` / `BUNDLED` / `OFF` |

```json
{
  "mode": "BUNDLED"
}
```

#### 에러 응답

| 상태 코드 | ErrorCode | 발생 조건 |
|-----------|-----------|-----------|
| `400 Bad Request` | `VALIDATION_FAILED` | `mode` 필드가 null이거나 유효하지 않은 값 |
| `401 Unauthorized` | `JWT_ENTRY_POINT` | 인증 토큰 없음 또는 만료 |
| `403 Forbidden` | `ROOMMATE_CHAT_ROOM_FORBIDDEN` | 해당 채팅방의 host 또는 guest가 아닌 사용자 |
| `404 Not Found` | `ROOMMATE_CHAT_ROOM_NOT_FOUND` | `roomId`에 해당하는 채팅방 없음 |

```json
{
  "code": 10004,
  "name": "ROOMMATE_CHAT_ROOM_FORBIDDEN",
  "message": "[RoommateChat] 이 채팅방에 속하지 않은 사용자입니다.",
  "errors": null
}
```

---

## 알림 모드 조회

| 항목 | 내용 |
|------|------|
| **메서드** | `GET` |
| **경로** | `/roommate-chatting-room/{roomId}/notification-mode` |
| **인증** | Bearer Token |
| **설명** | 로그인 사용자의 해당 룸메이트 채팅방 현재 알림 모드를 조회한다. |

### Request

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `roomId` | `Long` | ✅ | 룸메이트 채팅방 ID |

### Response

#### 성공 응답 — `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| `mode` | `String (enum)` | 현재 알림 모드. `EVERY` / `BUNDLED` / `OFF` |

```json
{
  "mode": "EVERY"
}
```

#### 에러 응답

| 상태 코드 | ErrorCode | 발생 조건 |
|-----------|-----------|-----------|
| `401 Unauthorized` | `JWT_ENTRY_POINT` | 인증 토큰 없음 또는 만료 |
| `403 Forbidden` | `ROOMMATE_CHAT_ROOM_FORBIDDEN` | 해당 채팅방의 host 또는 guest가 아닌 사용자 |
| `404 Not Found` | `ROOMMATE_CHAT_ROOM_NOT_FOUND` | `roomId`에 해당하는 채팅방 없음 |

```json
{
  "code": 7013,
  "name": "ROOMMATE_CHAT_ROOM_NOT_FOUND",
  "message": "[RoommateChat] 채팅방을 찾을 수 없습니다.",
  "errors": null
}
```

---

## 공통 사항

### ChatNotificationMode 열거값

| 값 | 설명 |
|----|------|
| `EVERY` | 오프라인 수신자에게 메시지 수신 즉시 FCM 발송 (기본값) |
| `BUNDLED` | 즉시 발송 안 함. 매 정각 스케줄러가 미독 메시지 건수와 함께 일괄 발송 |
| `OFF` | FCM 발송 안 함 |

### 재사용 DTO

| DTO | 패키지 |
|-----|--------|
| `RequestUpdateNotificationModeDto` | `domain.openChat.dto.request` |
| `ResponseNotificationModeDto` | `domain.openChat.dto.response` |

### 에러 응답 공통 형식

```json
{
  "code": 정수,
  "name": "ErrorCode 이름",
  "message": "사람이 읽을 수 있는 메시지",
  "errors": null
}
```
