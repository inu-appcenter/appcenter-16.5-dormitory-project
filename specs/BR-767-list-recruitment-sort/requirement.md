# BR-767 채팅방 목록 모집 상태 포함 및 정렬

## 기능 요약

채팅방 목록 응답(`ResponseOpenChatRoomDto`)에 `isJoinable` 필드를 추가하고,
모든 탭(MY·ALL·DORMITORY)의 목록 조회 결과를 "모집 중(OPEN) 먼저, 모집 마감(CLOSED) 나중" 순으로 정렬한다.
검색(keyword) 결과에도 동일한 정렬이 적용된다.

> 관련 BR: BR-759 (모집 상태 enum 및 기존 DTO 필드 추가)

---

## 동작 명세

### 1) isJoinable 필드 계산

- `isJoinable = !recruitmentClosed && (currentParticipants < maxParticipants)`
- 모든 채팅방 유형(OPEN, DERIVED, PERSONAL, ROOMMATE)에 동일하게 적용.
  - ROOMMATE 방은 `currentParticipants=2, maxParticipants=2`이므로 항상 `false`.
  - PERSONAL / OPEN 타입은 `recruitmentClosed=false`이므로 정원 초과 여부만 영향.
  - DERIVED 타입은 `recruitmentClosed` + 정원 초과 두 조건 모두 적용.
- 이미 참여한 유저(`isJoined=true`)라도 `isJoinable`은 **방 자체의 속성**으로 계산.

### 2) 정렬 변경

#### ALL 탭 (findAllPublicRooms)

```
1. recruitmentClosed ASC   (false=0 먼저)
2. lastMessageAt DESC NULLS LAST
3. createdDate DESC
```

#### DORMITORY 탭 (findByDormitory)

```
1. recruitmentClosed ASC
2. targetDorm IS NOT NULL DESC   (공식방 OPEN 그룹 내 상단 고정 — 공식방은 항상 OPEN)
3. lastMessageAt DESC NULLS LAST
4. createdDate DESC
```

#### MY 탭 (getMyRooms — 메모리 정렬)

DB 쿼리(`findMyRooms`) 정렬 변경 없음. 오픈채팅 + 룸메이트 병합 후 Java 인메모리 정렬:

```
1. recruitmentStatus == OPEN → 0, CLOSED → 1
2. isMyRoommate DESC               (OPEN 그룹 내 나의 룸메이트 상단)
3. lastMessageAt DESC NULLS LAST
```

> 룸메이트 방은 `recruitmentStatus=OPEN`이므로 항상 OPEN 그룹에 속한다.

### 3) 검색 결과 포함

- ALL·DORMITORY·MY 탭의 keyword 검색 시 동일한 정렬 로직이 적용된다.
- `recruitmentStatus` 필드는 BR-759에서 이미 `ResponseOpenChatRoomDto`에 포함되어 있으므로 별도 작업 없음.

---

## 도메인 데이터

### ResponseOpenChatRoomDto (기존 DTO에 필드 1개 추가)

| 필드 | 타입 | 설명 |
|---|---|---|
| `isJoinable` | `boolean` | `!recruitmentClosed && currentParticipants < maxParticipants` |

기존 필드 유지 (BR-759): `recruitmentClosed`, `recruitmentStatus`, `lastStatusChangedAt`, `lastStatusChangedBy`.

---

## 비즈니스 규칙 / 제약

- `isJoinable`은 방 속성 기준으로 계산. 호출자의 기숙사·비밀번호 일치 여부는 반영하지 않는다.
- 정렬 기준 ①은 `recruitmentClosed` boolean 기준이며 `OpenChatRoomRecruitmentStatus` enum과 항상 일치한다(BR-759 불변조건).
- DORMITORY 탭의 공식방 상단 고정(기존 동작)은 유지하되, CLOSED 공식방이 존재할 경우 CLOSED 그룹으로 내려간다. (현재 공식방은 CLOSED 전환 불가 — BR-759 비목표 — 이므로 실질적으로 항상 OPEN 그룹 내에 위치.)
- MY 탭에서 `isMyRoommate` 우선순위는 OPEN 그룹 내에서만 유지된다.

---

## 예외 · 경계 상황

- `lastMessageAt` null 방(신규 방, 메시지 없음): NULLS LAST 처리 → 같은 상태 그룹 내 하단.
- CLOSED 방을 이미 참여 중인 유저가 조회: `isJoined=true`, `isJoinable=false`.
- `maxParticipants` 초과 + OPEN 상태: `isJoinable=false`, `recruitmentStatus=OPEN`.

---

## 비목표 (Non-goals)

- `isJoinable` 기준에 기숙사·비밀번호·scope 조건 포함 (단순 CLOSED+정원 초과만)
- 정렬 기준을 API 파라미터로 제어하는 기능
- 기존 `/open-chat-rooms/{roomId}` 상세 조회 API 변경 (목록 API만 대상)
- `getRoomsForDormitory` (Admin용) 정렬 변경

---

## 수용 기준 (Acceptance Criteria)

### AC-01 목록에 isJoinable 포함 — OPEN + 정원 여유
- Given DERIVED 방 A: `recruitmentClosed=false`, `currentParticipants=2`, `maxParticipants=10`
- When ALL 탭 목록 조회
- Then 응답에 `isJoinable=true`

### AC-02 목록에 isJoinable 포함 — CLOSED
- Given DERIVED 방 B: `recruitmentClosed=true`
- When ALL 탭 목록 조회
- Then `isJoinable=false`

### AC-03 목록에 isJoinable 포함 — 정원 초과
- Given OPEN 방 C: `recruitmentClosed=false`, `currentParticipants=10`, `maxParticipants=10`
- When ALL 탭 목록 조회
- Then `isJoinable=false`

### AC-04 ALL 탭 정렬 — OPEN 먼저
- Given 방 [CLOSED, OPEN, CLOSED, OPEN]
- When ALL 탭 조회
- Then 결과 순서: OPEN 2개 → CLOSED 2개

### AC-05 ALL 탭 정렬 — 같은 상태 내 lastMessageAt DESC
- Given OPEN 방 X (lastMessageAt=2025-01-02), OPEN 방 Y (lastMessageAt=2025-01-01)
- When ALL 탭 조회
- Then 결과: X → Y

### AC-06 DORMITORY 탭 정렬 — OPEN 먼저, 공식방 OPEN 그룹 내 상단
- Given 공식방 O(OPEN), 일반 DERIVED 방 D1(OPEN), D2(CLOSED)
- When DORMITORY 탭 조회
- Then 결과: O → D1 → D2

### AC-07 MY 탭 정렬 — CLOSED 방 하단
- Given 참여 중인 DERIVED 방 M1(CLOSED, lastMessageAt=오늘), OPEN 방 M2(OPEN, lastMessageAt=어제)
- When MY 탭 조회
- Then 결과: M2(OPEN) → M1(CLOSED) — CLOSED가 최신 메시지여도 하단

### AC-08 MY 탭 정렬 — OPEN 그룹 내 isMyRoommate 상단
- Given 룸메이트 채팅방 R(OPEN), 일반 채팅방 M3(OPEN, lastMessageAt=더 최신)
- When MY 탭 조회
- Then 결과: R(isMyRoommate=true) → M3

### AC-09 검색 결과에도 동일 정렬 적용
- Given DERIVED 방 [CLOSED, OPEN] (이름에 "기숙사" 포함)
- When ALL 탭에서 keyword="기숙사" 검색
- Then 결과: OPEN → CLOSED 순

### AC-10 isJoinable — 이미 참여 중인 CLOSED 방
- Given 유저 U가 참여 중인 DERIVED 방 (`isJoined=true`, `recruitmentClosed=true`)
- When MY 탭 조회
- Then `isJoined=true`, `isJoinable=false` 동시 성립
