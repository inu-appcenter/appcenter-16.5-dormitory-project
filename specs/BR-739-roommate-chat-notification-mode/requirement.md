# BR-739 룸메이트 채팅방 알림 모드 (EVERY / BUNDLED / OFF)

## 기능 요약

룸메이트 채팅방에 오픈채팅(`OpenChatParticipant.notificationMode`)과 동일한 EVERY / BUNDLED / OFF 알림 모드를 추가한다.
기존에 수신자 오프라인이면 무조건 FCM을 발송하던 방식을 모드에 따라 분기한다.

---

## 동작 명세

### 알림 모드 변경
- 인증된 사용자가 자신이 참여한 룸메이트 채팅방의 알림 모드를 EVERY / BUNDLED / OFF 중 하나로 변경한다.
- 변경 후 변경된 모드를 응답한다.

### 알림 모드 조회
- 인증된 사용자가 자신이 참여한 룸메이트 채팅방의 현재 알림 모드를 조회한다.

### 메시지 발송 시 FCM 분기
- 수신자가 **EVERY**: 수신자 오프라인이면 즉시 FCM 발송 (기존 동작 유지)
- 수신자가 **BUNDLED**: 즉시 발송하지 않음. 매 정각 스케줄러가 읽지 않은 메시지가 있는 사용자에게 일괄 FCM 발송
- 수신자가 **OFF**: FCM 발송하지 않음

### BUNDLED 스케줄러
- 매 정각(`0 0 * * * *`)에 룸메이트 채팅방에서 BUNDLED 모드인 사용자 중 읽지 않은 메시지(`readByReceiver = false`)가 있는 경우 FCM 발송
- FCM은 FcmOutbox에 저장하여 기존 발송 파이프라인을 통해 전송

---

## 도메인 데이터

### RoommateChattingRoom 엔티티 변경
기존 `RoommateChattingRoom`에 두 필드 추가:

| 필드 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `hostNotificationMode` | `ChatNotificationMode` | `EVERY` | host(게시글 작성자)의 알림 모드 |
| `guestNotificationMode` | `ChatNotificationMode` | `EVERY` | guest(채팅 요청자)의 알림 모드 |

- `ChatNotificationMode` enum은 오픈채팅의 것을 재사용 (`EVERY`, `BUNDLED`, `OFF`)

### 알림 모드 변경 입력
- roomId (path), ChatNotificationMode mode (body)

### 알림 모드 조회 출력
- ChatNotificationMode mode

---

## 비즈니스 규칙 / 제약

1. 해당 채팅방의 host 또는 guest 본인만 자신의 알림 모드를 변경/조회할 수 있다.
2. 상대방의 알림 모드를 변경할 수 없다. (host → hostNotificationMode, guest → guestNotificationMode만 변경)
3. 신규 채팅방 생성 시 두 모드 모두 EVERY로 초기화된다.
4. mode 값은 EVERY / BUNDLED / OFF 중 하나여야 한다 (@NotNull 검증).
5. BUNDLED FCM 발송 시 FCM 토큰이 없는 사용자는 건너뛴다.
6. `FcmRoutingType`에 `CHAT_ROOMMATE`를 추가하여 클라이언트가 룸메이트 채팅방으로 라우팅할 수 있게 한다.

---

## 예외 · 경계 상황

| 상황 | 기대 동작 |
|---|---|
| 채팅방에 속하지 않은 사용자가 알림 모드 변경 시도 | 403 FORBIDDEN (`ROOMMATE_CHAT_ROOM_FORBIDDEN`) |
| 존재하지 않는 채팅방 ID로 요청 | 404 NOT_FOUND (`ROOMMATE_CHAT_ROOM_NOT_FOUND`) |
| mode 값이 null | 400 BAD_REQUEST (@Valid) |
| BUNDLED 스케줄러 실행 시 읽지 않은 메시지 없음 | 발송 없이 종료 |
| BUNDLED 스케줄러 실행 시 FCM 토큰 없음 | 해당 사용자 건너뜀 |

---

## 비목표 (Non-goals)

- 기존 오픈채팅 알림 모드 로직 변경 없음
- 룸메이트 채팅방 FCM 라우팅 경로 변경 없음 (기존 `sendChatNotification` 경로 그대로)
- 알림 모드 변경 이력 저장 없음
- BUNDLED 발송 주기 변경 (정각 고정, 설정 가능성 추가 없음)
- 룸메이트 채팅방 외 다른 도메인 알림 모드 추가 없음

---

## 수용 기준 (Acceptance Criteria)

**AC-1. 알림 모드 변경 — 정상**
- Given: 룸메이트 채팅방의 host 또는 guest인 사용자
- When: PATCH `/roommate/chat/rooms/{roomId}/notification-mode` body `{"mode":"BUNDLED"}` 요청
- Then: 200 OK, `{"mode":"BUNDLED"}` 반환. DB의 hostNotificationMode 또는 guestNotificationMode가 BUNDLED로 변경됨

**AC-2. 알림 모드 변경 — 권한 없음**
- Given: 해당 채팅방에 속하지 않은 사용자
- When: PATCH `/roommate/chat/rooms/{roomId}/notification-mode` 요청
- Then: 403 FORBIDDEN

**AC-3. 알림 모드 조회**
- Given: 룸메이트 채팅방의 참여자
- When: GET `/roommate/chat/rooms/{roomId}/notification-mode` 요청
- Then: 200 OK, 자신의 현재 모드 반환

**AC-4. EVERY 모드 — 즉시 FCM 발송**
- Given: 수신자의 notificationMode = EVERY, 수신자 오프라인
- When: 메시지 전송
- Then: FcmOutbox에 즉시 저장 (FCM 발송 예약)

**AC-5. OFF 모드 — FCM 미발송**
- Given: 수신자의 notificationMode = OFF, 수신자 오프라인
- When: 메시지 전송
- Then: FcmOutbox에 저장되지 않음

**AC-6. BUNDLED 모드 — 즉시 미발송**
- Given: 수신자의 notificationMode = BUNDLED, 수신자 오프라인
- When: 메시지 전송
- Then: FcmOutbox에 즉시 저장되지 않음

**AC-7. BUNDLED 스케줄러 — 미독 메시지 있을 때 FCM 발송**
- Given: BUNDLED 모드인 사용자에게 읽지 않은 룸메이트 채팅 메시지가 N개 존재
- When: 스케줄러 실행
- Then: FcmOutbox에 해당 사용자 토큰으로 "새 메시지 N개" 저장

**AC-8. BUNDLED 스케줄러 — 미독 메시지 없을 때 미발송**
- Given: BUNDLED 모드인 사용자에게 읽지 않은 메시지 없음
- When: 스케줄러 실행
- Then: FcmOutbox에 해당 사용자 항목 저장 안 됨

**AC-9. 기본값 EVERY**
- Given: 새로운 룸메이트 채팅방 생성
- When: 알림 모드 조회
- Then: host/guest 모두 EVERY 반환
