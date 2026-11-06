# BR-792 API 명세

> Base URL: `http://localhost:8080` (FcmController는 `/fcm` 하위)

---

## 결론

**이번 BR은 HTTP API 변경이 없다.**

변경 범위가 전부 백엔드 내부 구현(엔티티 필드 추가, Firebase 호출 방식 변경, 라벨 조립)에 한정되며, 클라이언트가 보는 엔드포인트 계약은 그대로 유지된다. 따라서 신규/수정 엔드포인트는 생성하지 않는다.

---

## 영향 받는 기존 엔드포인트 검토

`FcmController` 산하 엔드포인트를 점검한 결과:

| 메서드 | 경로 | 영향 | 비고 |
|--------|------|------|------|
| `POST` | `/fcm/token` | 없음 | FCM 토큰 저장. userId는 이미 로그인 세션으로 전달 |
| `DELETE` | `/fcm/token` | 없음 | 토큰 해제 |
| `GET` | `/fcm/stats` | 없음 | 집계 통계. 신규 컬럼 노출 없음 |
| `GET` | `/fcm/dlq` | **응답 변경 없음** | `ResponseFcmDlqDto` 필드 그대로 (`userId`/`firebaseMessageId` 비노출 — 비목표) |
| `POST` | `/fcm/dlq/{outboxId}/retry` | 없음 | 상태만 변경 |
| `POST` | `/fcm/send/all` | 없음 | Request/Response 스키마 그대로 |

---

## 내부 계약 변경 (API 아님)

HTTP API는 아니지만 **백엔드 내부 호출 계약**은 변경된다. 이 섹션은 /tdd·/implement 입력용 참고.

### FcmOutbox.create() 시그니처
변경 전:
```java
public static FcmOutbox create(String token, String title, String body);
public static FcmOutbox create(String token, String title, String body, FcmRoutingType, Long routingId);
public static FcmOutbox create(String token, String title, String body, FcmRoutingType, Long routingId, Long messageId);
```

변경 후:
```java
public static FcmOutbox create(Long userId, String token, String title, String body);
public static FcmOutbox create(Long userId, String token, String title, String body, FcmRoutingType, Long routingId);
public static FcmOutbox create(Long userId, String token, String title, String body, FcmRoutingType, Long routingId, Long messageId);
```

**userId는 nullable 허용** (기존 데이터 호환). 모든 호출부는 가능한 값을 전달해야 함.

### Firebase 외부 호출 변경
- `firebaseMessaging.sendEachForMulticast(MulticastMessage)` → `firebaseMessaging.sendEach(List<Message>)`
- Message마다 `FcmOptions.setAnalyticsLabel(...)` 부여
- `AndroidConfig.setDeliveryReceiptSupport(true)` 추가

이 변경은 Firebase SDK 레벨이며 유니돔 외부 API는 영향 없음.

### DB 계약
`fcm_outbox` 테이블에 다음 컬럼이 추가되어 외부 BI/모니터링 쿼리에서 조회 가능해진다.

| 컬럼 | 타입 | nullable | 인덱스 |
|------|------|----------|--------|
| `user_id` | `BIGINT` | YES | `idx_fcm_outbox_user_id` |
| `firebase_message_id` | `VARCHAR(255)` | YES | 없음 |

### BigQuery `analytics_label` 포맷
BigQuery `firebase_messaging.data` 테이블의 `analytics_label` 컬럼에 다음 포맷이 들어가게 된다.

| 상황 | 포맷 | 예시 |
|------|------|------|
| 즉시 알림 | `{routing}_{action}_msg_{messageId}_u_{userId}` | `openchat_create_msg_12345_u_67890` |
| 묶음 알림 | `{routing}_create_bundle_{routingId}_u_{userId}` | `openchat_create_bundle_42_u_67890` |
| userId null | 위 두 포맷의 `_u_{userId}` → `_u_unknown` | `openchat_create_msg_12345_u_unknown` |

운영 담당자가 BigQuery 쿼리로 활용할 수 있는 외부 계약이지만 HTTP 응답으로는 노출되지 않는다.

---

## 추론 항목

> 아래 항목은 코드에서 명시적으로 확인되지 않아 관례 및 코드 패턴으로 추론했습니다.

- `ResponseFcmDlqDto`에 `userId`/`firebaseMessageId` 미포함: design.md의 수정 클래스 목록에 없으므로 비노출로 간주. 관리자 화면에서 디버깅용으로 노출이 필요하다면 별도 BR로 분리한다.
- `/fcm/stats` 응답에 라벨별 집계 미포함: BigQuery로 조회하는 전제. 로컬 집계 노출은 범위 밖.
