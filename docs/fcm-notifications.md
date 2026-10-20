# FCM 알림 전송 및 클라이언트 연동

기준일: 2026-10-16. 현재 서버 구현을 기준으로 작성한 문서입니다.

## 1. 전송 흐름

```text
알림 발생 → 수신 기기의 FCM 토큰 조회 → FcmOutbox 저장(PENDING)
→ FcmOutboxProcessor 조회 → FcmAsyncSender에서 페이로드 구성
→ Firebase 전송 요청 → 수신 기기에서 알림 처리
```

- FCM은 WebSocket의 `/sub` 구독과 별개의 경로이며, 기기 토큰으로 수신 대상을 지정합니다.
- Outbox 처리 스케줄러는 이전 실행 종료 후 5초 간격으로 실행합니다. 5초 이내 도착을 보장하는 의미는 아닙니다.
- `SENT`는 Firebase가 전송 요청을 수락했다는 서버 기록입니다. 기기 도착·사용자 열람·알림 제거 완료를 의미하지 않습니다.

## 2. action과 status 구분

| 구분 | 용도 | 클라이언트 전달 |
|---|---|---|
| `action` | CREATE: 생성, UPDATE: 수정, DELETE: 제거 명령 | `data.action`으로 전달 |
| `status` | PENDING, PROCESSING, SENT, FAILED 등 발송 상태 관리 | 전달하지 않음 |
| `status=DELETE` | 취소된 Outbox를 발송 대상에서 제외 | 자체적으로 FCM을 보내지 않음 |

삭제 명령은 `action=DELETE`, `status=PENDING`인 **새 Outbox**입니다. 원본을 `status=DELETE`로 바꾸는 것만으로 기기의 알림이 제거되지는 않습니다.

## 3. 공통 페이로드 규칙

아래 예시는 서버가 구성하는 주요 필드이며 토큰과 SDK의 수신 메타데이터는 생략했습니다.

| 필드 | CREATE | UPDATE | DELETE |
|---|---|---|---|
| `notification` | 제목·본문 포함 | 제목·본문 포함 | 없음 |
| `data.action` | CREATE | UPDATE | DELETE |
| `data.type`, `path`, 대상 ID | 라우팅 정보가 있으면 포함 | 포함 | 포함 |
| `data.messageId` | 값이 있으면 포함 | 필수 | 필수 |

- `data`의 모든 값은 문자열입니다. 숫자 ID도 `"123"`처럼 전달합니다.
- null인 선택 필드는 생략합니다. JSON null이나 문자열 `"null"`로 보내지 않습니다.
- 서버 변수 `routingType`은 `data.type`으로, 채팅의 `routingId`는 `data.chatRoomId`로 전달합니다.
- `messageId`는 채팅 메시지 ID입니다. Firebase 전송 응답의 메시지 ID와 다릅니다.
- `messageId`가 없다고 채팅과 무관한 알림인 것은 아닙니다. 묶음 채팅 알림에도 없습니다.
- UPDATE·DELETE에서 routingType, routingId, messageId 중 하나라도 없으면 `FCM_INVALID_PAYLOAD` 예외가 발생하며 해당 페이로드를 전송하지 않습니다.

## 4. 알림 종류별 예시

### 4.1 오픈채팅 생성

제목은 채팅방 이름입니다. 이미지 알림은 같은 구조에서 본문이 `[이미지]`입니다.

```json
{
  "notification": { "title": "오픈채팅방 이름", "body": "안녕하세요" },
  "data": {
    "action": "CREATE",
    "type": "CHAT_OPEN",
    "path": "/chat/open/10",
    "chatRoomId": "10",
    "messageId": "123"
  }
}
```

### 4.2 개인채팅 생성

현재 개인채팅도 제목에 `room.getName()`을 사용합니다.

```json
{
  "notification": { "title": "개인채팅방 이름", "body": "안녕하세요" },
  "data": {
    "action": "CREATE",
    "type": "CHAT_PERSONAL",
    "path": "/chat/open/20",
    "chatRoomId": "20",
    "messageId": "456"
  }
}
```

### 4.3 룸메이트 채팅 생성

즉시 알림의 제목은 발신자 이름(`sender.getName()`)입니다.

```json
{
  "notification": { "title": "발신자 이름", "body": "안녕하세요" },
  "data": {
    "action": "CREATE",
    "type": "CHAT_ROOMMATE",
    "path": "/roommate/chat/30",
    "chatRoomId": "30",
    "messageId": "789"
  }
}
```

### 4.4 오픈채팅·개인채팅 묶음 알림

특정 메시지 하나에 대응하지 않아 messageId를 생략합니다. 개인채팅은 아래 예시에서 type이 `CHAT_PERSONAL`이며 해당 방의 ID와 경로를 사용합니다.

```json
{
  "notification": { "title": "채팅방 이름", "body": "새 메시지 5개" },
  "data": {
    "action": "CREATE",
    "type": "CHAT_OPEN",
    "path": "/chat/open/10",
    "chatRoomId": "10"
  }
}
```

### 4.5 룸메이트 채팅 묶음 알림

```json
{
  "notification": { "title": "룸메이트 채팅", "body": "새 메시지 5개" },
  "data": {
    "action": "CREATE",
    "type": "CHAT_ROOMMATE",
    "path": "/roommate/chat/30",
    "chatRoomId": "30"
  }
}
```

### 4.6 이미 발송한 채팅 알림 수정

기존 제목을 유지하고 최신 본문으로 새 UPDATE를 보냅니다. 개인채팅도 동일하며 type은 `CHAT_PERSONAL`입니다.

```json
{
  "notification": { "title": "기존 알림 제목", "body": "수정된 메시지" },
  "data": {
    "action": "UPDATE",
    "type": "CHAT_OPEN",
    "path": "/chat/open/10",
    "chatRoomId": "10",
    "messageId": "123"
  }
}
```

### 4.7 아직 발송하지 않은 채팅 알림 수정

원본 Outbox의 본문만 갱신합니다. 이후 다음과 같이 CREATE로 발송합니다.

```json
{
  "notification": { "title": "기존 알림 제목", "body": "수정된 메시지" },
  "data": {
    "action": "CREATE",
    "type": "CHAT_OPEN",
    "path": "/chat/open/10",
    "chatRoomId": "10",
    "messageId": "123"
  }
}
```

### 4.8 이미 발송한 채팅 알림 삭제

notification 없이 공통 data만 보냅니다. 개인채팅은 type이 `CHAT_PERSONAL`입니다.

```json
{
  "data": {
    "action": "DELETE",
    "type": "CHAT_OPEN",
    "path": "/chat/open/10",
    "chatRoomId": "10",
    "messageId": "123"
  }
}
```

아직 대기·실패 상태인 원본은 서버에서 발송을 취소하므로 별도 삭제 FCM을 보내지 않습니다.

### 4.9 공지 상세 연결 알림

```json
{
  "notification": { "title": "공지 제목", "body": "공지 내용" },
  "data": {
    "action": "CREATE",
    "type": "ANNOUNCEMENT",
    "path": "/announcements/50",
    "noticeId": "50"
  }
}
```

### 4.10 일반 알림·관리자 전체 발송

`FcmMessageService.enqueueOutbox()`와 전체 발송 경로는 라우팅 정보를 지정하지 않습니다.

```json
{
  "notification": { "title": "서비스 안내", "body": "점검 예정입니다." },
  "data": { "action": "CREATE" }
}
```

Outbox를 거치지 않는 `FcmAsyncSender.sendOne()`·`sendBatch()`는 notification만 구성합니다. 현재 운영 코드에서 이 두 메서드의 호출 경로는 확인되지 않았습니다.

## 5. 라우팅 정의

| type | path | 대상 ID 필드 | 현재 확인된 Outbox 생성 경로 |
|---|---|---|---|
| CHAT_OPEN | `/chat/open/{id}` | chatRoomId | 있음 |
| CHAT_PERSONAL | `/chat/open/{id}` | chatRoomId | 있음 |
| CHAT_ROOMMATE | `/roommate/chat/{id}` | chatRoomId | 있음 |
| ANNOUNCEMENT | `/announcements/{id}` | noticeId | 있음 |
| COMPLAINT | `/complain/{id}` | complaintId | 확인되지 않음 |
| ROOMMATE_POST | `/roommate/list/{id}` | roommatePostId | 확인되지 않음 |
| NOTICE | `/announcements/{id}` | noticeId | 확인되지 않음 |
| CHAT | `/chat/open/{id}` | chatRoomId | 확인되지 않음 |

enum의 경로 정의와 실제 발송 기능은 구분해야 합니다.

## 6. 수정·삭제 처리 상세

### 메시지 수정

1. 해당 방·메시지·채팅 유형의 SENT 원본(CREATE 또는 기존 action=null)을 먼저 조회합니다.
2. PENDING·FAILED인 CREATE 원본의 본문만 갱신합니다. 제목, action, status는 유지합니다.
3. PENDING·FAILED인 이전 UPDATE의 status를 DELETE로 변경합니다.
4. 1번에서 조회한 원본의 토큰과 제목으로 최신 본문의 UPDATE를 생성합니다. 같은 토큰은 하나로 합칩니다.

| 기기별 원본 상태 | 결과 |
|---|---|
| A: SENT | A에 새 UPDATE 전송 예약 |
| B: PENDING | B의 원본 본문 갱신 후 CREATE 발송 |
| C: FAILED | C의 원본 본문 갱신 후 CREATE 재시도 |

### 메시지 삭제

1. 해당 메시지의 PENDING·FAILED 알림 중 삭제 명령이 아닌 레코드를 status=DELETE로 변경합니다.
2. SENT 원본을 조회해 기존 토큰마다 새 `action=DELETE, status=PENDING` 명령을 저장합니다.
3. `markCancellationQueued()`로 원본의 status를 SENT에서 DELETE로 변경합니다.

3번은 원본이 다음 요청에서 다시 취소 대상으로 조회되지 않도록 표시하는 작업입니다. 앱에서 삭제가 완료됐다는 기록이 아닙니다. 명령 저장과 원본 변경은 같은 트랜잭션에 속합니다. 취소된 Outbox는 보관 기간이 지난 뒤 cleanup 대상이 됩니다.

## 7. 플랫폼별 전송 설정

라우팅 정보가 있는 CREATE·UPDATE에는 다음 설정을 추가합니다. 채팅방 10번 예시이며, 이는 data 내부 필드가 아니라 서버 전송 옵션입니다.

```json
{
  "apns": {
    "payload": { "aps": { "sound": "default", "thread-id": "chat_room_10" } }
  },
  "android": {
    "notification": { "sound": "default", "tag": "chat_room_10" }
  }
}
```

그룹 값은 채팅 `chat_room_{id}`, 공지 `notice`, 민원 `complaint`, 룸메이트 게시글 `roommate`입니다.

DELETE에는 iOS 백그라운드 설정을 추가합니다. Android 표시용 notification 설정은 추가하지 않습니다.

```json
{
  "apns": {
    "headers": { "apns-push-type": "background", "apns-priority": "5" },
    "payload": { "aps": { "content-available": 1 } }
  }
}
```

## 8. 클라이언트 처리 계약과 남은 범위

- CREATE: 알림 표시 시 `(type, chatRoomId, messageId)`와 기기의 알림 식별자를 연결해 관리해야 합니다.
- UPDATE: 해당 메시지의 기존 알림을 최신 본문으로 갱신해야 합니다. action 문자열 자체가 OS 알림 수정 명령은 아닙니다.
- DELETE: 해당 메시지의 기기 알림을 제거해야 합니다. notification이 없으므로 제목·본문 접근 전에 action 분기가 필요합니다.
- 라우팅 없는 일반 알림과 messageId 없는 묶음 알림을 별도로 처리해야 합니다.
- 현재 채팅 tag/thread-id는 **방 단위**입니다. 메시지 단위 식별자와 같지 않으므로 위 연결을 서버 설정만으로 완성했다고 볼 수 없습니다.
- UPDATE도 notification을 포함하는 현재 계약입니다. 앱 상태별 표시·콜백 동작을 모바일에서 검증해야 하며, 서버 테스트 통과가 기기 알림 갱신 완료를 뜻하지 않습니다.
- 수정·삭제 API 연결 범위는 오픈채팅·개인채팅입니다. 룸메이트 채팅 수정·삭제 연결과 묶음 알림 갱신·취소는 별도 작업입니다.
- PROCESSING과 수정·삭제 요청이 겹치는 경우 및 기기 도착 순서 역전은 현재 보장하지 않습니다. SENT를 먼저 조회하는 것만으로 전체 동시성 문제가 해결되지는 않습니다.

## 9. 관련 코드

- `FcmOutbox`: 메시지 ID, action, 상태 및 명령 객체 생성
- `FcmOutboxRepository`: 원본 본문 갱신, 이전 UPDATE 취소, SENT 원본 조회
- `FcmOutboxProcessor`: 발송 대상 조회, 배치 구성, 보관 기간이 지난 기록 정리
- `FcmAsyncSender.configurePayload()`: 실제 전송 필드 구성
- `OpenChatNotificationService`: 수정·삭제 시 Outbox 처리
- `OpenChatMessageService`: 메시지 API와 알림 처리 연결
- `RoommateChattingChatService`, `RoommateChattingNotificationService`: 룸메이트 즉시·묶음 알림
- `AnnouncementNotificationService`, `FcmMessageService`: 공지·일반 알림
