# BR-743 파생 톡방 마감 — API 명세서

> Base URL: `http://localhost:8080` (context-path 없음)
> 관련 문서: [`requirement.md`](./requirement.md) · [`design.md`](./design.md)

이 문서는 이 BR로 **추가되는 엔드포인트 1개** 와, **기존 엔드포인트 응답 스키마 변경 3건** 을 다룬다. 나열되지 않은 다른 엔드포인트는 이 BR의 범위 밖이며 여기서 만들지 않는다.

---

## 1. 파생 톡방 마감 (방장 또는 관리자 공용)

| 항목 | 내용 |
|------|------|
| **메서드** | `PATCH` |
| **경로** | `/open-chat-rooms/{roomId}/close-recruitment` |
| **인증** | Bearer Token |
| **설명** | 파생 톡방(`DERIVED`)의 모집을 마감한다. **방장(`createdBy`) 또는 `ROLE_ADMIN` 사용자**만 호출 가능. 이후 비참여자의 입장이 차단된다. 채팅·기존 참여자 활동은 계속 가능. 단방향(재개 불가). |

관리자 전용 별도 엔드포인트(`/admin/…`)는 제공하지 않는다. 방장/관리자 분기는 서버 role 검사로 처리 (기존 `kickParticipant` 패턴 준용).

### Request

#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `roomId` | `Long` | ✅ | 마감할 파생 톡방 ID |

#### Query / Body
없음.

### Response

#### 성공 응답 — `204 No Content`
본문 없음. DB 상태: `recruitment_closed = true`, `closed_at = <now>`, `closed_by = <요청자 ID (방장 또는 관리자)>`.

#### 에러 응답

| 상태 코드 | ErrorCode | 발생 조건 |
|-----------|-----------|-----------|
| `400 Bad Request` | `OPEN_CHAT_ROOM_NOT_DERIVED` (22029) | `roomType` 이 `DERIVED` 가 아닌 방(`OPEN` / `PERSONAL` / 공식방)에 요청 |
| `401 Unauthorized` | JWT 계열 | 인증 실패 (기존 공통 처리) |
| `403 Forbidden` | `OPEN_CHAT_ROOM_FORBIDDEN` (22002) | 요청자가 방장도 관리자도 아님 |
| `404 Not Found` | `OPEN_CHAT_ROOM_NOT_FOUND` (22001) | `roomId` 에 해당하는 방이 존재하지 않음 |
| `404 Not Found` | `USER_NOT_FOUND` (2001) | 요청자 계정이 존재하지 않음 |
| `409 Conflict` | `OPEN_CHAT_ROOM_ALREADY_CLOSED` (22030) | 이미 마감된 방을 다시 마감 시도 |

**에러 응답 형식** (프로젝트 공통):
```json
{
  "status": 400,
  "code": 22029,
  "message": "[OpenChat] 파생 톡방이 아닌 방은 마감할 수 없습니다."
}
```

---

## 2. 방 입장 (기존 엔드포인트) — 마감 가드 추가

이 BR로 **동작만 변경**된다. 요청·성공 응답 스키마는 그대로.

| 항목 | 내용 |
|------|------|
| **메서드** | `POST` |
| **경로** | `/open-chat-rooms/{roomId}/participants/me` |
| **인증** | Bearer Token |

### 변경 사항
**비참여자**가 마감된 방(`recruitment_closed = true`)에 입장 요청 시:

| 상태 코드 | ErrorCode | 발생 조건 |
|-----------|-----------|-----------|
| `409 Conflict` | `OPEN_CHAT_ROOM_CLOSED_FOR_JOIN` (22031) | 마감된 방에 비참여자가 입장 시도. 비밀번호·기숙사 스코프·정원 등 다른 검증보다 **먼저** 반환된다. |

기존 참여자가 같은 엔드포인트를 호출하는 경우(방 상세 재조회 목적)에는 마감 여부와 무관하게 정상 응답(`200 OK`)이 반환된다.

### 응답 스키마 변경 — `ResponseOpenChatRoomDetailDto`
| 필드 | 타입 | 변경 | 설명 |
|------|------|------|------|
| `recruitmentClosed` | `boolean` | **신규** | 방의 모집 마감 여부 |

```json
{
  "roomId": 42,
  "name": "파생 톡방 A",
  "description": "...",
  "scope": "DORMITORY",
  "currentParticipants": 4,
  "maxParticipants": 10,
  "isOfficial": false,
  "createdAt": "2026-09-16T10:30:00",
  "isBlockedByPartner": false,
  "recruitmentClosed": true
}
```

---

## 3. 방 목록 / 검색 (기존 엔드포인트) — 응답 스키마 확장

| 항목 | 내용 |
|------|------|
| **메서드** | `GET` |
| **경로** | `/open-chat-rooms?tab={ALL|MY|DORMITORY}&keyword={...}&page=...` |
| **인증** | Bearer Token |

### 응답 스키마 변경 — `ResponseOpenChatRoomDto` (`rooms[]` 원소)

| 필드 | 타입 | 변경 | 설명 |
|------|------|------|------|
| `recruitmentClosed` | `boolean` | **신규** | 방의 모집 마감 여부 |

마감된 방도 검색/목록에 그대로 노출된다(숨기지 않음). 클라이언트가 이 필드로 UI에 "마감" 배지 등을 표시.

```json
{
  "rooms": [
    {
      "roomId": 42,
      "name": "파생 톡방 A",
      "roomType": "DERIVED",
      "scope": "DORMITORY",
      "chatCategory": "OPEN_CHAT",
      "isPublic": true,
      "hasPassword": false,
      "currentParticipants": 4,
      "maxParticipants": 10,
      "isJoined": false,
      "lastMessageAt": "2026-09-16T10:22:00",
      "lastMessage": "안녕하세요",
      "unreadCount": 0,
      "isMyRoommate": false,
      "isBlockedByPartner": false,
      "isDormOfficial": false,
      "recruitmentClosed": true
    }
  ],
  "totalUnreadCount": 0,
  "page": 0,
  "size": 20
}
```

`GET /open-chat-rooms?tab=...` 로 반환되는 페이지네이션 형식은 기존과 동일.

---

## 4. 부모 톡방 메시지 목록 (기존 엔드포인트) — ROOM_LINK 응답 확장

| 항목 | 내용 |
|------|------|
| **메서드** | `GET` |
| **경로** | `/open-chat-messages/{roomId}?cursor={id}&size=...` (기존 경로 유지) |
| **인증** | Bearer Token |

### 응답 스키마 변경 — `ResponseOpenChatMessageDto` (`ROOM_LINK` 타입에만 유의미)

| 필드 | 타입 | 변경 | 설명 |
|------|------|------|------|
| `linkedRoomRecruitmentClosed` | `Boolean` (nullable) | **신규** | 링크된 파생 톡방의 현재 마감 여부. `ROOM_LINK` 타입 메시지에서만 유효. 다른 타입에서는 `null`. |

- 값은 **조회 시점**의 실시간 상태 (메시지 저장 당시 스냅샷 아님).
- 링크된 파생 톡방이 삭제된 경우에도 응답 자체는 실패하지 않으며, 이 필드는 `false` 로 세팅된다.
- 부모 톡방 메시지 목록에 `ROOM_LINK` 가 N개여도 파생방 상태 조회는 **단일 `IN` 쿼리 1건**으로 처리한다(N+1 금지).

```json
{
  "messages": [
    {
      "id": 12345,
      "roomId": 7,
      "senderId": 101,
      "senderNickname": "홍길동",
      "type": "ROOM_LINK",
      "content": "파생 톡방 링크",
      "createdAt": "2026-09-16T10:22:00",
      "unreadCount": 0,
      "linkedRoomId": 42,
      "linkedRoomName": "파생 톡방 A",
      "linkedRoomDescription": "...",
      "linkedRoomMaxParticipants": 10,
      "linkedRoomRecruitmentClosed": true
    },
    {
      "id": 12346,
      "roomId": 7,
      "senderId": 101,
      "senderNickname": "홍길동",
      "type": "TEXT",
      "content": "안녕하세요",
      "createdAt": "2026-09-16T10:23:00",
      "unreadCount": 0,
      "linkedRoomRecruitmentClosed": null
    }
  ],
  "hasNext": false,
  "nextCursor": null
}
```

---

## 이 BR에서 만들지 않는 API

명세를 벗어난 API를 임의로 추가하지 않는다.

- ~~`PATCH /open-chat-rooms/{roomId}/reopen-recruitment`~~ — 재개는 지원 안 함 (단방향)
- ~~`GET /open-chat-rooms/{roomId}/close-recruitment` (상태 조회 전용)~~ — 방 상세/목록 응답의 `recruitmentClosed` 필드로 이미 노출됨
- ~~`GET /open-chat-rooms?closed=true`~~ — 필터링 파라미터 없음. 클라이언트에서 필드로 필터링
- ~~`PATCH /admin/open-chat-rooms/{roomId}/close-recruitment`~~ — 관리자 전용 엔드포인트를 별도로 두지 않음. §1 단일 엔드포인트에서 role 분기

## 추론 항목

> 아래 항목은 코드에 명시적으로 확인되지 않았거나 프로젝트 공통 패턴에서 추론했다. 실제 동작과 다르면 이 문서를 갱신한다.

- **에러 응답 본문 형식** (`status` / `code` / `message` 3필드): 프로젝트 `GlobalExceptionHandler` 관례. `CustomException` + `ErrorCode` 조합에서 표준적으로 사용되는 형태. 실제 형식이 다르면 실제 `GlobalExceptionHandler.handleCustomException(...)` 응답 스키마를 확인 후 갱신.
- **Admin 인가**: `/admin/**` 경로의 `ROLE_ADMIN` 가드는 프로젝트 공통 Spring Security 설정에 위임. 개별 컨트롤러 메서드에 `@PreAuthorize` 를 붙이지 않음. 기존 `OpenChatRoomAdminController` 가 어노테이션 없이 라우팅 레벨 가드에 의존하고 있는 것과 동일.
- **메시지 목록 엔드포인트 경로**: 기존 `OpenChatMessageController` 의 base path 및 파라미터 이름(`cursor`, `size`)은 이 BR에서 변경하지 않으므로 실제 경로 검증은 스킵. 스키마 변경만 이 문서 범위.
- **`ResponseOpenChatMessageDto.linkedRoomRecruitmentClosed` 의 non-ROOM_LINK 메시지 값**: `null` 로 직렬화되도록 Boolean 래퍼 타입 사용. Spring 기본 Jackson 설정에서 `null` 필드가 응답에 포함되는지 여부는 프로젝트 설정에 따름 (`spring.jackson.default-property-inclusion` 미설정 시 포함). 필요 시 `@JsonInclude(NON_NULL)` 적용.
