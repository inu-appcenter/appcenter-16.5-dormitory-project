# BR-782 — 일반 오픈채팅방 모집 상태(OPEN/CLOSED) 변경 기능 추가

## 기능 요약

`OPEN` 타입 오픈채팅방(공식방 포함)에서도 방장 또는 ADMIN이 모집 상태를
`OPEN ↔ CLOSED`로 전환할 수 있게 한다.
기존 `DERIVED` 전용이었던 `PATCH /{roomId}/recruitment-status` 엔드포인트를 확장한다.

---

## 동작 명세

### 정상 흐름

1. 방장 또는 ADMIN이 `PATCH /api/open-chat/rooms/{roomId}/recruitment-status` 요청
2. 서버가 roomId로 방 조회
3. 요청자 권한 확인 (방장 or ADMIN)
4. 방 타입 확인 — `OPEN` 또는 `DERIVED`이면 진행, 그 외 400
5. 현재 상태와 요청 상태 동일 시 409
6. `recruitmentClosed` 및 `lastStatusChangedBy` / `lastStatusChangedAt` 갱신
7. `DERIVED`이면 기존 시스템 메시지 발송 유지; `OPEN`이면 메시지 발송 없음
8. 204 반환

### 분기

| 조건 | 처리 |
|------|------|
| roomType == PERSONAL | 400 OPEN_CHAT_ROOM_NOT_ALLOWED_TYPE |
| roomType == OPEN (공식/비공식 모두) | 허용 |
| roomType == DERIVED | 기존 로직 유지 (시스템 메시지 포함) |
| CLOSED 상태에서 비참여자 입장 시도 | 400 OPEN_CHAT_ROOM_RECRUITMENT_CLOSED |

---

## 도메인 데이터

**OpenChatRoom** (기존 필드 활용, 신규 필드 없음)

| 필드 | 타입 | 변화 |
|------|------|------|
| `recruitmentClosed` | boolean | false(OPEN) ↔ true(CLOSED) 로 전환 |
| `lastStatusChangedBy` | Long | 변경 요청자 userId |
| `lastStatusChangedAt` | LocalDateTime | 변경 시각 |

---

## 비즈니스 규칙 / 제약

1. **허용 타입**: `OPEN`(공식·비공식 모두), `DERIVED`. `PERSONAL`은 불가.
2. **권한**: 방장(`createdBy == actorId`) 또는 `ROLE_ADMIN`.
3. **멱등성 차단**: 현재 상태와 동일한 상태로의 전환은 409.
4. **CLOSED 입장 차단**: `OPEN` 타입 방이 CLOSED일 때 비참여자 입장 → 400.
   - 기존 DERIVED의 입장 차단 로직과 동일한 ErrorCode(`OPEN_CHAT_ROOM_RECRUITMENT_CLOSED`) 재사용.
5. **시스템 메시지 없음**: `OPEN` 타입에서는 모집 상태 변경 이벤트 메시지·재모집 카드 발송 안 함.
   - `originRoomId == null`이면 발송하지 않는 기존 분기로 자연히 처리됨.
6. **transitionCount 증가 없음**: `OPEN` 타입에는 파생 톡방 재모집 횟수 개념이 없으므로 증가 안 함.

---

## 예외 · 경계 상황

| 상황 | 기대 동작 |
|------|----------|
| 존재하지 않는 roomId | 404 OPEN_CHAT_ROOM_NOT_FOUND |
| 존재하지 않는 actorId | 404 USER_NOT_FOUND |
| 권한 없는 일반 참여자 요청 | 403 OPEN_CHAT_ROOM_FORBIDDEN |
| PERSONAL 타입 방 요청 | 400 OPEN_CHAT_ROOM_NOT_ALLOWED_TYPE |
| 현재 CLOSED → CLOSED 전환 | 409 OPEN_CHAT_ROOM_ALREADY_CLOSED |
| 현재 OPEN → OPEN 전환 | 409 OPEN_CHAT_ROOM_ALREADY_OPEN |
| CLOSED 상태 방에 비참여자 입장 | 400 OPEN_CHAT_ROOM_RECRUITMENT_CLOSED |

> **ErrorCode 신규 추가**
> - `OPEN_CHAT_ROOM_NOT_ALLOWED_TYPE`: PERSONAL 타입에 모집 상태 변경 요청
>   (기존 `OPEN_CHAT_ROOM_NOT_DERIVED` 대체 또는 병행)

---

## 비목표 (Non-goals)

- OPEN 타입 방에서의 모집 이벤트 메시지 발송 (카드 메시지 등)
- OPEN 타입 전용 신규 엔드포인트 추가
- `transitionCount` 관리 (DERIVED 전용)
- 공식방 모집 상태의 별도 권한 체계 (방장 + ADMIN 공통 규칙 그대로)
- PERSONAL 타입 모집 상태 지원
- 모집 상태에 따른 푸시 알림

---

## 수용 기준 (Acceptance Criteria)

### 정상 케이스

- **AC-1** Given OPEN 타입 비공식 방의 방장이 / When CLOSED 요청 시 / Then 204, recruitmentClosed=true
- **AC-2** Given OPEN 타입 공식방(isOfficial=true)의 방장이 / When CLOSED 요청 시 / Then 204, recruitmentClosed=true
- **AC-3** Given ADMIN이 OPEN 타입 방에 / When OPEN 요청(현재 CLOSED) 시 / Then 204, recruitmentClosed=false
- **AC-4** Given OPEN 타입 방이 CLOSED 상태일 때 / When 비참여자가 입장 시도 / Then 400 OPEN_CHAT_ROOM_RECRUITMENT_CLOSED

### 예외 케이스

- **AC-5** Given PERSONAL 타입 방에 / When 방장이 CLOSED 요청 시 / Then 400 OPEN_CHAT_ROOM_NOT_ALLOWED_TYPE
- **AC-6** Given OPEN 타입 방이 이미 CLOSED / When 방장이 CLOSED 재요청 / Then 409
- **AC-7** Given 일반 참여자가 OPEN 타입 방에 / When CLOSED 요청 시 / Then 403
- **AC-8** Given DERIVED 타입 방에 / When 방장이 CLOSED 요청 시 / Then 기존과 동일하게 204 + 시스템 메시지 발송 (회귀 없음)
