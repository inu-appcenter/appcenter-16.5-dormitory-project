# 채팅 WebSocket 이벤트

오픈채팅은 `/sub/openchat/{roomId}`, 룸메톡은 `/sub/roommate/chat/{roomId}` 하나만 구독한다.
두 도메인의 방 ID는 중복될 수 있으므로 기본 토픽은 구분한다. 공동구매 채팅은 이번 변경 대상이 아니다.

기존 오픈채팅 `/read`, `/edit`, `/recruitment-status` 및 룸메톡 `/read/{roomId}/user/{userId}`로는 더 이상 발행하지 않는다.
클라이언트는 기본 토픽의 JSON `eventType`으로 처리해야 한다. 서버와 클라이언트 변경을 함께 배포해야 한다.
`/pub` 전송 경로 및 REST API 경로는 기존과 같다.

| eventType | 의미 | 주요 payload |
| --- | --- | --- |
| MESSAGE_CREATED | 텍스트·답장·이미지·시스템·봇 메시지 생성 | 오픈채팅은 ResponseOpenChatMessageCreateEventDto |
| STUDENT_ID_REQUEST | 오픈채팅 학번 공개 요청 메시지 생성 | 생성 DTO + disclosureRequestId |
| ROOM_LINK_CREATED | 오픈채팅 방 링크·재모집 카드 생성 | 생성 DTO + linkedRoom 필드 (type으로 ROOM_LINK / REOPEN_CARD 구분) |
| MESSAGE_UPDATED | 오픈채팅 메시지 수정 | messageId, roomId, content, editedAt |
| MESSAGE_DELETED | 메시지 삭제 | messageId, roomId, content |
| MESSAGE_READ | 읽음 상태 갱신 | 아래 도메인별 형식 |
| RECRUITMENT_STATUS_CHANGED | 오픈채팅 파생방 모집 상태 변경 | derivedRoomId, recruitmentStatus |

`type`은 기존 메시지 종류(TEXT, IMAGE 등)이고 `eventType`은 이벤트 종류다.
오픈채팅은 소켓 생성 전용 DTO와 REST 응답 DTO를 분리한다. REST 응답은 기존 ResponseOpenChatMessageDto를 유지한다.
생성 이벤트에는 unreadCount가 없으며, 이어서 보내는 MESSAGE_READ의 값으로 설정한다.
프론트는 messageId로 읽음 상태를 연결하고, 생성 이벤트보다 먼저 받은 읽음 상태도 보관하여 적용한다.
재연결 시 REST 조회 결과의 unreadCount로 상태를 복구한다.

오픈채팅 읽음 이벤트는 기존 메시지별 읽지 않은 인원 수를 유지한다. roomId는 구독 토픽에서 식별한다.

```json
{"eventType":"MESSAGE_READ","messageId":100,"unreadCount":0}
```

룸메톡 읽음 이벤트는 기존 ID 배열을 DTO로 감싼다.
`readerId`는 알림 수신자가 아니라 읽은 사용자다. 방의 모든 구독자가 수신한다.

```json
{"eventType":"MESSAGE_READ","roomId":7,"readerId":2,"messageIds":[100,101]}
```

기본 방 토픽 구독을 방 입장으로 간주하는 기존 정책을 유지하므로 화면 이탈 시 구독을 해제한다.
알 수 없는 eventType은 새 메시지로 렌더링하지 않는다.
