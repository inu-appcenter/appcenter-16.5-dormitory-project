# BR-782 — 설계 문서

## 엔티티 / 값 객체

**OpenChatRoom** — 기존 엔티티 재사용, 신규 필드 없음.

변경 대상 도메인 메서드:

| 메서드 | 현재 검증 조건 | 변경 후 검증 조건 |
|--------|--------------|----------------|
| `updateRecruitmentStatus(status, actorId)` | `roomType != DERIVED` → `OPEN_CHAT_ROOM_NOT_DERIVED` | `roomType == PERSONAL` → `OPEN_CHAT_ROOM_NOT_ALLOWED_TYPE` |

`closeRecruitment(actorId)` — `PATCH /{roomId}/close-recruitment` 전용 레거시 메서드.
이번 BR 범위 밖이므로 **수정하지 않는다.**

---

## 애그리거트 경계

변경 없음. `OpenChatRoom` 단일 애그리거트 루트.

---

## 연관관계

변경 없음.

---

## DB 스키마 변경

**없음.** `recruitmentClosed`, `lastStatusChangedBy`, `lastStatusChangedAt` 컬럼이 이미 존재한다.

---

## 도메인 계층 구조

```
domain/openChat/
├── entity/
│   └── OpenChatRoom.java              ← 수정: updateRecruitmentStatus() 타입 검증 변경
├── controller/
│   └── OpenChatRoomApiSpecification.java ← 수정: updateRecruitmentStatus @Operation 설명
└── (나머지 파일 변경 없음)

global/exception/
└── ErrorCode.java                     ← 수정: OPEN_CHAT_ROOM_NOT_ALLOWED_TYPE 추가
```

### 신규 생성 파일
없음.

### 수정 파일 상세

#### `OpenChatRoom.java`

```java
// Before
public void updateRecruitmentStatus(OpenChatRoomRecruitmentStatus status, Long actorId) {
    if (this.roomType != OpenChatRoomType.DERIVED) {
        throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_DERIVED);
    }
    ...
}

// After
public void updateRecruitmentStatus(OpenChatRoomRecruitmentStatus status, Long actorId) {
    if (this.roomType == OpenChatRoomType.PERSONAL) {
        throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_ALLOWED_TYPE);
    }
    ...
}
```

#### `ErrorCode.java`

`OPEN_CHAT_ROOM_NOT_DERIVED`(22029) 바로 다음 번호에 추가:
```java
OPEN_CHAT_ROOM_NOT_ALLOWED_TYPE(BAD_REQUEST, 22034, "[OpenChat] 모집 상태를 변경할 수 없는 방 타입입니다."),
```
> 22030~22033은 이미 사용 중이므로 실제 다음 미사용 번호를 확인 후 지정.

#### `OpenChatRoomApiSpecification.java`

`updateRecruitmentStatus()` `@Operation` description 수정:
- `OPEN` / `PERSONAL` / 공식방은 400 반환 → `PERSONAL`은 400 반환, `OPEN`(공식 포함) 허용
- `@Parameter` 설명 "파생 톡방 ID" → "채팅방 ID"

---

## 변경이 필요 없는 이유

| 파일 | 이유 |
|------|------|
| `OpenChatRoomService.updateRecruitmentStatus()` | `originRoomId == null`이면 시스템 메시지 발송 건너뜀 — OPEN 타입(originRoomId 없음)은 이미 자연히 메시지 없이 처리됨 |
| `OpenChatRoomService.joinRoom()` | line 297의 `isRecruitmentClosed()` 체크가 타입 무관하게 동작 — OPEN 방 CLOSED 시 입장 차단 이미 처리됨 |
| DB 스키마 | `recruitmentClosed`, `lastStatusChangedBy`, `lastStatusChangedAt` 모두 기존 컬럼 |

---

## 비목표

- `closeRecruitment()` / `PATCH /{roomId}/close-recruitment` 엔드포인트 확장 (별도 레거시 API, 이번 BR 범위 밖)
- `OPEN_CHAT_ROOM_NOT_DERIVED` ErrorCode 제거 또는 deprecate (closeRecruitment에서 여전히 사용 중)
- OPEN 타입 방 모집 이벤트 메시지 발송
- `transitionCount` 증가 로직 추가
