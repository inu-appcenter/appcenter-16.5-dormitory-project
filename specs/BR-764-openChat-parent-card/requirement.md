# BR-764 부모 오픈채팅 카드 관리

## 기능 요약
파생 톡방(`DERIVED`)에 부모 방 참조(`originRoomId`, `transitionCount`) 필드를 추가하고, 모집 상태 변경(BR-759) 후처리로 부모 오픈채팅에 WebSocket(STOMP) 이벤트를 전송한다. 재개(CLOSED→OPEN) 시에는 부모 방 하단에 `REOPEN_CARD` 타입 메시지를 자동 생성하며, 동일 상태 전이(`transitionCount`)에 대한 중복 생성을 방지한다.

---

## 동작 명세

### 1) 파생 톡방 생성 시 부모 방 연결
- **입력:** `RequestCreateDerivedRoomDto.originRoomId` (기존 필드)
- **처리:** `OpenChatRoom.createDerived(...)` 시 `originRoomId` 저장, `transitionCount = 0`으로 초기화
- 기존 `sendRoomLinkMessage` 흐름 변경 없음

### 2) 모집 상태 변경 시 부모 방 이벤트 전송
BR-759 `PATCH /open-chat-rooms/{roomId}/recruitment-status` 처리 성공 이후 수행.

- 파생 방의 `originRoomId`가 null이면 이벤트 전송 생략
- 방향 무관(OPEN→CLOSED, CLOSED→OPEN 모두) 이벤트 전송
- 전송 토픽: `/sub/openchat/{originRoomId}/recruitment-status`
- Payload: `{ "derivedRoomId": roomId, "recruitmentStatus": "OPEN" | "CLOSED" }`

**CLOSED → OPEN 추가 처리 (재모집 카드 생성):**
1. `transitionCount += 1`
2. `duplKey = "{roomId}_reopen_{transitionCount}"` 로 중복 조회
3. 중복 없으면 `REOPEN_CARD` 메시지 생성 → `/sub/openchat/{originRoomId}` 브로드캐스트
4. 중복 있으면 메시지 생성 건너뜀 (이벤트는 정상 전송)

### 3) 카드 조회 시 모집 상태 반환
- `ROOM_LINK` 메시지 응답 DTO에 `recruitmentStatus: OpenChatRoomRecruitmentStatus` 필드 추가
- 기존 `recruitmentClosed` boolean 유지 (중복 필드 — 하위 호환)
- `REOPEN_CARD` 메시지도 동일한 DTO 구조로 응답 (type만 다름)

---

## 도메인 데이터

### OpenChatRoom (필드 2개 추가)
| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `originRoomId` | `Long` | nullable | 파생 방이 속한 부모 방 ID (DERIVED 타입만 해당) |
| `transitionCount` | `int` | not null, default 0 | 이 방의 CLOSED→OPEN 전이 횟수 (재모집 카드 중복 방지 기준) |

### OpenChatMessage (필드 1개 추가)
| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `duplKey` | `String(100)` | nullable, unique | 재모집 카드 중복 방지 키 (`{roomId}_reopen_{transitionCount}`) |

### OpenChatMessageType (신규 값 추가)
```
REOPEN_CARD  // CLOSED→OPEN 재개 시 부모 방에 생성되는 재모집 카드
```

### REOPEN_CARD content JSON 구조
`ROOM_LINK`와 동일한 JSON을 저장한다.
```json
{
  "derivedRoomId": 12,
  "roomName": "방 이름",
  "description": "방 설명",
  "maxParticipants": 30
}
```

### 신규 DTO
- `ResponseRecruitmentStatusEventDto`
  - `derivedRoomId: Long`
  - `recruitmentStatus: OpenChatRoomRecruitmentStatus`

---

## 비즈니스 규칙 / 제약
- `originRoomId`가 null인 파생 방은 부모 방 이벤트 전송 대상이 아니다. (기존 파생 방 하위 호환)
- 부모 방이 DB에 존재하지 않으면 이벤트·카드 생성 없이 soft-fail. 예외 throw 금지.
- 재모집 카드 중복 방지: `duplKey`가 이미 존재하면 메시지 생성 건너뜀. 이벤트 전송은 항상 수행.
- `transitionCount` 증가는 CLOSED→OPEN 전이 성공 직후 한 번만 수행한다.
- OPEN→CLOSED(마감) 시에는 이벤트만 전송하고 카드는 생성하지 않는다.
- 재모집 카드 생성 시 발신자(`senderId`)는 상태를 변경한 `actorId`(방장 또는 관리자)로 설정한다.

---

## 예외 · 경계 상황
| 상황 | 기대 동작 |
|---|---|
| 파생 방 `originRoomId = null` | 이벤트·카드 생략, 정상 204 |
| 부모 방 DB에 없음 | 이벤트·카드 생략, 정상 204 |
| 동일 `transitionCount` 재모집 카드 이미 존재 | 카드 생성 건너뜀, 이벤트는 전송 |
| OPEN인 방에서 재개(CLOSED→OPEN) 없이 OPEN 재요청 | BR-759에서 409 거절 (이 BR 해당 없음) |
| 이벤트 전송 실패 | WebSocket 채널 미구독자는 자연히 수신 못함 (별도 재시도 없음) |

---

## 비목표 (Non-goals)
- 기존 파생 방 데이터에 `originRoomId` 역산 마이그레이션
- OPEN→CLOSED 마감 시 카드 메시지 생성
- FCM 푸시 알림 (이벤트는 WebSocket(STOMP)만)
- `/sub/openchat/{originRoomId}/recruitment-status` 토픽 구독 인증·권한 제어
- 이벤트 전송 실패 시 재시도·큐잉 로직
- ROOM_LINK 메시지 content JSON에 `recruitmentStatus` enum 저장 (조회 시 DB에서 동적 반환)
- 재모집 카드 삭제·숨김 기능

---

## 수용 기준 (Acceptance Criteria)

### AC-01 파생 방 생성 시 originRoomId·transitionCount 저장
- Given 부모 방 P (OPEN 타입), 요청자 U
- When U가 파생 방 생성 요청 (`originRoomId=P.id`)
- Then DB: 파생 방의 `originRoomId=P.id`, `transitionCount=0`

### AC-02 OPEN→CLOSED 마감 시 부모 방 이벤트 전송
- Given `originRoomId=P`인 파생 방 D (현재 OPEN)
- When 방장이 `{"status":"CLOSED"}` 호출 → 204
- Then `/sub/openchat/{P}/recruitment-status` 토픽에 `{"derivedRoomId":D.id, "recruitmentStatus":"CLOSED"}` 전송
- And 재모집 카드 메시지 생성 없음

### AC-03 CLOSED→OPEN 재개 시 부모 방 이벤트 전송
- Given `originRoomId=P`인 파생 방 D (현재 CLOSED)
- When 방장이 `{"status":"OPEN"}` 호출 → 204
- Then `/sub/openchat/{P}/recruitment-status` 토픽에 `{"derivedRoomId":D.id, "recruitmentStatus":"OPEN"}` 전송

### AC-04 재개 시 재모집 카드 메시지 생성
- Given `originRoomId=P`인 파생 방 D (`transitionCount=0`, CLOSED)
- When 방장이 `{"status":"OPEN"}` 호출
- Then DB: `open_chat_message`에 `roomId=P`, `type=REOPEN_CARD`, `duplKey="{D.id}_reopen_1"` 메시지 1개 생성
- And D.`transitionCount=1`
- And `/sub/openchat/{P}` 토픽에 재모집 카드 DTO 브로드캐스트

### AC-05 동일 transitionCount 재모집 카드 중복 방지
- Given `originRoomId=P`인 파생 방 D (`transitionCount=2`, CLOSED), `duplKey="{D.id}_reopen_2"` 카드 이미 존재
- When 방장이 `{"status":"OPEN"}` 재요청 (네트워크 재시도)
- Then 카드 추가 생성 없음 (기존 카드 1개 유지)
- And 이벤트 전송은 정상 수행

### AC-06 originRoomId=null 방은 이벤트·카드 없이 204 반환
- Given `originRoomId=null`인 파생 방 (기존에 생성된 방)
- When 방장이 모집 상태 변경
- Then 204. 이벤트 전송 없음. 카드 생성 없음. transitionCount 변화 없음.

### AC-07 부모 방 존재하지 않아도 204 반환
- Given `originRoomId=999`(존재하지 않는 방)인 파생 방 D
- When 방장이 모집 상태 변경
- Then 204. 예외 없음.

### AC-08 카드 조회 응답에 recruitmentStatus enum 포함
- Given 부모 방 P에 ROOM_LINK 메시지 (derivedRoomId=D.id), D는 CLOSED 상태
- When 참여자가 부모 방 메시지 조회
- Then ROOM_LINK DTO에 `recruitmentStatus: CLOSED`, `recruitmentClosed: true` 포함

### AC-09 재모집 카드 조회 응답 형식
- Given 부모 방 P에 REOPEN_CARD 메시지 (derivedRoomId=D.id), D는 OPEN 상태
- When 참여자가 부모 방 메시지 조회
- Then REOPEN_CARD DTO에 `derivedRoomId`, `roomName`, `description`, `maxParticipants`, `recruitmentStatus: OPEN` 포함
