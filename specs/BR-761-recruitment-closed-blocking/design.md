# BR-761 마감 상태의 참여 차단 — 설계 문서

## 기능 개요
BR-761은 BR-759 "파생 톡방 모집 상태 관리"의 핵심 기능 중 하나인 **joinRoom 차단**의 구현을 다룬다. BR-759에서 도입한 `recruitmentClosed` boolean 필드와 enum `OpenChatRoomRecruitmentStatus`를 활용하여, CLOSED 상태의 파생 톡방에 대한 비참여자의 입장을 차단한다.

## 엔티티 / 값 객체

### OpenChatRoom (기존 엔티티, BR-759에서 도입)
| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `recruitmentClosed` | `boolean` | NOT NULL, default false | 모집 상태 (false=OPEN, true=CLOSED) |
| `closedAt` | `LocalDateTime` | nullable | CLOSED 상태로 전환된 시각 |
| `closedBy` | `Long` | nullable | CLOSED 상태로 전환한 사용자 ID |
| `lastStatusChangedBy` | `Long` | nullable | 마지막 상태 변경을 실행한 사용자 ID |
| `lastStatusChangedAt` | `LocalDateTime` | nullable | 마지막 상태 변경 시각 |

**도메인 메서드:**
- `isRecruitmentClosed()`: boolean — `recruitmentClosed == true` 판정. joinRoom에서 호출됨.
- `updateRecruitmentStatus(OpenChatRoomRecruitmentStatus status, Long actorId)` (BR-759): 상태 전환 로직. joinRoom에서는 호출 안 함.

### OpenChatParticipant (기존 엔티티, 변경 없음)
| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | `Long` | PK | 참여자 매핑 ID |
| `roomId` | `Long` | FK, NOT NULL | OpenChatRoom 참조 |
| `userId` | `Long` | FK, NOT NULL | User 참조 |
| `joinedAt` | `LocalDateTime` | NOT NULL | 입장 시각 |
| `isHost` | `boolean` | NOT NULL | 방장 여부 |

**도메인 메서드:**
- `create(Long roomId, Long userId, LocalDateTime joinedAt, ChatNotificationMode mode)`: 정적 팩토리, joinRoom에서 호출.

### OpenChatRoomRecruitmentStatus (기존 Enum, BR-759에서 도입)
```java
public enum OpenChatRoomRecruitmentStatus {
    OPEN,    // recruitmentClosed == false
    CLOSED   // recruitmentClosed == true
}
```

## 애그리거트 경계

### OpenChatRoom 애그리거트 루트
- 루트: `OpenChatRoom`
- 내부: 상태 필드들 (`recruitmentClosed`, `closedAt`, `closedBy`, `lastStatusChangedBy/At`)
- 경계: `OpenChatRoom` ID로만 참조됨. joinRoom 로직에서는 `roomId`를 받아 `OpenChatRoomRepository.findByIdWithLock(roomId)`로 조회.

### OpenChatParticipant 애그리거트 루트
- 루트: `OpenChatParticipant`
- 참여자 존재 여부는 `OpenChatParticipantRepository.existsByRoomIdAndUserId(roomId, userId)`로 판정.
- joinRoom에서는 중복 참여 검증(`existsByRoomIdAndUserId`)과 새 참여 생성(`save`)만 담당.

## 연관관계

| 관계 | 설명 |
|---|---|
| `OpenChatRoom` - `OpenChatParticipant` | 1:N, FK(`roomId`). `OpenChatRoom`에서 컬렉션 노출 안 함. |
| `OpenChatParticipant` - `User` | N:1, FK(`userId`). LAZY 로딩. |
| joinRoom에서 검증 | `room.isRecruitmentClosed()` → 509 차단 (기존 OPEN_CHAT_ROOM_CLOSED_FOR_JOIN 재사용) |

**Fetch 전략:**
- `OpenChatRoom`: `findByIdWithLock(roomId)` — 비관적 락 + LAZY 로딩 (관계사 컬렉션 불필요).
- `OpenChatParticipant`: 필요시만 조회 (기존 `existsByRoomIdAndUserId` 활용).

## DB 스키마 변경

**없음**. BR-759에서 이미 추가된 필드들을 그대로 사용:
- `OpenChatRoom.recruitment_closed` (boolean, default 0)
- `OpenChatRoom.closed_at` (datetime, nullable)
- `OpenChatRoom.closed_by` (bigint, nullable)
- `OpenChatRoom.last_status_changed_by` (bigint, nullable)
- `OpenChatRoom.last_status_changed_at` (datetime, nullable)

## 도메인 계층 구조

### 신규 생성 클래스
**없음**. BR-759에서 도입한 엔티티와 enum을 그대로 사용.

### 수정 클래스

#### `OpenChatRoomService.joinRoom(Long userId, Long roomId, String password)`
**위치:** `src/main/java/com/example/appcenter_project/domain/openChat/service/OpenChatRoomService.java:304`

**현재 구현 (BR-759 이미 포함):**
```java
public ResponseOpenChatRoomDetailDto joinRoom(Long userId, Long roomId, String password) {
    OpenChatRoom room = openChatRoomRepository.findByIdWithLock(roomId)
            .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND));

    if (openChatParticipantRepository.existsByRoomIdAndUserId(roomId, userId)) {
        return toDetailDtoWithBlockedCheck(room, roomId, userId);
    }

    if (room.isRecruitmentClosed()) {  // ← CLOSED 상태 검증 (BR-759)
        throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_CLOSED_FOR_JOIN);
    }

    // ... 이후 표준 가입 로직
}
```

**BR-761 관점:**
- **CLOSED 상태 검증은 이미 312-314번 줄에 구현되어 있음.**
- `isRecruitmentClosed()` 메서드는 `room.recruitmentClosed == true` 판정 (BR-759).
- 비참여자가 CLOSED 방에 입장 시 `ErrorCode.OPEN_CHAT_ROOM_CLOSED_FOR_JOIN` (409) 반환.
- **BR-761은 이 기능의 명세 및 테스트 강화를 다루며, 새로운 코드 추가는 최소화.**

#### 변경 내용 (BR-761 구현 후)
- **joinRoom 메서드 변경 없음**. 기존 309-314번 줄의 검증 로직이 BR-761의 모든 요구사항을 충족.
- 동시성 처리: DB 트랜잭션 격리 수준(기본 `REPEATABLE_READ`)과 `findByIdWithLock()`의 비관적 락(SELECT FOR UPDATE)으로 자동 보장.
  - 마감(`updateRecruitmentStatus(CLOSED)`)과 joinRoom 동시 요청 시 락 대기로 순서화됨.
- 테스트만 추가 (AC-01 ~ AC-10).

### 기타 도메인 계층

| 계층 | 클래스 | 변경 |
|---|---|---|
| **Repository** | `OpenChatRoomRepository` | 없음. 기존 `findByIdWithLock()` 사용. |
| **Repository** | `OpenChatParticipantRepository` | 없음. 기존 메서드 사용. |
| **DTO** | `ResponseOpenChatRoomDetailDto` | 없음. BR-759에서 이미 `recruitmentStatus` enum 필드 추가됨. |
| **DTO** | `ResponseOpenChatRoomDto` | 없음. BR-759에서 이미 `recruitmentStatus` enum 필드 추가됨. |
| **Enum** | `OpenChatRoomRecruitmentStatus` | 없음. BR-759에서 이미 정의됨. |
| **ErrorCode** | 신규 코드 | 없음. 기존 `OPEN_CHAT_ROOM_CLOSED_FOR_JOIN` 재사용. |

## 동시성 처리 전략

### 시나리오: CLOSED 전환과 joinRoom 동시 요청

**Case 1: 마감(updateRecruitmentStatus)이 먼저 DB 커밋**
1. `updateRecruitmentStatus(CLOSED, actorId)` → `room.recruitmentClosed = true` DB 커밋
2. joinRoom 요청 들어옴 → `findByIdWithLock(roomId)` → 비관적 락 대기 → 마감 커밋 후 락 획득
3. `room.isRecruitmentClosed()` 검증 → true → 409 `OPEN_CHAT_ROOM_CLOSED_FOR_JOIN`

**Case 2: joinRoom이 먼저 DB 커밋**
1. joinRoom 요청 → `findByIdWithLock(roomId)` → 비관적 락 획득
2. `room.isRecruitmentClosed()` 검증 → false (아직 OPEN)
3. `OpenChatParticipant.create()` → DB 커밋
4. 이후 마감 요청 → 정상 진행 (이미 참여한 유저는 CLOSED 후에도 메시지 송수신 가능)

**구현:**
- Spring Data JPA `@Lock(LockModeType.PESSIMISTIC_WRITE)` 또는 쿼리 메서드의 `SELECT ... FOR UPDATE`.
- `OpenChatRoomRepository.findByIdWithLock(roomId)` 메서드가 이미 비관적 락 적용 (BR-759에서 도입).

## 예외 처리

| 상황 | HTTP | ErrorCode | 설명 |
|---|---|---|---|
| CLOSED 방 비참여자 입장 | 409 | `OPEN_CHAT_ROOM_CLOSED_FOR_JOIN` | 마감 상태 차단 |
| 이미 참여자 (중복 입장) | 200 | — | 기존 동작 (CLOSED와 무관) |
| 방 없음 | 404 | `OPEN_CHAT_ROOM_NOT_FOUND` | — |
| 비DERIVED 방 | 400 | `OPEN_CHAT_ROOM_NOT_DERIVED` | DERIVED만 대상 |
| 방 가득 참 | 409 | `OPEN_CHAT_ROOM_FULL` | 정원 초과 |
| 사용자 없음 | 404 | `USER_NOT_FOUND` | — |
| 비밀번호 불일치 | 403 | `OPEN_CHAT_ROOM_FORBIDDEN` | — |
| 기숙사 범위 불일치 | 403 | `OPEN_CHAT_ROOM_FORBIDDEN` | — |

## 비목표

requirement.md와 동일:
- 마감 시 시스템 메시지 또는 FCM 알림 발송.
- 마감 이력 전체 저장 또는 감사 로그.
- OPEN / PERSONAL / 공식 방의 모집 상태 관리.
- 상태 조회 전용 GET 엔드포인트 (기존 상세 조회로 커버).
- 기존 `/close-recruitment` 엔드포인트 통합.
