# BR-743 파생 톡방 마감 (모집 마감)

## 기능 요약
파생 톡방(`OpenChatRoomType.DERIVED`)에 **모집 마감** 상태를 도입한다. 방장 또는 관리자가 명시적으로 마감하면 이후 새 유저의 입장이 차단되며, 채팅과 기존 참여자의 활동은 계속 가능하다. 상태 전이는 단방향(재개 불가). 부모 톡방의 파생 톡방 안내 메시지(`ROOM_LINK`)와 방 목록/상세 조회 응답에 마감 여부(boolean)를 노출한다.

## 동작 명세

### 1) 마감 요청 (방장 또는 관리자)
- **입력:** 로그인 유저(`actorId`), 파생 톡방 `roomId`
- **처리:** 단일 엔드포인트에서 role 기반으로 분기 (kickParticipant 패턴 준용).
  1. 대상 방 조회 (`OpenChatRoom`). 없으면 `OPEN_CHAT_ROOM_NOT_FOUND`.
  2. 요청자 조회. 없으면 `USER_NOT_FOUND`.
  3. `Role.ROLE_ADMIN` 이 아니면서 `createdBy != actorId` 이면 `OPEN_CHAT_ROOM_FORBIDDEN`.
     - 즉 방장이거나 관리자면 통과.
  4. `roomType != DERIVED` 이면 `OPEN_CHAT_ROOM_NOT_DERIVED` (도메인이 검증).
  5. 이미 `recruitmentClosed == true` 이면 `OPEN_CHAT_ROOM_ALREADY_CLOSED` (도메인이 검증).
  6. `recruitmentClosed = true`, `closedAt = now`, `closedBy = actorId` 로 상태 전이.
- **출력:** 204 No Content

### 2) 파생 톡방 입장(joinRoom) 마감 가드
- 기존 `OpenChatRoomService.joinRoom(userId, roomId, password)` 흐름 안에서:
  - **비참여자**가 마감된 방(`recruitmentClosed == true`)에 입장 시도 → `OPEN_CHAT_ROOM_CLOSED_FOR_JOIN` (409).
  - **이미 참여자**인 경우는 마감 여부와 무관하게 정상 진입(방 상세 반환). 마감은 채팅/활동을 막지 않는다.
  - 마감 검증 순서는 비밀번호·기타 조인 검증보다 **앞**에 둔다(불필요한 정보 노출 방지 관점보다는, 마감을 최우선 실패 이유로 노출).

### 3) 부모 톡방의 ROOM_LINK 메시지에 마감 상태 노출
- 부모 톡방 메시지 목록 조회 시, 각 `ROOM_LINK` 메시지의 응답 DTO에 `linkedRoomRecruitmentClosed` boolean 필드가 포함된다.
- 값은 조회 시점의 파생 톡방 상태를 반영한다(스냅샷이 아니라 실시간).
- 파생 톡방이 이후 삭제/부재 상황이 되어도 안전하게 응답해야 한다(값은 `false` 또는 `null` — /design에서 확정).

### 4) 방 목록/상세 조회에 마감 상태 노출
- 방 목록(내 채팅방 목록, 검색 목록)과 방 상세 조회 응답 DTO에 `recruitmentClosed` boolean 필드가 포함된다.
- 마감된 방도 검색/목록에 계속 노출된다(숨김 X). 클라이언트가 필드로 UI 표기.

## 도메인 데이터

### OpenChatRoom (기존 엔티티에 필드 추가)
| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `recruitmentClosed` | boolean | not null, default false | 모집 마감 여부 |
| `closedAt` | `LocalDateTime` | nullable | 마감된 시각 |
| `closedBy` | `Long` | nullable | 마감을 트리거한 유저(방장) 또는 관리자 `userId` |

- 상태 전이: `recruitmentClosed = false → true` (단방향). true → false 전이는 API/도메인에서 제공하지 않는다.
- 도메인 메서드: `OpenChatRoom.closeRecruitment(Long actorId)` — 규칙 위반 시 도메인에서 `CustomException` throw (아래 규칙 참조).

### 새 ErrorCode
| Code | HTTP | 발생 상황 |
|---|---|---|
| `OPEN_CHAT_ROOM_ALREADY_CLOSED` | 409 | 이미 마감된 방을 다시 마감 시도 |
| `OPEN_CHAT_ROOM_NOT_DERIVED` | 400 | DERIVED 타입이 아닌 방에 마감 시도 |
| `OPEN_CHAT_ROOM_CLOSED_FOR_JOIN` | 409 | 마감된 방에 비참여자가 입장 시도 |

기존 재사용: `OPEN_CHAT_ROOM_NOT_FOUND`(404), `OPEN_CHAT_ROOM_FORBIDDEN`(403).

## 비즈니스 규칙 / 제약
- 마감 대상은 `OpenChatRoomType.DERIVED` 에 한정한다. OPEN / PERSONAL / 공식방(`isOfficial == true`)은 이 기능의 대상이 아니다.
- 방장(`createdBy`) 이거나 관리자(`Role.ROLE_ADMIN`)여야 마감할 수 있다. 일반 참여자는 불가.
- 마감 API는 **단일 엔드포인트**로 제공하고, 관리자·방장 분기는 서버 role 검사에서 처리한다 (기존 `kickParticipant` 패턴 준용).
- 마감은 **비가역**이다. 재개 API 없음.
- 마감 상태에서도 다음은 모두 정상 동작한다:
  - 기존 참여자의 메시지 발송/조회
  - 기존 참여자의 방 상세 조회
  - 시스템 메시지(입장/퇴장 등 기존 로직) 발송
  - 관리자 봇 메시지
- 마감 시 별도 시스템 메시지·FCM 알림은 발송하지 않는다. 클라이언트는 응답 필드를 통해서만 상태를 인지한다.

## 예외 · 경계 상황
- `OPEN` 또는 `PERSONAL` 방에 마감 API 호출 → `OPEN_CHAT_ROOM_NOT_DERIVED` (400).
- 방장이 아닌 참여자가 방장용 마감 API 호출 → `OPEN_CHAT_ROOM_FORBIDDEN` (403).
- 관리자가 아닌 유저가 관리자용 마감 API 호출 → 기존 관리자 인가 실패 처리(401/403).
- 존재하지 않는 `roomId` 로 마감 API 호출 → `OPEN_CHAT_ROOM_NOT_FOUND` (404).
- 이미 마감된 방을 다시 마감 시도 → `OPEN_CHAT_ROOM_ALREADY_CLOSED` (409).
- 마감된 방에 비참여자가 `joinRoom` 시도 → `OPEN_CHAT_ROOM_CLOSED_FOR_JOIN` (409). 비밀번호 검증 등 다른 실패 이유와 무관하게 우선 반환.
- 마감된 방에서 나갔던 유저가 다시 입장 시도 → 비참여자이므로 `OPEN_CHAT_ROOM_CLOSED_FOR_JOIN` (409).
- 마감된 방의 기존 참여자가 방 상세/메시지를 조회 → 정상 응답 (마감 필드는 `true`로 노출).
- 부모 톡방 메시지 목록 응답 시, `ROOM_LINK`가 여러 개면 파생 톡방들의 상태를 **한 번의 batch 조회**(`findAllById`)로 채운다. 개별 N+1 금지.

## 비목표 (Non-goals)
- 자동/시간 기반 마감(스케줄러, deadline 필드 등)
- 정원 도달 자동 마감
- 마감 재개(reopen) API
- 마감 사유(reason) 저장, 마감 통계
- 마감 시 시스템 메시지 삽입, FCM 푸시 알림
- OPEN / PERSONAL / 공식방 마감
- 초대(invitation) 도메인 연동 — 현재 main 코드에 초대 엔티티/서비스가 없음. 초대 도메인이 merge된 뒤 별도 BR로 연동.
- 마감된 방을 검색/목록에서 숨기는 필터링

## 수용 기준 (Acceptance Criteria)

### AC-01 방장이 자신의 파생 톡방을 정상 마감
- Given 방장 A가 만든 DERIVED 방(마감 전)
- When A가 `PATCH /open-chat-rooms/{roomId}/close-recruitment` 호출
- Then 204 응답. DB의 방은 `recruitmentClosed=true`, `closedAt != null`, `closedBy=A`.

### AC-02 방장·관리자가 아닌 참여자는 마감 불가
- Given DERIVED 방 참여자 B(방장 아님, 일반 유저)
- When B가 마감 API 호출
- Then 403 `OPEN_CHAT_ROOM_FORBIDDEN`. DB 상태 변화 없음.

### AC-03 이미 마감된 방을 다시 마감 시도
- Given 이미 마감된 DERIVED 방
- When 방장이 다시 마감 API 호출
- Then 409 `OPEN_CHAT_ROOM_ALREADY_CLOSED`. `closedAt`/`closedBy` 변경 없음.

### AC-04 OPEN/PERSONAL 방에는 마감 불가
- Given 방장 A의 `OPEN` 방 (또는 PERSONAL)
- When A가 마감 API 호출
- Then 400 `OPEN_CHAT_ROOM_NOT_DERIVED`.

### AC-05 존재하지 않는 방
- When `roomId=999999` 로 마감 API 호출
- Then 404 `OPEN_CHAT_ROOM_NOT_FOUND`.

### AC-06 관리자가 임의의 DERIVED 방 마감 (동일 엔드포인트)
- Given DERIVED 방(방장 A), 마감 전. 요청자 = 관리자 M(방 참여자 아님)
- When M이 마감 API 호출 (`PATCH /open-chat-rooms/{roomId}/close-recruitment`)
- Then 204. DB `recruitmentClosed=true`, `closedBy=M.id`. 방장 여부 무관.

### AC-07 관리자도 DERIVED 아닌 방은 마감 불가
- Given `OPEN` 방
- When 관리자가 마감 API 호출
- Then 400 `OPEN_CHAT_ROOM_NOT_DERIVED`.

### AC-08 비참여자의 마감된 방 입장 차단
- Given 마감된 DERIVED 방, 유저 C는 참여자 아님
- When C가 `joinRoom(roomId, password)` 시도 (비밀번호 유무 무관)
- Then 409 `OPEN_CHAT_ROOM_CLOSED_FOR_JOIN`. C는 방에 참여자로 추가되지 않음.

### AC-09 마감된 방에서 나갔다가 재입장 시도
- Given 유저 D가 예전에 참여자였다가 나간 뒤 방이 마감됨
- When D가 다시 `joinRoom` 시도
- Then 409 `OPEN_CHAT_ROOM_CLOSED_FOR_JOIN`.

### AC-10 마감된 방의 기존 참여자는 활동 지속
- Given 마감된 DERIVED 방의 기존 참여자 E
- When E가 메시지 전송/조회, 방 상세 조회
- Then 모두 정상 응답. 방 상세 응답에 `recruitmentClosed=true`.

### AC-11 부모 톡방 ROOM_LINK 응답의 마감 필드
- Given 부모 톡방 P에 파생 톡방 D1(마감 안 됨), D2(마감됨)에 대한 `ROOM_LINK` 메시지 2개
- When P의 메시지 목록을 조회
- Then D1의 `ROOM_LINK` 응답은 `linkedRoomRecruitmentClosed=false`, D2는 `true`.

### AC-12 부모 톡방 메시지 조회 시 batch 로딩
- Given 부모 톡방에 `ROOM_LINK` 메시지 N개
- When 메시지 목록 조회
- Then 파생 톡방 상태 조회를 위한 SQL은 방별 개별 조회가 아니라 단일 `IN` 쿼리 1건이어야 함(N+1 방지).

### AC-13 방 상세 조회에 recruitmentClosed 노출
- Given 마감된 DERIVED 방
- When 참여자가 방 상세 조회
- Then 응답 DTO에 `recruitmentClosed=true` 포함.

### AC-14 검색/방 목록 조회에 recruitmentClosed 노출
- Given DERIVED 방 3개 (마감 2 / 미마감 1)
- When 검색 또는 방 목록 조회
- Then 3개 모두 응답에 포함되며 각각의 `recruitmentClosed` 값이 정확히 반영됨.

### AC-15 마감 상태 단방향
- 도메인 관점: `OpenChatRoom` 은 마감 상태를 되돌리는 public 메서드를 노출하지 않는다(reopen API/서비스 메서드 부재).
