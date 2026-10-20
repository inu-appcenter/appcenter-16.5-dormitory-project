# BR-761 마감 상태의 참여 차단 — API 명세서

> Base URL: `http://localhost:8080/api`

---

## 1. 파생 톡방 입장 (joinRoom)

| 항목 | 내용 |
|------|------|
| **메서드** | `POST` |
| **경로** | `/open-chat-rooms/{roomId}/participants/me` |
| **인증** | 필요 (Bearer Token) |
| **설명** | 비참여자가 파생 톡방에 입장. CLOSED 상태일 때 요청 차단 |

### Request

#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `roomId` | `Long` | ✅ | 대상 파생 톡방 ID |

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `password` | `String` | ❌ | 비밀번호 보호 방의 비밀번호 |

#### Request Body
없음

### Response

#### 성공 응답 — `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| `roomId` | `Long` | 방 ID |
| `name` | `String` | 방 이름 |
| `description` | `String` | 방 설명 |
| `scope` | `String` (enum) | 방 범위 (`DORMITORY_WIDE`, `FLOOR`, `FLOOR_AND_SAME_DORM`) |
| `currentParticipants` | `Integer` | 현재 참여자 수 |
| `maxParticipants` | `Integer` | 최대 참여자 수 |
| `isOfficial` | `Boolean` | 공식 방 여부 |
| `createdAt` | `String` (ISO 8601) | 방 생성 시각 |
| `isBlockedByPartner` | `Boolean` | 상대방 차단 여부 |
| `recruitmentClosed` | `Boolean` | 모집 마감 여부 |
| `recruitmentStatus` | `String` (enum) | 모집 상태 (`OPEN`, `CLOSED`) |
| `lastStatusChangedAt` | `String` (ISO 8601) | 마지막 상태 변경 시각 |
| `lastStatusChangedBy` | `Long` | 마지막 상태 변경 유저 ID |

```json
{
  "roomId": 1,
  "name": "기숙사 1층 친목 방",
  "description": "1층 주민들의 친목 모임",
  "scope": "FLOOR",
  "currentParticipants": 5,
  "maxParticipants": 20,
  "isOfficial": false,
  "createdAt": "2026-05-17T10:30:00",
  "isBlockedByPartner": false,
  "recruitmentClosed": false,
  "recruitmentStatus": "OPEN",
  "lastStatusChangedAt": null,
  "lastStatusChangedBy": null
}
```

#### 에러 응답

| 상태 코드 | 발생 조건 | ErrorCode | 응답 예시 |
|-----------|-----------|-----------|-----------|
| `404 Not Found` | 방이 존재하지 않음 | `OPEN_CHAT_ROOM_NOT_FOUND` | `{"name": "OPEN_CHAT_ROOM_NOT_FOUND", "message": "방을 찾을 수 없습니다."}` |
| `400 Bad Request` | `OPEN`/`PERSONAL`/공식 방 입장 시도 | `OPEN_CHAT_ROOM_NOT_DERIVED` | `{"name": "OPEN_CHAT_ROOM_NOT_DERIVED", "message": "파생 톡방이 아닙니다."}` |
| `409 Conflict` | **CLOSED 상태 방에 비참여자 입장 시도 (BR-761)** | `OPEN_CHAT_ROOM_CLOSED_FOR_JOIN` | `{"name": "OPEN_CHAT_ROOM_CLOSED_FOR_JOIN", "message": "방이 모집 완료 상태입니다."}` |
| `409 Conflict` | 정원 초과 | `OPEN_CHAT_ROOM_FULL` | `{"name": "OPEN_CHAT_ROOM_FULL", "message": "방이 가득 찼습니다."}` |
| `403 Forbidden` | 비밀번호 불일치 | `OPEN_CHAT_ROOM_FORBIDDEN` | `{"name": "OPEN_CHAT_ROOM_FORBIDDEN", "message": "접근할 수 없습니다."}` |
| `403 Forbidden` | 기숙사 범위 미일치 | `OPEN_CHAT_ROOM_FORBIDDEN` | `{"name": "OPEN_CHAT_ROOM_FORBIDDEN", "message": "접근할 수 없습니다."}` |
| `404 Not Found` | 사용자 없음 | `USER_NOT_FOUND` | `{"name": "USER_NOT_FOUND", "message": "사용자를 찾을 수 없습니다."}` |

---

## 2. 파생 톡방 모집 상태 변경

| 항목 | 내용 |
|------|------|
| **메서드** | `PATCH` |
| **경로** | `/open-chat-rooms/{roomId}/recruitment-status` |
| **인증** | 필요 (Bearer Token, 방장만) |
| **설명** | 파생 톡방의 모집 상태를 OPEN ↔ CLOSED로 변경. 상태 변경 이후 비참여자의 입장이 차단됨 |

### Request

#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `roomId` | `Long` | ✅ | 대상 파생 톡방 ID |

#### Request Body
Content-Type: `application/json`

| 필드 | 타입 | 필수 | 허용값 | 설명 |
|------|------|------|--------|------|
| `status` | `String` (enum) | ✅ | `OPEN`, `CLOSED` | 변경할 모집 상태 |

```json
{
  "status": "CLOSED"
}
```

### Response

#### 성공 응답 — `204 No Content`

응답 바디 없음

#### 에러 응답

| 상태 코드 | 발생 조건 | ErrorCode | 응답 예시 |
|-----------|-----------|-----------|-----------|
| `404 Not Found` | 방이 존재하지 않음 | `OPEN_CHAT_ROOM_NOT_FOUND` | `{"name": "OPEN_CHAT_ROOM_NOT_FOUND", "message": "방을 찾을 수 없습니다."}` |
| `400 Bad Request` | 필수 필드 누락 또는 유효하지 않은 상태값 | — | `{"name": "MethodArgumentNotValidException", "message": "status must not be null"}` |
| `403 Forbidden` | 방장이 아님 | `OPEN_CHAT_ROOM_FORBIDDEN` | `{"name": "OPEN_CHAT_ROOM_FORBIDDEN", "message": "접근할 수 없습니다."}` |
| `404 Not Found` | 사용자 없음 | `USER_NOT_FOUND` | `{"name": "USER_NOT_FOUND", "message": "사용자를 찾을 수 없습니다."}` |

---

## 3. 파생 톡방 마감 (레거시)

| 항목 | 내용 |
|------|------|
| **메서드** | `PATCH` |
| **경로** | `/open-chat-rooms/{roomId}/close-recruitment` |
| **인증** | 필요 (Bearer Token, 방장만) |
| **설명** | 파생 톡방을 즉시 CLOSED 상태로 변경. `/recruitment-status` 엔드포인트를 권장 |

### Request

#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `roomId` | `Long` | ✅ | 대상 파생 톡방 ID |

#### Request Body
없음

### Response

#### 성공 응답 — `204 No Content`

응답 바디 없음

#### 에러 응답

| 상태 코드 | 발생 조건 | ErrorCode | 응답 예시 |
|-----------|-----------|-----------|-----------|
| `404 Not Found` | 방이 존재하지 않음 | `OPEN_CHAT_ROOM_NOT_FOUND` | `{"name": "OPEN_CHAT_ROOM_NOT_FOUND", "message": "방을 찾을 수 없습니다."}` |
| `403 Forbidden` | 방장이 아님 | `OPEN_CHAT_ROOM_FORBIDDEN` | `{"name": "OPEN_CHAT_ROOM_FORBIDDEN", "message": "접근할 수 없습니다."}` |

---

## 동시성 처리 (BR-761 명세)

### 시나리오: 마감 전환과 joinRoom 동시 요청

BR-761은 마감(`updateRecruitmentStatus(CLOSED)`)과 비참여자의 `joinRoom` 요청이 동시에 발생할 때 **DB 수준에서 먼저 커밋된 요청이 우선권을 가지도록** 보장한다.

#### Case 1: 마감이 먼저 DB 커밋
```
시간 T0: [마감 요청] START → 트랜잭션 시작, recruitmentClosed = true → DB 커밋
시간 T1: [joinRoom 요청] START → SELECT FOR UPDATE로 대기 → 마감 커밋 후 락 획득
시간 T2: [joinRoom] room.isRecruitmentClosed() 검증 → true → 409 OPEN_CHAT_ROOM_CLOSED_FOR_JOIN
```

**결과:** 비참여자의 joinRoom 차단 ✓

#### Case 2: joinRoom이 먼저 DB 커밋
```
시간 T0: [joinRoom 요청] START → SELECT FOR UPDATE 락 획득
시간 T1: room.isRecruitmentClosed() 검증 → false (아직 OPEN) → OpenChatParticipant 생성 → DB 커밋
시간 T2: [마감 요청] 수신 → 정상 진행 (이미 참여한 유저는 메시지 송수신 계속 가능)
```

**결과:** 비참여자 입장 성공 후 마감 진행 ✓

### 구현 기술

- **비관적 락:** `OpenChatRoomRepository.findByIdWithLock(roomId)` 
  - Spring Data JPA: `@Lock(LockModeType.PESSIMISTIC_WRITE)` 또는 쿼리 메서드 `SELECT ... FOR UPDATE`
- **격리 수준:** MySQL/Oracle 기본값 `REPEATABLE_READ` (또는 `SERIALIZABLE`)
- **보증:** 락 대기를 통해 요청 순서화, race condition 제거

---

## 추론 항목

> 아래 항목은 코드 패턴과 설계 문서로 추론했으며, 실제 동작과 다를 수 있습니다.

- **Enum 값:** `OpenChatRoomRecruitmentStatus` 값은 `OPEN`, `CLOSED`로 추론 (BR-759 설계 문서 기준)
- **Scope 값:** `scope` 응답 필드 값은 `DORMITORY_WIDE`, `FLOOR`, `FLOOR_AND_SAME_DORM` 등으로 추론
- **기존 참여자 예외:** 이미 참여한 유저의 메시지 송수신(`sendMessage`, `getMessages`)은 `recruitmentClosed` 상태 무관하게 계속 허용되는 것으로 추론 (AC-02 수용 기준 기준)
- **레거시 `/close-recruitment`:** 새 `/recruitment-status` 엔드포인트와 병행 지원, 권장은 `/recruitment-status`
