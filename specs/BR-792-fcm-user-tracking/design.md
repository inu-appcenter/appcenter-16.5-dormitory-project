# BR-792 도메인 설계

## 엔티티 / 값 객체

### FcmOutbox (수정)
기존 엔티티에 두 필드를 추가한다.

| 필드 | 타입 | nullable | 설명 |
|------|------|----------|------|
| `userId` | `Long` | true | FCM 토큰 소유 유저의 PK. 기존 레코드 호환을 위해 nullable |
| `firebaseMessageId` | `String` (255) | true | Firebase가 `isSuccessful()` 성공 응답으로 반환한 메시지 ID (`projects/uni-dorm/messages/0:...`) |

**기존 필드는 변경 없음** (token, title, body, status, retryCount, maxRetry, nextRetryAt, lastErrorCode, expiredAt, routingType, routingId, messageId, action).

### 정적 팩토리 메서드 재설계
- 현재 `create()` 오버로드 3개 — 모두 `userId`를 포함한 형태로 통일한다.
- 가장 완전한 시그니처를 private 내부 팩토리로 두고 공개 메서드는 명시적 용도별로 분리한다.

```java
public static FcmOutbox create(Long userId, String token, String title, String body);  // 기본
public static FcmOutbox create(Long userId, String token, String title, String body,
                               FcmRoutingType routingType, Long routingId);             // 라우팅 포함
public static FcmOutbox create(Long userId, String token, String title, String body,
                               FcmRoutingType routingType, Long routingId, Long messageId);  // 전체
```

- `userId`는 **nullable**로 허용하되, 호출부는 가능한 모든 경우 전달한다.

### 파생 팩토리 (기존 메서드) 조정
- `createCancellation()`, `createUpdate(String, String)` — 내부에서 `create()`로 새 Outbox를 만들 때 `this.userId`를 그대로 전파한다.
- `firebaseMessageId`는 신규 Outbox에서 항상 null로 시작하고, 발송 성공 시 세터 없이 **패키지 전용 메서드**로 주입한다.

```java
public void recordFirebaseMessageId(String firebaseMessageId) { ... }
```

### analytics_label 생성 책임
- 라벨 조립은 **인프라 레이어(`FcmAsyncSender`)의 private 메서드**에 둔다. 기존 `configurePayload()`와 같은 계층이 자연스럽다.
- 테스트 용이성을 위해 **package-private static 메서드**로 추출한다.

```java
static String buildAnalyticsLabel(FcmOutbox outbox);  // FcmAsyncSender 내부
```

- 라벨 조립 규칙(요구사항 재진술):
  - 즉시 알림: `{routing}_{action}_msg_{messageId}_u_{userId}`
  - 묶음 알림(messageId null): `{routing}_create_bundle_{routingId}_u_{userId}`
  - userId null: `_u_unknown`
  - 라벨이 50자를 초과하면 **생략**하고 WARN 로그만 남김

### routing 값 매핑
새 enum 메서드 `FcmRoutingType.analyticsLabelPrefix()`를 추가하여 enum이 자기 자신의 라벨 prefix를 안다.

| Enum 값 | 라벨 prefix |
|--------|-------------|
| `CHAT_OPEN` | `openchat` |
| `CHAT_PERSONAL` | `personalchat` |
| `CHAT` | `chat` |
| `CHAT_ROOMMATE` | `roommatechat` |
| `ANNOUNCEMENT` | `announcement` |
| `NOTICE` | `notice` |
| `COMPLAINT` | `complaint` |
| `ROOMMATE_POST` | `roommatepost` |

### action 값 매핑
`FcmAction.name().toLowerCase()`를 그대로 사용 (CREATE→create, UPDATE→update, DELETE→delete). 별도 매핑 불필요.

---

## 애그리거트 경계

- `FcmOutbox`는 자체 애그리거트 루트.
- 추가되는 `userId`는 **User 애그리거트 참조를 ID로만 유지**한다 (`@ManyToOne` 연관관계 **사용하지 않음**).
  - 이유:
    1. Outbox는 User의 라이프사이클과 독립적이며 User가 삭제되어도 발송 기록은 유지되어야 함
    2. 기존 `token` 필드도 FcmToken 엔티티 참조가 아닌 문자열로 보관하는 패턴과 일관성 유지
    3. FCM Outbox Processor의 성능상 대량 LAZY 로딩을 피해야 함
- `firebaseMessageId`는 FcmOutbox에 **내부 보관값**으로, 외부 식별자이지 FK 아님.

---

## 연관관계

- **추가 없음.** `userId`는 `Long` 스칼라 필드로만 보관.
- 기존 연관관계도 변경 없음 (FcmOutbox는 원래 User/FcmToken과 JPA 연관관계가 없음).
- 역조회가 필요한 경우(ex. Mixpanel `push_sent` 이벤트) `FcmAsyncSender` 내부에서 수행하던 `fcmTokenRepository.findAllByTokenIn()` 호출을 **제거**하고 `outbox.getUserId()`를 직접 사용한다.

---

## DB 스키마 변경

### 마이그레이션 V19__fcm_outbox_user_tracking.sql

```sql
ALTER TABLE fcm_outbox
    ADD COLUMN user_id BIGINT NULL,
    ADD COLUMN firebase_message_id VARCHAR(255) NULL;

CREATE INDEX idx_fcm_outbox_user_id ON fcm_outbox(user_id);
```

**인덱스 추가 이유**: 운영 중 특정 유저의 알림 발송 이력을 조회할 수 있어야 하기 때문. BigQuery로 가기 전 로컬 debugging이 많이 사용됨.

**인덱스 추가하지 않는 이유 (firebase_message_id)**: Firebase 메시지 ID는 역조회보다 저장 위주이며, 저장되는 값이 긴 문자열이라 인덱스 효율이 낮다.

**기존 FK 제약 추가하지 않음**: userId는 User 테이블 FK가 아닌 "소유자 ID" 스칼라 값으로 취급. User가 탈퇴해도 발송 로그 유지.

---

## 도메인 계층 구조

```
domain/fcm/
├── entity/
│   └── FcmOutbox.java               [수정] userId, firebaseMessageId 추가, create 시그니처 변경
├── enums/
│   ├── FcmAction.java               [변경 없음]
│   └── FcmRoutingType.java          [수정] analyticsLabelPrefix() 메서드 추가
├── service/
│   ├── FcmAsyncSender.java          [수정] sendEach 전환, analytics_label, deliveryReceipt, firebaseMessageId 저장
│   ├── FcmOutboxProcessor.java      [변경 없음]
│   └── FcmMessageService.java       [수정] FcmOutbox.create 호출부에 userId 전달
└── repository/
    └── FcmOutboxRepository.java     [수정] bulkRecordFirebaseMessageId(Map<Long, String>) 추가
```

```
domain/openChat/service/
└── OpenChatNotificationService.java       [수정] create 호출 시 userId 전달 (2곳)

domain/roommate/service/
├── RoommateChattingNotificationService.java [수정] create 호출 시 userId 전달
└── RoommateChattingChatService.java         [수정] create 호출 시 userId 전달

domain/notification/service/
└── AnnouncementNotificationService.java     [수정] create 호출 시 userId 전달
```

```
src/main/resources/db/migration/
└── V19__fcm_outbox_user_tracking.sql   [신규]
```

### 신규 클래스
- **없음.** 모든 변경은 기존 엔티티·서비스·enum·마이그레이션 파일에서 처리.

### 수정할 기존 클래스 (요약)
| 파일 | 변경 핵심 |
|------|----------|
| `FcmOutbox` | 필드 2개 추가, `create()` 시그니처 userId 포함, `recordFirebaseMessageId()`, 파생 팩토리에서 userId 전파 |
| `FcmRoutingType` | `analyticsLabelPrefix()` 메서드 추가 |
| `FcmAsyncSender` | `sendOutboxBatch` 내부에서 `List<Message>` 조립 후 `sendEach`, 각 Message에 `FcmOptions.setAnalyticsLabel()` 부여, `AndroidConfig.setDeliveryReceiptSupport(true)`, Firebase 성공 응답의 messageId 저장, 기존 token→userId 역조회 제거 |
| `FcmOutboxRepository` | 성공 응답 messageId를 bulk로 반영하는 `bulkRecordFirebaseMessageId(Map<Long outboxId, String firebaseMessageId>)` 쿼리 추가 (실제로는 개별 setter 호출이 더 자연스러울 수 있으니 구현 단계에서 선택) |
| `FcmMessageService` | `sendAllUsers()`에서 토큰만 가져오던 로직을 `FcmToken` 객체 기반으로 바꿔 userId 확보 후 전달. `enqueueOutbox(User, ...)`는 user.getId() 전달 |
| `OpenChatNotificationService` | `sendHourlyUnreadNotifications`, `sendImmediateNotifications` 모두 userId 전달 |
| `RoommateChattingNotificationService` | `token.getUser().getId()` 전달 |
| `RoommateChattingChatService` | `receiver.getId()` 전달 |
| `AnnouncementNotificationService` | `token.getUser().getId()` 전달 |

---

## 비목표

### 이번 설계에서 명시적으로 제외
- **User ↔ FcmOutbox JPA 연관관계 추가**: `@ManyToOne`로 묶지 않음. userId는 스칼라.
- **FK 제약**: `user_id`에 FOREIGN KEY 걸지 않음 (User 탈퇴 후에도 로그 유지).
- **기존 레코드 userId 백필 배치**: 마이그레이션에 포함 안 함. null 그대로.
- **FcmOutbox 전용 조회 API/엔드포인트**: 운영용 조회는 DB 직접 조회 또는 추후 별도 BR.
- **클라이언트 ACK API / iOS delivery receipt**: 요구사항 비목표와 동일.
- **BigQuery 쿼리 집합 문서화**: 별도 운영 문서로 분리.
- **Mixpanel 이벤트 스키마 변경**: 기존 `push_sent` 이벤트의 `userId` 전달 방식만 단순화, 이벤트 payload 자체는 그대로.

### 설계 원칙으로 넣지 않은 것
- analytics_label 전용 값 객체(`AnalyticsLabel` record) 도입은 과잉 추상화로 판단, static 메서드 1개로 처리.
- `FcmOutbox.getAnalyticsLabel()` 엔티티 메서드 도입은 안 함 (엔티티가 Firebase 제약을 모르게 유지).
