# BR-769 채팅 답장 메타데이터 API 명세서

> **Base URL**: `http://localhost:8080`  
> **인증**: 모든 엔드포인트에 Bearer Token(JWT) 필요  
> **변경 범위**: 기존 채팅 API에 답장·soft-delete 필드 추가 + 삭제 엔드포인트 신규

---

## 공통 DTO — ReplySourceDto

답장 메시지 조회 시 원본 메시지 상태를 담는 공통 객체. 답장이 아닌 메시지에서는 `replySource = null`.

| 필드 | 타입 | nullable | 설명 |
|------|------|----------|------|
| `replyToMessageId` | `Long` | No | 원본 메시지 ID |
| `status` | `ReplySourceStatus` | No | 원본 상태 (아래 표 참고) |
| `replyToSenderId` | `Long` | Yes | 원본 작성자 ID (DELETED·NOT_FOUND이면 null) |
| `replyToSenderNickname` | `String` | Yes | 원본 작성자 닉네임 (DELETED·NOT_FOUND이면 null) |
| `contentPreview` | `String` | Yes | 원본 내용 앞 100자 (DELETED·NOT_FOUND이면 null) |
| `replyToRoomType` | `ChatRoomType` | No | 원본 채팅방 유형 (`ROOMMATE` \| `OPEN` \| `DERIVED`) |
| `replyToRoomId` | `Long` | No | 원본 채팅방 ID |
| `replyToDerivedRoomId` | `Long` | Yes | 파생 톡방 ID (status가 RECRUITING·RECRUITMENT_CLOSED일 때만 포함) |

**ReplySourceStatus 값**

| 값 | 조건 |
|---|---|
| `NORMAL` | 원본 메시지 존재 + isDeleted=false + REOPEN_CARD 아님 |
| `DELETED` | 원본 메시지 isDeleted=true |
| `NOT_FOUND` | 원본 메시지 ID가 DB에 없음 |
| `RECRUITING` | 원본이 REOPEN_CARD + 파생 톡방 모집 중 |
| `RECRUITMENT_CLOSED` | 원본이 REOPEN_CARD + 파생 톡방 모집 마감 |

```json
{
  "replyToMessageId": 42,
  "status": "NORMAL",
  "replyToSenderId": 3,
  "replyToSenderNickname": "김철수",
  "contentPreview": "안녕하세요 만나서 반갑습니다",
  "replyToRoomType": "OPEN",
  "replyToRoomId": 1,
  "replyToDerivedRoomId": null
}
```

---

# 오픈채팅 메시지 API

---

## 1. [WebSocket] 오픈채팅 텍스트 메시지 전송

| 항목 | 내용 |
|------|------|
| **프로토콜** | STOMP over WebSocket |
| **발행 경로** | `/pub/openchat/socketchat` |
| **수신 경로** | `/sub/openchat/{roomId}` |
| **인증** | WebSocket 세션의 `userId` 속성 (연결 시 JWT 인증) |
| **설명** | 텍스트 메시지 전송. `replyToMessageId` 포함 시 답장 메시지로 저장. |

### Request (STOMP Payload)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `roomId` | `Long` | ✅ | 채팅방 ID |
| `content` | `String` | ✅ | 메시지 내용 (공백 불가) |
| `replyToMessageId` | `Long` | ❌ | 답장할 원본 메시지 ID (생략하면 일반 메시지) |

```json
{
  "roomId": 1,
  "content": "네 맞아요!",
  "replyToMessageId": 42
}
```

### Response (WebSocket broadcast — `/sub/openchat/{roomId}`)

`ResponseOpenChatMessageDto` (아래 [공통 응답 DTO] 참고)

### 에러 응답 (연결 유지, 브로드캐스트 없음)

| 조건 | 처리 |
|------|------|
| 채팅방 없음 / 참여자 아님 / senderId 없음 | silently return (메시지 저장·브로드캐스트 생략) |
| `replyToMessageId` 존재하지 않음 | `22020 OPEN_CHAT_MESSAGE_NOT_FOUND` |
| 원본이 이미 삭제됨 | `22033 OPEN_CHAT_MESSAGE_ALREADY_DELETED` |
| 답장 불가 메시지 유형 | `22034 OPEN_CHAT_REPLY_NOT_ALLOWED_FOR_TYPE` |
| 다른 방의 메시지에 답장 | `22035 OPEN_CHAT_REPLY_TARGET_NOT_IN_SAME_ROOM` |
| 중첩 답장 시도 | `22036 OPEN_CHAT_NESTED_REPLY_NOT_ALLOWED` |
| REOPEN_CARD derivedRoomId 파싱 실패 | `22037 OPEN_CHAT_DERIVED_ROOM_ID_PARSE_FAILED` |

---

## 2. [REST] 오픈채팅 이미지 메시지 전송

| 항목 | 내용 |
|------|------|
| **메서드** | `POST` |
| **경로** | `/open-chat-rooms/{roomId}/messages/image` |
| **Content-Type** | `multipart/form-data` |
| **인증** | Bearer Token |
| **설명** | 이미지 전송. `replyToMessageId` 포함 시 마지막 이미지 메시지가 해당 원본에 대한 답장으로 저장. |

### Request

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `roomId` | `Long` | ✅ | 채팅방 ID |

#### Request Body (multipart/form-data)

| 파트 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `images` | `MultipartFile[]` | ✅ | 이미지 파일 목록 (jpg/jpeg/png/gif/webp, 최대 5장) |
| `replyToMessageId` | `Long` | ❌ | 답장할 원본 메시지 ID |

### Response

#### 성공 응답 — `201 Created`

`List<ResponseOpenChatMessageDto>` (이미지 1장 = 메시지 1개)

#### 에러 응답

| 상태 코드 | ErrorCode | 발생 조건 |
|-----------|-----------|-----------|
| `400` | `OPEN_CHAT_IMAGE_EMPTY` | 이미지 없음 |
| `400` | `OPEN_CHAT_IMAGE_COUNT_EXCEEDED` | 5장 초과 |
| `400` | `OPEN_CHAT_REPLY_NOT_ALLOWED_FOR_TYPE` | 답장 불가 유형 |
| `400` | `OPEN_CHAT_REPLY_TARGET_NOT_IN_SAME_ROOM` | 다른 방 메시지에 답장 |
| `400` | `OPEN_CHAT_NESTED_REPLY_NOT_ALLOWED` | 중첩 답장 |
| `400` | `OPEN_CHAT_MESSAGE_ALREADY_DELETED` | 원본 삭제됨 |
| `403` | `OPEN_CHAT_NOT_PARTICIPANT` | 채팅방 참여자 아님 |
| `404` | `OPEN_CHAT_ROOM_NOT_FOUND` | 채팅방 없음 |
| `404` | `OPEN_CHAT_MESSAGE_NOT_FOUND` | 원본 메시지 없음 |

---

## 3. [REST] 오픈채팅 메시지 목록 조회

| 항목 | 내용 |
|------|------|
| **메서드** | `GET` |
| **경로** | `/open-chat-rooms/{roomId}/messages` |
| **인증** | Bearer Token |
| **설명** | 커서 기반 페이지네이션. 답장 메시지에 `replySource` 포함. |

### Request

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `roomId` | `Long` | ✅ | 채팅방 ID |

#### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| `lastMessageId` | `Long` | ❌ | — | 커서. 이전 응답의 `nextCursor` 값 (첫 요청 시 생략) |
| `size` | `Int` | ❌ | `30` | 페이지 크기 |

### Response

#### 성공 응답 — `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| `messages` | `List<ResponseOpenChatMessageDto>` | 메시지 목록 |
| `hasNext` | `Boolean` | 더 이전 메시지 존재 여부 |
| `nextCursor` | `Long` | 다음 요청에 쓸 커서 (hasNext=false이면 null) |

**ResponseOpenChatMessageDto 전체 필드**

| 필드 | 타입 | nullable | 설명 |
|------|------|----------|------|
| `messageId` | `Long` | No | 메시지 ID |
| `roomId` | `Long` | No | 채팅방 ID |
| `senderId` | `Long` | Yes | 발신자 ID (SYSTEM 메시지는 null 가능) |
| `senderNickname` | `String` | Yes | 발신자 닉네임 |
| `content` | `String` | No | 메시지 내용 |
| `type` | `OpenChatMessageType` | No | `TEXT` \| `IMAGE` \| `SYSTEM` \| `ROOM_LINK` \| `STUDENT_ID_REQUEST` \| `BOT` \| `REOPEN_CARD` |
| `imageUrls` | `List<String>` | No | 이미지 URL 목록 (IMAGE 타입일 때 포함) |
| `unreadCount` | `Int` | No | 미읽음 수 |
| `createdAt` | `LocalDateTime` | No | 생성 시각 |
| `linkedRoomId` | `Long` | Yes | ROOM_LINK·REOPEN_CARD 연결 방 ID |
| `linkedRoomName` | `String` | Yes | 연결 방 이름 |
| `linkedRoomDescription` | `String` | Yes | 연결 방 설명 |
| `linkedRoomMaxParticipants` | `Integer` | Yes | 연결 방 최대 인원 |
| `linkedRoomRecruitmentClosed` | `Boolean` | Yes | 연결 방 마감 여부 |
| `linkedRoomRecruitmentStatus` | `OpenChatRoomRecruitmentStatus` | Yes | `OPEN` \| `CLOSED` |
| `disclosureRequestId` | `Long` | Yes | 학번 공개 요청 ID (STUDENT_ID_REQUEST 타입) |
| `isBot` | `Boolean` | No | 봇 메시지 여부 |
| `isDeleted` | `Boolean` | No | **[신규]** soft-delete 여부 |
| `replySource` | `ReplySourceDto` | Yes | **[신규]** 답장 원본 정보 (답장이 아닌 메시지는 null) |

```json
{
  "messages": [
    {
      "messageId": 100,
      "roomId": 1,
      "senderId": 5,
      "senderNickname": "홍길동",
      "content": "네 맞아요!",
      "type": "TEXT",
      "imageUrls": [],
      "unreadCount": 2,
      "createdAt": "2026-10-04T10:30:00",
      "linkedRoomId": null,
      "linkedRoomName": null,
      "linkedRoomDescription": null,
      "linkedRoomMaxParticipants": null,
      "linkedRoomRecruitmentClosed": null,
      "linkedRoomRecruitmentStatus": null,
      "disclosureRequestId": null,
      "isBot": false,
      "isDeleted": false,
      "replySource": {
        "replyToMessageId": 42,
        "status": "NORMAL",
        "replyToSenderId": 3,
        "replyToSenderNickname": "김철수",
        "contentPreview": "안녕하세요 만나서 반갑습니다",
        "replyToRoomType": "OPEN",
        "replyToRoomId": 1,
        "replyToDerivedRoomId": null
      }
    },
    {
      "messageId": 99,
      "roomId": 1,
      "senderId": 3,
      "senderNickname": "김철수",
      "content": "안녕하세요 만나서 반갑습니다",
      "type": "TEXT",
      "imageUrls": [],
      "unreadCount": 3,
      "createdAt": "2026-10-04T10:29:00",
      "isBot": false,
      "isDeleted": false,
      "replySource": null
    }
  ],
  "hasNext": true,
  "nextCursor": 99
}
```

#### 에러 응답

| 상태 코드 | ErrorCode | 발생 조건 |
|-----------|-----------|-----------|
| `403` | `OPEN_CHAT_NOT_PARTICIPANT` | 채팅방 참여자 아님 |
| `404` | `OPEN_CHAT_ROOM_NOT_FOUND` | 채팅방 없음 |

---

## 4. [REST] 오픈채팅 메시지 삭제 ⭐ 신규

| 항목 | 내용 |
|------|------|
| **메서드** | `DELETE` |
| **경로** | `/open-chat-rooms/{roomId}/messages/{messageId}` |
| **인증** | Bearer Token |
| **설명** | 메시지 soft-delete. 발신자 본인만 가능. 해당 메시지를 원본으로 참조하는 기존 답장의 `replySource.status`는 이후 `DELETED`로 반환됨. |

### Request

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `roomId` | `Long` | ✅ | 채팅방 ID |
| `messageId` | `Long` | ✅ | 삭제할 메시지 ID |

### Response

#### 성공 응답 — `204 No Content`

응답 바디 없음.

#### 에러 응답

| 상태 코드 | ErrorCode | 발생 조건 |
|-----------|-----------|-----------|
| `400` | `OPEN_CHAT_MESSAGE_ALREADY_DELETED` | 이미 삭제된 메시지 |
| `403` | `OPEN_CHAT_MESSAGE_NOT_OWNED_BY_USER` | 발신자 본인이 아님 |
| `403` | `OPEN_CHAT_NOT_PARTICIPANT` | 채팅방 참여자 아님 |
| `404` | `OPEN_CHAT_MESSAGE_NOT_FOUND` | 메시지 없음 |
| `404` | `OPEN_CHAT_ROOM_NOT_FOUND` | 채팅방 없음 |

---

# 룸메이트 채팅 메시지 API

---

## 5. [WebSocket] 룸메이트 채팅 메시지 전송

| 항목 | 내용 |
|------|------|
| **프로토콜** | STOMP over WebSocket |
| **발행 경로** | `/pub/roommate/socketchat` |
| **수신 경로** | `/sub/roommate/{roomId}` (추론) |
| **인증** | WebSocket 세션의 `userId` 속성 |
| **설명** | 텍스트 메시지 전송. `replyToMessageId` 포함 시 답장 저장. |

### Request (STOMP Payload)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `roommateChattingRoomId` | `Long` | ✅ | 채팅방 ID |
| `content` | `String` | ✅ | 메시지 내용 (공백 불가) |
| `replyToMessageId` | `Long` | ❌ | 답장할 원본 메시지 ID |

```json
{
  "roommateChattingRoomId": 7,
  "content": "안녕하세요!",
  "replyToMessageId": 180
}
```

### Response (WebSocket broadcast)

`ResponseRoommateChatDto` (아래 [공통 응답 DTO] 참고)

### 에러 응답

| 조건 | ErrorCode |
|------|-----------|
| 채팅방 없음 | `ROOMMATE_CHAT_ROOM_NOT_FOUND` |
| 채팅방 권한 없음 | `ROOMMATE_CHAT_ROOM_FORBIDDEN` |
| 원본 메시지 없음 | `10005 ROOMMATE_CHAT_MESSAGE_NOT_FOUND` |
| 원본 이미 삭제됨 | `10006 ROOMMATE_CHAT_MESSAGE_ALREADY_DELETED` |
| 답장 불가 유형 | `10007 ROOMMATE_CHAT_REPLY_NOT_ALLOWED_FOR_TYPE` |
| 다른 방 메시지에 답장 | `10008 ROOMMATE_CHAT_REPLY_TARGET_NOT_IN_SAME_ROOM` |
| 중첩 답장 | `10009 ROOMMATE_CHAT_NESTED_REPLY_NOT_ALLOWED` |

---

## 6. [REST] 룸메이트 채팅 메시지 전송

| 항목 | 내용 |
|------|------|
| **메서드** | `POST` |
| **경로** | `/roommate/chat` |
| **인증** | Bearer Token |
| **설명** | REST 방식 메시지 전송. `replyToMessageId` 포함 시 답장. |

### Request Body

Content-Type: `application/json`

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `roommateChattingRoomId` | `Long` | ✅ | 채팅방 ID |
| `content` | `String` | ✅ | 메시지 내용 (공백 불가) |
| `replyToMessageId` | `Long` | ❌ | 답장할 원본 메시지 ID |

```json
{
  "roommateChattingRoomId": 7,
  "content": "안녕하세요!",
  "replyToMessageId": 180
}
```

### Response

#### 성공 응답 — `200 OK`

`ResponseRoommateChatDto` (아래 참고)

#### 에러 응답

| 상태 코드 | ErrorCode | 발생 조건 |
|-----------|-----------|-----------|
| `400` | `ROOMMATE_CHAT_REPLY_NOT_ALLOWED_FOR_TYPE` | 답장 불가 유형 |
| `400` | `ROOMMATE_CHAT_REPLY_TARGET_NOT_IN_SAME_ROOM` | 다른 방 메시지에 답장 |
| `400` | `ROOMMATE_CHAT_NESTED_REPLY_NOT_ALLOWED` | 중첩 답장 |
| `400` | `ROOMMATE_CHAT_MESSAGE_ALREADY_DELETED` | 원본 삭제됨 |
| `403` | `ROOMMATE_CHAT_ROOM_FORBIDDEN` | 채팅방 권한 없음 |
| `404` | `USER_NOT_FOUND` | 사용자 없음 |
| `404` | `ROOMMATE_CHAT_ROOM_NOT_FOUND` | 채팅방 없음 |
| `404` | `ROOMMATE_CHAT_MESSAGE_NOT_FOUND` | 원본 메시지 없음 |

---

## 7. [REST] 룸메이트 채팅 내역 조회

| 항목 | 내용 |
|------|------|
| **메서드** | `GET` |
| **경로** | `/roommate/chat/{roomId}` |
| **인증** | Bearer Token |
| **설명** | 채팅방 전체 내역 반환. 답장 메시지에 `replySource` 포함. |

### Request

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `roomId` | `Long` | ✅ | 채팅방 ID |

### Response

#### 성공 응답 — `200 OK`

`List<ResponseRoommateChatDto>`

**ResponseRoommateChatDto 전체 필드**

| 필드 | 타입 | nullable | 설명 |
|------|------|----------|------|
| `roommateChattingRoomId` | `Long` | No | 채팅방 ID |
| `roommateChatId` | `Long` | Yes | 메시지 ID (시스템 생성 DTO는 null 가능) |
| `userId` | `Long` | Yes | 발신자 ID (시스템 메시지는 null) |
| `content` | `String` | No | 메시지 내용 |
| `read` | `Boolean` | No | 수신자 읽음 여부 |
| `isSystem` | `Boolean` | No | 시스템 메시지 여부 |
| `createdDate` | `String` | No | 생성 시각 (ISO 8601) |
| `userImageUrl` | `String` | Yes | 발신자 프로필 이미지 URL |
| `disclosureRequestId` | `Long` | Yes | 학번 공개 요청 ID (STUDENT_ID_REQUEST 타입) |
| `isDeleted` | `Boolean` | No | **[신규]** soft-delete 여부 |
| `replySource` | `ReplySourceDto` | Yes | **[신규]** 답장 원본 정보 (답장이 아니면 null) |

```json
[
  {
    "roommateChattingRoomId": 7,
    "roommateChatId": 200,
    "userId": 5,
    "content": "안녕하세요!",
    "read": false,
    "isSystem": false,
    "createdDate": "2026-10-04T10:30:00",
    "userImageUrl": "https://example.com/profile/5.jpg",
    "disclosureRequestId": null,
    "isDeleted": false,
    "replySource": {
      "replyToMessageId": 180,
      "status": "NORMAL",
      "replyToSenderId": 3,
      "replyToSenderNickname": "박영희",
      "contentPreview": "오늘 청소 당번이에요?",
      "replyToRoomType": "ROOMMATE",
      "replyToRoomId": 7,
      "replyToDerivedRoomId": null
    }
  },
  {
    "roommateChattingRoomId": 7,
    "roommateChatId": 199,
    "userId": 3,
    "content": "오늘 청소 당번이에요?",
    "read": true,
    "isSystem": false,
    "createdDate": "2026-10-04T10:28:00",
    "userImageUrl": "https://example.com/profile/3.jpg",
    "disclosureRequestId": null,
    "isDeleted": false,
    "replySource": null
  }
]
```

#### 에러 응답

| 상태 코드 | ErrorCode | 발생 조건 |
|-----------|-----------|-----------|
| `403` | `ROOMMATE_CHAT_ROOM_FORBIDDEN` | 채팅방 권한 없음 |
| `404` | `USER_NOT_FOUND` | 사용자 없음 |
| `404` | `ROOMMATE_CHAT_ROOM_NOT_FOUND` | 채팅방 없음 |

---

## 8. [REST] 룸메이트 채팅 메시지 삭제 ⭐ 신규

| 항목 | 내용 |
|------|------|
| **메서드** | `DELETE` |
| **경로** | `/roommate/chat/messages/{chatId}` |
| **인증** | Bearer Token |
| **설명** | 메시지 soft-delete. 발신자 본인만 가능. |

### Request

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `chatId` | `Long` | ✅ | 삭제할 메시지 ID |

### Response

#### 성공 응답 — `204 No Content`

응답 바디 없음.

#### 에러 응답

| 상태 코드 | ErrorCode | 발생 조건 |
|-----------|-----------|-----------|
| `400` | `ROOMMATE_CHAT_MESSAGE_ALREADY_DELETED` | 이미 삭제된 메시지 |
| `403` | `ROOMMATE_CHAT_MESSAGE_NOT_OWNED_BY_USER` | 발신자 본인이 아님 |
| `404` | `ROOMMATE_CHAT_MESSAGE_NOT_FOUND` | 메시지 없음 |

---

## 신규 ErrorCode 목록

| ErrorCode | HTTP | 코드 | 설명 |
|-----------|------|------|------|
| `OPEN_CHAT_MESSAGE_ALREADY_DELETED` | `400` | 22033 | 이미 삭제된 오픈채팅 메시지 |
| `OPEN_CHAT_REPLY_NOT_ALLOWED_FOR_TYPE` | `400` | 22034 | 답장 불가 메시지 유형 (SYSTEM 등) |
| `OPEN_CHAT_REPLY_TARGET_NOT_IN_SAME_ROOM` | `400` | 22035 | 원본 메시지가 다른 채팅방 소속 |
| `OPEN_CHAT_NESTED_REPLY_NOT_ALLOWED` | `400` | 22036 | 답장에 대한 답장 불가 |
| `OPEN_CHAT_DERIVED_ROOM_ID_PARSE_FAILED` | `500` | 22037 | REOPEN_CARD content에서 derivedRoomId 파싱 실패 |
| `OPEN_CHAT_MESSAGE_NOT_OWNED_BY_USER` | `403` | 22038 | 메시지 발신자가 아님 (삭제 권한 없음) |
| `ROOMMATE_CHAT_MESSAGE_NOT_FOUND` | `404` | 10005 | 룸메이트 채팅 메시지 없음 |
| `ROOMMATE_CHAT_MESSAGE_ALREADY_DELETED` | `400` | 10006 | 이미 삭제된 룸메이트 채팅 메시지 |
| `ROOMMATE_CHAT_REPLY_NOT_ALLOWED_FOR_TYPE` | `400` | 10007 | 답장 불가 메시지 유형 (시스템 메시지 등) |
| `ROOMMATE_CHAT_REPLY_TARGET_NOT_IN_SAME_ROOM` | `400` | 10008 | 원본 메시지가 다른 채팅방 소속 |
| `ROOMMATE_CHAT_NESTED_REPLY_NOT_ALLOWED` | `400` | 10009 | 중첩 답장 불가 |
| `ROOMMATE_CHAT_MESSAGE_NOT_OWNED_BY_USER` | `403` | 10010 | 메시지 발신자가 아님 (삭제 권한 없음) |

---

## 추론 항목

> 코드에서 명시적으로 확인되지 않아 관례 및 패턴으로 추론한 항목입니다.

- **오픈채팅 메시지 삭제 경로**: `/open-chat-rooms/{roomId}/messages/{messageId}` — 기존 REST 패턴 준수, 구현 시 변경 가능
- **룸메이트 메시지 삭제 경로**: `/roommate/chat/messages/{chatId}` — 기존 `/roommate/chat` prefix 하위, 구현 시 확인 필요
- **이미지 메시지 답장 시 `replyToMessageId` 전달 방식**: multipart form-data part로 추가. 구현 방식에 따라 query param 또는 별도 DTO로 변경될 수 있음
- **룸메이트 채팅 WebSocket 수신 경로**: `/sub/roommate/{roomId}` — 기존 코드 미확인, 실제 구현 확인 필요
- **삭제 응답 코드**: `204 No Content` — 프로젝트 내 유사 삭제 API 패턴 준수
