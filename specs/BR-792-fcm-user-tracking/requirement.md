# BR-792 FCM 알림 사용자 추적을 위한 analytics_label 및 FcmOutbox userId 컬럼 추가

## 기능 요약
BigQuery로 Export되는 FCM 메시지에 수신자 userId와 알림 종류를 담은 `analytics_label`을 부여하여, 어떤 유저가 어떤 알림을 수신했는지를 BigQuery에서 직접 집계·역추적할 수 있게 한다. 추가로 Firebase가 반환한 messageId를 FcmOutbox에 저장하여 로컬 DB와 BigQuery 이벤트를 1:1 매칭할 수 있게 한다. Android 디바이스 도달 여부(`MESSAGE_DELIVERED` 이벤트) 추적도 함께 활성화한다.

## 동작 명세
- FcmOutbox를 생성하는 모든 호출부(openChat·roommate·announcement 등)는 수신 `userId`를 함께 전달한다.
- `FcmOutbox.create()` 시그니처는 userId를 받는 형태로 확장되며, 기존 레코드는 userId가 null일 수 있다.
- `FcmAsyncSender.sendOutboxBatch()`는 기존 `sendEachForMulticast(MulticastMessage)` 대신 **`sendEach(List<Message>)`** 로 전환하여 토큰(유저)마다 개별 Message를 구성한다.
- 각 Message에는 outbox 정보로 생성한 `analytics_label`을 `FcmOptions`에 부여한다.
- Android 전용 설정으로 `AndroidConfig`에 `setDeliveryReceiptSupport(true)`를 추가하여 Firebase가 `MESSAGE_DELIVERED` 이벤트를 BigQuery에 기록하도록 한다.
- Firebase가 성공 응답으로 반환한 messageId(`projects/xxx/messages/yyy`)를 FcmOutbox의 `firebaseMessageId` 컬럼에 저장한다.

## 도메인 데이터
`FcmOutbox` 엔티티에 두 컬럼을 추가한다.

| 컬럼 | 타입 | nullable | 설명 |
|------|------|----------|------|
| `userId` | `Long` | true | FCM 토큰 소유 유저의 PK. 기존 레코드 호환을 위해 nullable |
| `firebaseMessageId` | `String` | true | Firebase 성공 응답으로 반환되는 메시지 ID (`projects/...`) |

DB 컬럼은 각각 `user_id`, `firebase_message_id`.

## 비즈니스 규칙 / 제약

### analytics_label 포맷 제약
- 최대 **50자**
- 허용 문자: `[a-zA-Z0-9_-]`
- 반드시 **문자로 시작**해야 함

### 라벨 생성 규칙
| 상황 | 포맷 | 예시 |
|------|------|------|
| 즉시 알림 (messageId 있음) | `{routing}_{action}_msg_{messageId}_u_{userId}` | `openchat_create_msg_12345_u_67890` |
| 묶음 알림 (messageId null, routingId 있음) | `{routing}_create_bundle_{routingId}_u_{userId}` | `openchat_create_bundle_42_u_67890` |
| userId가 null인 경우 | 위 두 포맷에서 `_u_{userId}` → `_u_unknown` | `openchat_create_msg_12345_u_unknown` |

### routing 값 매핑
- `CHAT_OPEN` → `openchat`
- `CHAT_PERSONAL` → `personalchat`
- 추후 추가되는 `FcmRoutingType`은 enum 자체에 소문자 변환 규칙을 둔다.

### action 값 매핑
- `CREATE` → `create`
- `UPDATE` → `update`
- `DELETE` → `delete`

### 호출부 요구
- 모든 신규 FcmOutbox 생성은 userId를 전달한다.
- 기존 레코드는 그대로 유지(백필 안 함).

## 예외 · 경계 상황
- **userId null**: outbox 처리 시점에 라벨은 `..._u_unknown`로 변환되어 Firebase에 전송된다.
- **Firebase 응답에 messageId 없음**: `response.isSuccessful()`이 true이지만 messageId가 null인 극단 케이스. 이 경우 `firebaseMessageId`는 null 유지.
- **라벨 길이 초과**: 생성 결과가 50자를 초과하면 label 설정을 생략하고 WARN 로그만 남긴다 (현실적으로 messageId/userId가 극단적으로 길어야 발생).
- **sendEach 전환으로 인한 응답 처리 변화 없음**: `BatchResponse.getResponses()`의 순서가 입력 리스트 순서와 일치한다는 전제는 기존 로직과 동일하게 유지된다.
- **Mixpanel push_sent 이벤트**: 기존에 토큰→userId 역조회로 수행하던 로직은 FcmOutbox.userId를 바로 사용하는 방식으로 단순화한다.

## 비목표 (Non-goals)
- 클라이언트(Android/iOS) ACK API 구현 — 범위 밖
- iOS `MESSAGE_DELIVERED` 추적 — APNs 제약으로 원천적으로 불가
- 기존 FcmOutbox 레코드의 userId/firebaseMessageId 백필 — null 유지
- Firebase BigQuery Export 자체 설정 — 이미 완료됨
- 알림 템플릿/콘텐츠·발송 로직 변경 — 이번 범위는 추적 메타데이터만
- BigQuery 집계 대시보드/슬랙 알림 구성 — 후속 작업
- FCM 재시도·DLQ 정책 변경 — 기존 유지
- 운영 로그 포맷 전면 재설계 — userId/messageId를 로그에 추가하는 정도만 허용

## 수용 기준 (Acceptance Criteria)

### AC1 — userId를 받아 저장
- **Given** `FcmOutbox.create(userId, token, title, body, routingType, routingId, messageId)` 호출
- **When** 신규 FcmOutbox가 생성된다
- **Then** 반환된 outbox의 `userId`가 인자로 전달된 값과 일치한다

### AC2 — 즉시 알림 라벨 포맷
- **Given** userId=67890, routingType=CHAT_OPEN, action=CREATE, messageId=12345인 outbox
- **When** `FcmAsyncSender.sendOutboxBatch()`가 실행된다
- **Then** Firebase로 전송되는 Message의 analytics_label이 `openchat_create_msg_12345_u_67890`이다

### AC3 — 묶음 알림 라벨 포맷
- **Given** userId=67890, routingType=CHAT_OPEN, action=CREATE, routingId=42, messageId=null인 outbox
- **When** sendOutboxBatch가 실행된다
- **Then** analytics_label이 `openchat_create_bundle_42_u_67890`이다

### AC4 — userId null 처리
- **Given** userId=null, routingType=CHAT_OPEN, action=CREATE, messageId=12345인 outbox
- **When** sendOutboxBatch가 실행된다
- **Then** analytics_label이 `openchat_create_msg_12345_u_unknown`이다

### AC5 — Firebase messageId 저장
- **Given** Firebase가 성공 응답으로 `projects/uni-dorm/messages/0:1234567890`을 반환
- **When** sendOutboxBatch가 성공 처리된다
- **Then** 해당 FcmOutbox.firebaseMessageId에 반환된 messageId가 저장된다

### AC6 — Android delivery receipt 활성화
- **Given** Android 토큰이 포함된 outbox 발송
- **When** sendOutboxBatch가 Message를 구성한다
- **Then** AndroidConfig에 `setDeliveryReceiptSupport(true)`가 포함된다

### AC7 — sendEach 전환
- **Given** 같은 title/body/routing이지만 수신 유저가 서로 다른 outbox 다수
- **When** sendOutboxBatch가 실행된다
- **Then** Firebase 호출은 `sendEach(List<Message>)`이며, 각 Message마다 토큰별 analytics_label이 독립적으로 설정된다

### AC8 — 호출부 전체 반영
- **Given** 서비스 코드에서 FcmOutbox.create()를 호출하는 모든 지점(openChat, roommate, announcement 등)
- **When** 신규 알림이 생성된다
- **Then** userId가 null이 아닌 값으로 전달되어 outbox에 저장된다

### AC9 — 기존 레코드 호환
- **Given** 마이그레이션 전에 userId 없이 저장된 FcmOutbox 레코드
- **When** sendOutboxBatch가 해당 레코드를 처리한다
- **Then** 예외 없이 `_u_unknown` 라벨로 Firebase에 전송된다
