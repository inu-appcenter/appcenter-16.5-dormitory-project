# BR-767 채팅방 목록 API 명세서

> Base URL: `http://localhost:8080`
>
> 변경 대상 엔드포인트: `GET /open-chat-rooms` (채팅방 목록 조회)
> 다른 엔드포인트는 이번 BR에서 변경 없음.

---

## 채팅방 목록 조회

| 항목 | 내용 |
|------|------|
| **메서드** | `GET` |
| **경로** | `/open-chat-rooms` |
| **인증** | Bearer Token (필수) |
| **설명** | 탭에 따라 채팅방 목록을 조회한다. BR-767에서 `isJoinable` 응답 필드 추가 및 정렬 변경. |

### Request

#### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| `tab` | `OpenChatRoomTab` | ✅ | — | `MY` / `DORMITORY` / `ALL` |
| `keyword` | `String` | ❌ | `null` | 방 이름·설명 검색어 |
| `page` | `Int` | ❌ | `0` | 페이지 번호 (0-based) |
| `size` | `Int` | ❌ | `20` | 페이지 크기 |

`OpenChatRoomTab`:
- `MY` — 내가 참여 중인 방 (룸메이트 채팅방 포함)
- `DORMITORY` — 기숙사 전용 공개 방
- `ALL` — 전체 공개 방

### Response

#### 성공 응답 — `200 OK`

**`ResponseChatRoomListDto`**

| 필드 | 타입 | 설명 |
|------|------|------|
| `content` | `List<ResponseOpenChatRoomDto>` | 채팅방 목록 |
| `totalElements` | `Long` | 전체 항목 수 |
| `totalPages` | `Int` | 전체 페이지 수 |
| `pageNumber` | `Int` | 현재 페이지 번호 |
| `pageSize` | `Int` | 페이지 크기 |
| `totalUnreadCount` | `Int` | 전체 안읽은 메시지 합계 (MY 탭에서만 유의미) |

**`ResponseOpenChatRoomDto`** (content 항목)

| 필드 | 타입 | 설명 |
|------|------|------|
| `roomId` | `Long` | 채팅방 ID |
| `name` | `String` | 채팅방 이름 |
| `description` | `String?` | 채팅방 설명 |
| `scope` | `OpenChatRoomScope?` | `ALL` / `DORMITORY` (룸메이트 방은 null) |
| `roomType` | `OpenChatRoomType?` | `OPEN` / `DERIVED` / `PERSONAL` (룸메이트 방은 null) |
| `chatCategory` | `ChatCategory` | `OPEN_CHAT` / `ROOMMATE` |
| `isPublic` | `Boolean?` | 공개 여부 |
| `hasPassword` | `Boolean` | 비밀번호 설정 여부 |
| `currentParticipants` | `Int` | 현재 참여자 수 |
| `maxParticipants` | `Int` | 최대 참여자 수 |
| `isJoined` | `Boolean` | 요청자 참여 여부 |
| `lastMessageAt` | `String? (ISO 8601)` | 마지막 메시지 시각 |
| `lastMessage` | `String?` | 마지막 메시지 미리보기 |
| `unreadCount` | `Int` | 안읽은 메시지 수 (MY 탭에서만 유의미) |
| `isMyRoommate` | `Boolean` | 나의 룸메이트 채팅방 여부 |
| `isBlockedByPartner` | `Boolean` | 상대방이 나를 차단한 경우 true |
| `isDormOfficial` | `Boolean` | 기숙사 공식 채팅방 여부 |
| `recruitmentClosed` | `Boolean` | 모집 마감 여부 (DERIVED만 의미 있음) |
| `recruitmentStatus` | `OpenChatRoomRecruitmentStatus` | `OPEN` / `CLOSED` |
| `lastStatusChangedAt` | `String? (ISO 8601)` | 마지막 모집 상태 변경 시각 |
| `lastStatusChangedBy` | `Long?` | 마지막 모집 상태 변경자 userId |
| `isJoinable` | `Boolean` | **[BR-767 신규]** 참여 가능 여부. `!recruitmentClosed && currentParticipants < maxParticipants` |

> `isJoinable`은 방 자체 속성 기준이며, 요청자의 기숙사·비밀번호 일치 여부는 반영하지 않는다.

#### 정렬 규칙 (BR-767 변경)

| 탭 | 1순위 | 2순위 | 3순위 | 4순위 |
|----|-------|-------|-------|-------|
| `ALL` | `recruitmentClosed ASC` (OPEN 먼저) | `lastMessageAt DESC NULLS LAST` | `createdDate DESC` | — |
| `DORMITORY` | `recruitmentClosed ASC` (OPEN 먼저) | 공식방 상단 고정 | `lastMessageAt DESC NULLS LAST` | `createdDate DESC` |
| `MY` | `recruitmentStatus OPEN=0/CLOSED=1` | `isMyRoommate DESC` | `lastMessageAt DESC NULLS LAST` | — |

> keyword 검색 시에도 동일한 정렬이 적용된다.

```json
{
  "content": [
    {
      "roomId": 1,
      "name": "기숙사 스터디",
      "description": "같이 공부해요",
      "scope": "DORMITORY",
      "roomType": "DERIVED",
      "chatCategory": "OPEN_CHAT",
      "isPublic": true,
      "hasPassword": false,
      "currentParticipants": 3,
      "maxParticipants": 10,
      "isJoined": false,
      "lastMessageAt": "2026-10-04T10:00:00",
      "lastMessage": "안녕하세요!",
      "unreadCount": 0,
      "isMyRoommate": false,
      "isBlockedByPartner": false,
      "isDormOfficial": false,
      "recruitmentClosed": false,
      "recruitmentStatus": "OPEN",
      "lastStatusChangedAt": null,
      "lastStatusChangedBy": null,
      "isJoinable": true
    },
    {
      "roomId": 2,
      "name": "마감된 방",
      "description": "모집 완료",
      "scope": "ALL",
      "roomType": "DERIVED",
      "chatCategory": "OPEN_CHAT",
      "isPublic": true,
      "hasPassword": false,
      "currentParticipants": 5,
      "maxParticipants": 5,
      "isJoined": true,
      "lastMessageAt": "2026-10-04T09:00:00",
      "lastMessage": "마감됐어요",
      "unreadCount": 2,
      "isMyRoommate": false,
      "isBlockedByPartner": false,
      "isDormOfficial": false,
      "recruitmentClosed": true,
      "recruitmentStatus": "CLOSED",
      "lastStatusChangedAt": "2026-10-03T12:00:00",
      "lastStatusChangedBy": 42,
      "isJoinable": false
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "pageNumber": 0,
  "pageSize": 20,
  "totalUnreadCount": 2
}
```

#### 에러 응답

| 상태 코드 | 발생 조건 |
|-----------|-----------|
| `401 Unauthorized` | 인증 토큰 없음 또는 만료 |
| `400 Bad Request` | `tab` 파라미터 누락 또는 enum 외 값 |
