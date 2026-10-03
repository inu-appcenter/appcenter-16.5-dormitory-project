# BR-761 마감 상태의 참여 차단 — 신규 참여 API 모두 차단

## 기능 요약
파생 톡방(`DERIVED`)이 **CLOSED** 모집 상태일 때, 비참여자의 **joinRoom 요청을 모두 차단**한다. 마감과 참여 요청이 동시에 발생하면 서버에서 먼저 처리된 요청을 기준으로 일관성 있게 처리한다. 기존 참여자의 메시지 송수신과 조회는 계속 허용한다.

## 동작 명세

### 1) joinRoom 차단 — CLOSED 상태 검증
- **입력:** 비참여자 `userId`, 파생 톡방 `roomId`
- **처리:**
  1. 대상 방 조회. 없으면 `OPEN_CHAT_ROOM_NOT_FOUND` (404).
  2. `roomType != DERIVED` 이면 `OPEN_CHAT_ROOM_NOT_DERIVED` (400).
  3. 현재 요청자가 이미 참여자인지 확인 (중복 참여 방지, 기존 로직).
  4. **신규: 방의 모집 상태 확인**
     - `recruitmentClosed == true` (즉, `recruitmentStatus == CLOSED`) 이면 → `OPEN_CHAT_ROOM_CLOSED_FOR_JOIN` (409) 반환, 참여 불가.
     - `recruitmentClosed == false` (즉, `recruitmentStatus == OPEN`) 이면 → 참여 진행.
- **출력:** 
  - 성공: 201 CREATED 또는 200 OK (기존 응답 그대로)
  - 실패: 409 `OPEN_CHAT_ROOM_CLOSED_FOR_JOIN`

### 2) 동시성 처리 — "먼저 처리된 요청 기준"
- CLOSED 전환과 joinRoom 요청이 동시에 발생할 때:
  - **마감(`updateRecruitmentStatus(CLOSED)`)이 먼저 DB 커밋** → 이후 joinRoom은 409 `OPEN_CHAT_ROOM_CLOSED_FOR_JOIN` 반환.
  - **joinRoom이 먼저 DB 커밋** → 이후 마감 전환은 정상 진행 (이미 참여한 유저는 메시지 송수신 계속 가능).
  - 기술적 구현: DB 트랜잭션 격리 수준(Isolation Level)과 낙관적/비관적 락을 활용해 일관성 보장. (구체적 구현은 `/implement`에서 결정).

### 3) 기존 참여자는 예외
- CLOSED 상태 여부와 무관하게 **이미 참여한 유저**의 다음 작업은 정상 진행:
  - 메시지 전송 (`sendMessage`)
  - 메시지 조회 (`getMessages`)
  - 방 상세 조회 (`getRoomDetail`)
  - 참여자 목록 조회 등 기존 API
- 추가 검증 불필요 (기존 참여 여부는 `OpenChatParticipant`에서 확인).

## 도메인 데이터

### OpenChatRoom (기존, 변경 없음)
- `recruitmentClosed: boolean` (default: false) — BR-759에서 도입
- `recruitmentStatus: OpenChatRoomRecruitmentStatus` (enum: OPEN/CLOSED) — BR-759에서 도입, 응답 DTO에만 노출

### 변경 사항
- **없음**. BR-759에서 도입한 필드와 enum을 그대로 사용. 신규 DB 필드 또는 엔티티 메서드 추가 불필요.

### 새 ErrorCode
- **없음**. 기존 `OPEN_CHAT_ROOM_CLOSED_FOR_JOIN` (409, BR-743에서 도입)을 재사용.

## 비즈니스 규칙 / 제약

- 대상: `OpenChatRoomType.DERIVED` 타입 방만. `OPEN` / `PERSONAL` / 공식방(`isOfficial == true`)은 영향 없음.
- CLOSED 판정 기준: `recruitmentClosed == true` (또는 enum으로 `recruitmentStatus == CLOSED`).
- 차단 대상: **비참여자**의 `joinRoom` 요청만. 이미 참여한 유저의 작업은 제약 없음.
- 동시성 보장: 마감 전환 또는 참여 요청 중 먼저 DB에 커밋된 것이 최종 상태가 되며, 후속 요청은 그 상태를 반영해 처리.
- 기존 `/close-recruitment` 또는 신규 `/recruitment-status` 엔드포인트로 마감 전환 후 즉시 차단 시작 (특별한 지연 없음).

## 예외 · 경계 상황

- 존재하지 않는 `roomId` → `OPEN_CHAT_ROOM_NOT_FOUND` (404).
- `OPEN` / `PERSONAL` / 공식 방에 참여 시도 → `OPEN_CHAT_ROOM_NOT_DERIVED` (400) (기존 로직).
- CLOSED 상태 방에 비참여자 참여 시도 → `OPEN_CHAT_ROOM_CLOSED_FOR_JOIN` (409).
- 이미 참여한 유저의 재입장 시도 → 차단하지 않음 (기존 중복 참여 방지 로직 적용, CLOSED 상태와 무관).
- 마감과 참여 동시 요청:
  - DB 수준에서 먼저 커밋된 요청이 우선권 가짐.
  - race condition으로 인한 불일치 발생 시 → DB 상태를 권위로 삼아 처리 (타임스탬프 또는 버전 체크).

## 비목표 (Non-goals)

- CLOSED 상태 해제 후 자동 참여 허용 (이미 BR-759에서 구현 — `recruitmentStatus=OPEN`으로 전환 후 비참여자는 정상 참여 가능).
- 마감 시 기존 참여자에게 알림/메시지 발송.
- 마감 이유 필드 저장 또는 이력 기록 (상태 변경만 기록).
- joinRoom 이외의 다른 API 차단 (입장 전용).
- 방 생성 시 초기 상태를 CLOSED로 설정하는 기능 (현재는 기본값 OPEN).
- `OPEN` / `PERSONAL` / 공식 방의 모집 상태 관리 (DERIVED만).

## 수용 기준 (Acceptance Criteria)

### AC-01 비참여자의 joinRoom 차단 — CLOSED 상태
- Given 방장 A의 DERIVED 방, 현재 CLOSED (`recruitmentClosed=true`)
- When 비참여자 B가 `POST /open-chat-rooms/{roomId}/members` (또는 joinRoom) 호출
- Then 409 `OPEN_CHAT_ROOM_CLOSED_FOR_JOIN`. 참여 불가.

### AC-02 기존 참여자는 CLOSED 상태 무관하게 메시지 송수신
- Given CLOSED 상태 방에 이미 참여한 유저 C
- When C가 `POST /open-chat-rooms/{roomId}/messages` (메시지 전송) 호출
- Then 200 또는 201. 메시지 정상 저장 및 전송.

### AC-03 CLOSED → OPEN 전환 후 비참여자 입장 가능
- Given CLOSED 상태의 DERIVED 방
- When 방장 또는 관리자가 `PATCH /open-chat-rooms/{roomId}/recruitment-status` 바디 `{"status":"OPEN"}` 호출
- Then 204. 이후 비참여자 D의 joinRoom 시도 → 200/201 성공.

### AC-04 OPEN 상태에서는 비참여자 입장 차단 없음
- Given OPEN 상태의 DERIVED 방
- When 비참여자 E가 joinRoom 호출
- Then 200 또는 201. 참여 성공.

### AC-05 마감 전환과 joinRoom 동시 요청 — 마감 먼저
- Given OPEN 상태의 DERIVED 방, 비참여자 F의 joinRoom과 방장의 마감(`{"status":"CLOSED"}`) 요청이 거의 동시
- When 서버가 마감 요청을 먼저 DB 커밋
- Then 비참여자 F의 joinRoom은 409 `OPEN_CHAT_ROOM_CLOSED_FOR_JOIN` 반환.

### AC-06 마감 전환과 joinRoom 동시 요청 — 참여 먼저
- Given 동일 상황 (AC-05)
- When 서버가 joinRoom 요청을 먼저 DB 커밋
- Then 유저 F가 참여 성공. 이후 마감 전환도 정상 진행.

### AC-07 OPEN/PERSONAL/공식 방은 차단 없음
- Given CLOSED 상태의 `OPEN` 타입 또는 `PERSONAL` 또는 공식 방 (논리상 CLOSED 상태 자체가 없음)
- When 비참여자가 joinRoom 호출
- Then 차단하지 않음 (기존 로직). 정상 진행되거나 다른 이유(권한, 방 설정)로 실패할 수 있음.

### AC-08 기존 참여자의 재입장 시도는 중복 참여 방지만 적용
- Given 이미 참여한 유저 G, CLOSED 상태의 방에 G가 다시 joinRoom 호출
- When G의 요청 처리
- Then 중복 참여 방지 (기존 로직)로 400 또는 409. CLOSED 상태 검증과 무관.

### AC-09 마감 이후 기존 참여자의 메시지 조회 정상
- Given CLOSED 상태 방의 기존 참여자 H
- When H가 `GET /open-chat-rooms/{roomId}/messages` 호출
- Then 200. 메시지 목록 정상 반환 (마감 상태 무관).

### AC-10 joinRoom 실패 응답 포맷
- When 비참여자가 CLOSED 방에 입장 시도
- Then HTTP 409, 응답 바디:
  ```json
  {
    "code": "OPEN_CHAT_ROOM_CLOSED_FOR_JOIN",
    "message": "방이 모집 완료 상태입니다."
  }
  ```
  (기존 BR-743 포맷 재사용).
