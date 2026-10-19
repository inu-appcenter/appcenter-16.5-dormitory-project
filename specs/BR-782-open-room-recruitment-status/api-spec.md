# BR-782 — 오픈채팅방 모집 상태 변경 API 명세서

> Base URL: `http://localhost:8080`

---

## 모집 상태 변경

| 항목 | 내용 |
|------|------|
| **메서드** | `PATCH` |
| **경로** | `/open-chat-rooms/{roomId}/recruitment-status` |
| **인증** | Bearer Token (JWT) 필요 |
| **설명** | 채팅방(`OPEN` 또는 `DERIVED` 타입)의 모집 상태를 `OPEN ↔ CLOSED`로 전환한다. `PERSONAL` 타입은 불가. |

### Request

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `roomId` | `Long` | ✅ | 모집 상태를 변경할 채팅방 ID |

#### Request Body

Content-Type: `application/json`

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `status` | `String (enum)` | ✅ | 전환할 모집 상태. `OPEN` 또는 `CLOSED` |

```json
{
  "status": "CLOSED"
}
```

### Response

#### 성공 응답 — `204 No Content`

본문 없음.

#### 에러 응답

공통 에러 응답 형식:

```json
{
  "code": 22036,
  "name": "OPEN_CHAT_ROOM_NOT_ALLOWED_TYPE",
  "message": "[OpenChat] 모집 상태를 변경할 수 없는 방 타입입니다.",
  "errors": null
}
```

| HTTP 상태 | ErrorCode (name) | code | 발생 조건 |
|-----------|-----------------|------|----------|
| `400 Bad Request` | `OPEN_CHAT_ROOM_NOT_ALLOWED_TYPE` | 220xx | `PERSONAL` 타입 방에 요청 |
| `400 Bad Request` | `VALIDATION_FAILED` | 5001 | `status` 필드 누락, `null`, 또는 허용되지 않는 enum 값 |
| `401 Unauthorized` | `JWT_ENTRY_POINT` | — | 인증 토큰 없음 또는 만료 |
| `403 Forbidden` | `OPEN_CHAT_ROOM_FORBIDDEN` | — | 방장(`createdBy`)도 `ROLE_ADMIN`도 아닌 사용자 요청 |
| `404 Not Found` | `OPEN_CHAT_ROOM_NOT_FOUND` | — | `roomId`에 해당하는 방 없음 |
| `404 Not Found` | `USER_NOT_FOUND` | — | 인증된 사용자 계정이 DB에 없음 |
| `409 Conflict` | `OPEN_CHAT_ROOM_ALREADY_CLOSED` | 22030 | 현재 `CLOSED` → `CLOSED` 전환 시도 |
| `409 Conflict` | `OPEN_CHAT_ROOM_ALREADY_OPEN` | 22032 | 현재 `OPEN` → `OPEN` 전환 시도 |

#### 에러 응답 예시

**400 — PERSONAL 타입 방 요청**
```json
{
  "code": 220xx,
  "name": "OPEN_CHAT_ROOM_NOT_ALLOWED_TYPE",
  "message": "[OpenChat] 모집 상태를 변경할 수 없는 방 타입입니다.",
  "errors": null
}
```

**400 — status 필드 누락**
```json
{
  "code": 5001,
  "name": "VALIDATION_FAILED",
  "message": "DTO에서 요청한 값이 올바르지 않습니다.",
  "errors": ["status: must not be null"]
}
```

**409 — 이미 CLOSED 상태**
```json
{
  "code": 22030,
  "name": "OPEN_CHAT_ROOM_ALREADY_CLOSED",
  "message": "[OpenChat] 이미 마감된 채팅방입니다.",
  "errors": null
}
```

---

## 부수 동작 (roomType에 따른 차이)

| roomType | 모집 상태 변경 | 시스템 메시지 발송 | transitionCount 증가 |
|----------|-------------|----------------|-------------------|
| `OPEN` | 허용 (공식방 포함) | 없음 | 없음 |
| `DERIVED` | 허용 (기존 동작 유지) | RECRUITMENT_STATUS_EVENT 발송, OPEN 전환 시 재모집 카드 발송 | OPEN 전환 시 +1 |
| `PERSONAL` | 불가 (400) | — | — |

---

## 추론 항목

> 아래 항목은 코드에서 직접 확인되지 않아 기존 패턴·명세로 추론했습니다.

- `OPEN_CHAT_ROOM_NOT_ALLOWED_TYPE` ErrorCode의 정확한 `code` 번호: 22034(`OPEN_CHAT_REPLY_NOT_ALLOWED_FOR_TYPE`), 22035(`OPEN_CHAT_REPLY_TARGET_NOT_IN_SAME_ROOM`)가 이미 사용 중이므로 **22036 이상**이 됩니다. 구현 시 확인 필요.
- `CLOSED` 상태에서 비참여자 입장 시도 시 `OPEN_CHAT_ROOM_CLOSED_FOR_JOIN` (409) — 이 동작은 `joinRoom()` 로직에 이미 구현되어 있으며 이번 BR에서 수정하지 않습니다.
