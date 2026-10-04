# BR-767 도메인 설계

## 엔티티 / 값 객체

엔티티 변경 없음. `OpenChatRoom`의 기존 필드(`recruitmentClosed`, `maxParticipants`)를 그대로 사용한다.

`isJoinable`은 영속 필드가 아니라 DTO 생성 시 계산되는 파생 값이다.

```
isJoinable = !recruitmentClosed && (currentParticipants < maxParticipants)
```

`currentParticipants`는 `OpenChatParticipant` count 값으로, 이미 `buildOpenChatDtos`에서 `countMap`으로 조회한다.

---

## 애그리거트 경계

변경 없음. `OpenChatRoom`이 애그리거트 루트이며, 이번 BR은 해당 엔티티를 읽기만 한다.

---

## 연관관계

변경 없음.

---

## DB 스키마 변경

없음. `isJoinable`은 DB 컬럼이 아닌 DTO 계산 필드다.

---

## 도메인 계층 구조

```
domain/openChat/
├── dto/
│   └── response/
│       └── ResponseOpenChatRoomDto.java       ← 수정 (isJoinable 추가)
├── repository/
│   └── OpenChatRoomQuerydslRepositoryImpl.java ← 수정 (정렬 변경)
└── service/
    └── OpenChatRoomService.java               ← 수정 (MY 탭 인메모리 정렬 변경)
```

### 신규 클래스

없음.

### 수정 클래스

#### 1. `ResponseOpenChatRoomDto`

- `isJoinable` (`boolean`) 필드 추가.
- `from(room, currentParticipants, joined)` 오버로드 2개: `isJoinable` 세팅 추가.
  ```java
  .isJoinable(!room.isRecruitmentClosed() && currentParticipants < room.getMaxParticipants())
  ```
- `fromRoommate(...)`: 룸메이트 방은 항상 정원(2/2)이므로 `isJoinable = false` 하드코딩.

#### 2. `OpenChatRoomQuerydslRepositoryImpl`

**`findAllPublicRooms` 정렬 변경:**
```
기존: (정렬 없음 — DB 기본 순서)
변경: recruitmentClosed ASC, lastMessageAt DESC NULLS LAST, createdDate DESC
```

**`findByDormitory` 정렬 변경:**
```
기존: targetDorm.isNotNull() DESC, lastMessageAt DESC NULLS LAST, createdDate DESC
변경: recruitmentClosed ASC, targetDorm.isNotNull() DESC, lastMessageAt DESC NULLS LAST, createdDate DESC
```

QueryDSL 표현식:
```java
.orderBy(
    openChatRoom.recruitmentClosed.asc(),
    new CaseBuilder().when(openChatRoom.targetDorm.isNotNull()).then(1).otherwise(0).desc(),
    openChatRoom.lastMessageAt.desc().nullsLast(),
    openChatRoom.createdDate.desc()
)
```

> `findMyRooms`는 DB 정렬 불필요 — 인메모리에서 덮어 쓴다.

#### 3. `OpenChatRoomService.getMyRooms`

`merged.sort(...)` 인메모리 정렬 변경:

```
기존: isMyRoommate DESC → lastMessageAt DESC NULLS LAST
변경: recruitmentStatus(OPEN=0/CLOSED=1) ASC → isMyRoommate DESC → lastMessageAt DESC NULLS LAST
```

Java Comparator:
```java
merged.sort(Comparator
    .comparingInt((ResponseOpenChatRoomDto r) ->
        r.getRecruitmentStatus() == OpenChatRoomRecruitmentStatus.OPEN ? 0 : 1)
    .thenComparing(ResponseOpenChatRoomDto::isMyRoommate, Comparator.reverseOrder())
    .thenComparing(ResponseOpenChatRoomDto::getLastMessageAt,
        Comparator.nullsLast(Comparator.reverseOrder())));
```

> 룸메이트 방은 `fromRoommate()`에서 `recruitmentStatus = OPEN`으로 세팅되므로 항상 OPEN 그룹에 속한다.

---

## 비목표

- 상세 조회 API(`ResponseOpenChatRoomDetailDto`) `isJoinable` 추가 — 명세 범위 밖
- `getRoomsForDormitory` (Admin용) 정렬 변경 — 명세 범위 밖
- 정렬 파라미터화 — 명세 범위 밖
- 엔티티·DB 스키마 변경 — 파생 계산 값이므로 불필요
