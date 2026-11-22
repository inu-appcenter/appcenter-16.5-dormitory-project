# BR-TBD 채팅 Mixpanel 퍼널 이벤트

> BR/이슈 번호 미정. 확정되면 폴더명·커밋·PR에 반영한다.

## 목표
방 상세 열람 → 실제 참여 → 첫 메시지 → 모집 마감을 `room_id` + `room_type`으로 연결한다.
메시지는 "서버 저장 성공 + 메시지 ID" 기준으로만 집계한다.

## Non-goals
GroupOrder 채팅, FCM/알림, 기존 이벤트명 변경, 오픈채팅 상세 GET 신설, 미사용 `sendMessageWithReply` 추적.

## 이벤트 (distinct_id = userId, 공통 속성 room_id, room_type)
room_type: ROOMMATE / OPEN / DERIVED / PERSONAL

| 이벤트 | 조건 | 추가 속성 |
|---|---|---|
| chat_room_viewed | 룸메이트 상세 GET 성공 (오픈류는 프론트 발화) | - |
| chat_room_joined | 오픈류 joinRoom으로 참여자 행 신규 저장 / 룸메이트 방 신규 생성 | - |
| chat_message_sent | 사용자 메시지(TEXT, IMAGE) 저장 성공, 트랜잭션 커밋 후 | message_id, message_type |
| chat_first_message_sent | 방당 최초 사용자 메시지 1건 (DB 조건부 UPDATE로 원자적) | message_id |
| recruitment_closed | 파생방 모집이 OPEN→CLOSED로 실제 전이 (distinct_id=처리자) | origin_room_id, participant_count |

## 제외
시스템/봇/방링크/학번요청/재오픈카드 메시지, 수정, 삭제, 저장 실패(예외·조용한 return),
이미 참여 중인 사용자의 join 재호출, 룸메이트 기존 방 재요청·재입장(rejoin).

## 재입장 점검 결과
- 오픈채팅: 참여자 행은 hard delete라 나갔다 재입장하면 신규 참여로 기록된다(이력 없음).
- 룸메이트: 같은 쌍 재요청은 기존 방을 반환하고, 나간 뒤 재입장은 플래그 복구라 신규 참여로 기록하지 않는다.

## 스키마
open_chat_room, roommate_chatting_room에 first_message_sent_at (V21).
엔티티에서는 insertable=false, updatable=false (dirty checking이 값을 NULL로 덮어쓰지 않도록).
