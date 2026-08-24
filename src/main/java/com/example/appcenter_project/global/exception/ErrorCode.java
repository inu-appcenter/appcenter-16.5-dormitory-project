package com.example.appcenter_project.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // JWT
    JWT_NOT_VALID(UNAUTHORIZED, 1001, "[Jwt] 유효하지 않은 Jwt"),
    JWT_ACCESS_TOKEN_EXPIRED(UNAUTHORIZED, 1002, "[Jwt] 만료된 엑세스 토큰입니다."),
    JWT_REFRESH_TOKEN_EXPIRED(UNAUTHORIZED, 1003, "[Jwt] 만료된 리프레시 토큰입니다."),
    JWT_MALFORMED(UNAUTHORIZED, 1004, "[Jwt] 잘못된 토큰 형식입니다."),
    JWT_SIGNATURE(UNAUTHORIZED, 1005, "[Jwt] 유효하지 않은 서명입니다."),
    JWT_UNSUPPORTED(UNAUTHORIZED, 1006, "[Jwt] 지원하지 않는 토큰입니다."),
    JWT_ENTRY_POINT(UNAUTHORIZED, 1007, "[Jwt] 인증되지 않은 사용자입니다."),
    JWT_ACCESS_DENIED(FORBIDDEN, 1008, "[Jwt] 리소스에 접근할 권한이 없습니다."),

    // USER
    USER_NOT_FOUND(NOT_FOUND, 2001, "[User] 사용자를 찾을 수 없습니다."),
    INVALID_REFRESH_TOKEN(UNAUTHORIZED, 2002, "[User] 유효하지 않은 Refresh Token입니다."),
    REFRESH_TOKEN_USER_NOT_FOUND(NOT_FOUND, 2003, "[User] 해당 Refresh Token과 일치하는 사용자가 없습니다."),
    DUPLICATE_USER_NAME(CONFLICT, 2004, "[User] 중복된 닉네임 입니다."),
    FCM_TOKEN_NOT_ENABLE(NOT_ACCEPTABLE, 2005, "[User] 해당 Fcm 토큰을 이용한 알림이 실패했습니다." ),
    USER_KEYWORD_ALREADY_EXISTS(NOT_ACCEPTABLE, 2006, "[User] 해당 키워드는 이미 존재합니다."),
    USER_KEYWORD_NOT_FOUND(NOT_FOUND, 2007, "[User] 해당 키워드는 이미 존재하지 않습니다."),
    USER_NOTIFICATION_NOT_FOUND(NOT_FOUND, 2008, "[UserNotification] 해당 유저 알림은 존재하지 않습니다."),
    INVALID_PASSWORD(UNAUTHORIZED, 2009, "[User] 비밀번호가 일치하지 않습니다."),
    ALREADY_REGISTERED_USER(CONFLICT, 2010, "[User] 이미 계정이 존재합니다"),
    USER_NOT_FRESHMAN(UNAUTHORIZED, 2011, "[User] 신입생 계정이 아닙니다."),

    // GROUP_ORDER
    GROUP_ORDER_NOT_FOUND(NOT_FOUND, 3001, "[GroupOrder] 공동구매 글을 찾을 수 없습니다."),
    GROUP_ORDER_CHAT_ROOM_NOT_FOUND(NOT_FOUND, 3002, "[GroupOrder] 채팅방을 찾을 수 없습니다."),
    USER_GROUP_ORDER_NOT_FOUND(NOT_FOUND, 3003, "[GroupOrder] 사용자의 공동구매 참여 정보를 찾을 수 없습니다."),
    GROUP_ORDER_COMMENT_NOT_FOUND(NOT_FOUND, 3004, "[GroupOrder] 댓글을 찾을 수 없습니다."),
    GROUP_ORDER_TITLE_DUPLICATE(CONFLICT, 3005, "[GroupOrder] 이미 존재하는 제목입니다."),
    GROUP_ORDER_NOT_OWNED_BY_USER(FORBIDDEN, 3006, "[GroupOrder] 공동구매 게시글을 생성한 유저가 아니기 때문에 수정 및 삭제할 권한이 없습니다."),
    GROUP_ORDER_COMMENT_NOT_OWNED_BY_USER(FORBIDDEN, 3007, "[GroupOrder] 공동구매 게시글의 댓글을 생성한 유저가 아니기 때문에 수정 및 삭제할 권한이 없습니다."),
    GROUP_ORDER_LIKE_NOT_FOUND(NOT_FOUND, 3008, "[GroupOrder] 공동구매 게시글의 좋아요를 누른 유저가 아닙니다."),
    ALREADY_GROUP_ORDER_LIKE_USER(UNAUTHORIZED, 3009, "[GroupOrder] 이미 공동구매 게시글에 좋아요를 누른 유저입니다"),
    USER_GROUP_ORDER_CHAT_ROOM_NOT_FOUND(NOT_FOUND, 30010, "[GroupOrder] 사용자의 공동구매 채팅방 참여 정보를 찾을 수 없습니다."),

    // TIP
    TIP_NOT_FOUND(NOT_FOUND, 4001, "[Tip] 팁 게시글을 찾을 수 없습니다."),
    TIP_COMMENT_NOT_FOUND(NOT_FOUND, 4002, "[Tip] 팁 게시글의 댓글을 찾을 수 없습니다."),
    TIP_COMMENT_NOT_OWNED_BY_USER(FORBIDDEN, 4004, "[Tip] 팁 게시글의 댓글을 생성한 유저가 아니기 때문에 수정 및 삭제할 권한이 없습니다."),
    TIP_NOT_OWNED_BY_USER(FORBIDDEN, 4005, "[Tip] 팁 게시글을 생성한 유저가 아니기 때문에 수정 및 삭제할 권한이 없습니다."),
    TIP_LIKE_NOT_FOUND(NOT_FOUND, 4006, "[Tip] 팁 좋아요를 찾을 수 없습니다."),
    ALREADY_TIP_LIKE_USER(UNAUTHORIZED, 4006, "[Tip] 이미 팁에 좋아요를 누른 유저입니다"),
    NOT_LIKED_TIP(UNAUTHORIZED, 4006, "[Tip] 팁에 좋아요를 누른 유저가 아닙니다."),


    // VALIDATION
    VALIDATION_FAILED(BAD_REQUEST, 5001, "[Validation] Request에서 요청한 값이 올바르지 않습니다."),

    // IMAGE
    IMAGE_NOT_FOUND(NOT_FOUND, 6001, "[Image] 이미지를 찾을 수 없습니다."),
    DEFAULT_IMAGE_NOT_FOUND(NOT_FOUND, 6002, "[Image] 기본 이미지를 찾을 수 없습니다."),
    IMAGE_SAVE_FAIL(FORBIDDEN, 6003, "[Image] 저장에 실패했습니다."),
    IMAGE_DIRECTORY_SAVE_FAIL(FORBIDDEN, 6003, "[Image] 이미지 폴더 저장에 실패했습니다."),
    IMAGE_UPDATE_NOT_ALLOWED(FORBIDDEN, 6004, "[Image] 수정 권한이 없습니다."),
    IMAGE_INVALID_FORMAT(BAD_REQUEST, 6005, "[Image] 허용되지 않는 이미지 형식입니다."),


    // ROOMMATE
    // ROOMMATE
    ROOMMATE_USER_NOT_FOUND(NOT_FOUND, 7001, "[Roommate] 해당 유저가 존재하지 않습니다."),
    ROOMMATE_BOARD_NOT_FOUND(NOT_FOUND, 7002, "[Roommate] 게시글을 찾을 수 없습니다."),
    ROOMMATE_CHECKLIST_NOT_FOUND(NOT_FOUND, 7003, "[Roommate] 체크리스트를 찾을 수 없습니다."),
    ROOMMATE_BOARD_ALREADY_EXISTS(CONFLICT, 7004, "[Roommate] 이미 작성된 게시글이 있습니다."),
    ROOMMATE_FORBIDDEN_ACCESS(FORBIDDEN, 7005, "[Roommate] 접근 권한이 없습니다."),
    ROOMMATE_NO_SIMILAR_BOARD(NOT_FOUND, 7006, "[Roommate] 유사도 비교할 게시글이 없습니다."),
    ROOMMATE_UPDATE_NOT_ALLOWED(FORBIDDEN, 7007, "[Roommate] 수정 권한이 없습니다."),
    ROOMMATE_CHECKLIST_UPDATE_FAILED(BAD_REQUEST, 7008, "[Roommate] 체크리스트 수정에 실패했습니다."),
    ROOMMATE_DELETE_NOT_ALLOWED(FORBIDDEN, 7009, "[Roommate] 삭제 권한이 없습니다."),

    // ROOMMATE_BOARD_LIKE
    ROOMMATE_BOARD_LIKE_NOT_FOUND(NOT_FOUND, 7501, "[RoommateBoard] 좋아요 정보를 찾을 수 없습니다."),
    ALREADY_ROOMMATE_BOARD_LIKE_USER(UNAUTHORIZED, 7502, "[RoommateBoard] 이미 해당 게시글에 좋아요를 누른 유저입니다."),

    // ROOMMATE_MATCHING
    ROOMMATE_MATCHING_ALREADY_REQUESTED(CONFLICT, 8101, "[RoommateMatching] 이미 해당 사용자에게 매칭 요청을 보냈습니다."),
    ROOMMATE_MATCHING_NOT_FOUND(NOT_FOUND, 8102, "[RoommateMatching] 해당 매칭 요청을 찾을 수 없습니다."),
    ROOMMATE_MATCHING_ALREADY_COMPLETED(BAD_REQUEST, 8103, "[RoommateMatching] 이미 수락되었거나 실패한 요청입니다."),
    ROOMMATE_MATCHING_NOT_FOR_USER(FORBIDDEN, 8104, "[RoommateMatching] 해당 매칭 요청은 현재 사용자와 관련이 없습니다."),
    ROOMMATE_ALREADY_MATCHED(HttpStatus.CONFLICT,8105,"[RoommateMatching] 이미 매칭된 사람이 있습니다."),
    ROOMMATE_MATCHING_NOT_COMPLETED(BAD_REQUEST, 8106, "[RoommateMatching] 매칭이 완료된 상태가 아닙니다."),


    // ROOMMATE_MYROOMMATE
    MY_ROOMMATE_NOT_REGISTERED(NOT_FOUND, 9201, "[MyRoommate] 해당 사용자의 룸메이트 정보를 찾을 수 없습니다"),
    RULE_NOT_FOUND(NOT_FOUND, 9203, "[MyRoommate] 수정할 규칙이 존재하지 않습니다."),

    // ROOMMATE_CHAT
    ROOMMATE_CHAT_CANNOT_CHAT_WITH_SELF(BAD_REQUEST, 10001, "[RoommateChat] 자신에게는 채팅을 보낼 수 없습니다."),
    DUPLICATE_CHAT_ROOM(CONFLICT, 10001, "[RoommateChat] 이미 존재하는 채팅방입니다."),
    ROOMMATE_CHAT_ROOM_NOT_FOUND(NOT_FOUND, 7013, "[RoommateChat] 채팅방을 찾을 수 없습니다."),
    ROOMMATE_CHAT_ROOM_FORBIDDEN(FORBIDDEN, 10004, "[RoommateChat] 이 채팅방에 속하지 않은 사용자입니다."),
    ROOMMATE_CHAT_ROOM_DENIED(PRECONDITION_FAILED, 10004, "[RoommateChat] 양방향 채팅방 생성은 할 수 없습니다."),


    // REPORT
    REPORT_NOT_REGISTERED(NOT_FOUND, 11001, "[Report] 해당 신고 정보를 찾을 수 없습니다"),

    // ANNOUNCEMENT
    ANNOUNCEMENT_NOT_REGISTERED(NOT_FOUND, 12001, "[ANNOUNCEMENT] 해당 공지사항 정보를 찾을 수 없습니다"),
    ANNOUNCEMENT_FORBIDDEN(FORBIDDEN, 12002, "[ANNOUNCEMENT] 해당 유형의 공지사항에 대한 권한이 없습니다."),
    ANNOUNCEMENT_NOT_FOUND(NOT_FOUND, 12003, "[ANNOUNCEMENT] 공지사항이 존재하지 않습니다."),

    // ATTACHEDFILE
    ATTACHEDFILE_NOT_REGISTERED(NOT_FOUND, 13001, "[ATTACHEDFILE] 해당 파일을 찾을 수 없습니다"),

    // CALENDER
    CALENDER_NOT_REGISTERED(NOT_FOUND, 13001, "[CALENDER] 해당 캘린더 정보를 찾을 수 없습니다"),

    // COMPLAINT
    COMPLAINT_NOT_FOUND(NOT_FOUND, 14001, "[Complaint] 해당 민원을 찾을 수 없습니다."),
    COMPLAINT_REPLY_NOT_FOUND(NOT_FOUND, 14002, "[Complaint] 해당 민원의 답변을 찾을 수 없습니다."),
    COMPLAINT_NOT_OWNED_BY_USER(FORBIDDEN, 14003, "[Complaint] 본인이 작성한 민원이 아니기 때문에 수정 및 삭제할 권한이 없습니다."),
    COMPLAINT_ALREADY_REPLIED(CONFLICT, 14004, "[Complaint] 이미 답변이 등록된 민원입니다."),
    INVALID_COMPLAINT_TYPE(BAD_REQUEST, 14005, "[Complaint] 올바르지 않은 민원 유형입니다."),
    INVALID_COMPLAINT_STATUS(BAD_REQUEST, 14006, "[Complaint] 올바르지 않은 민원 상태입니다."),
    INVALID_DORM_TYPE(BAD_REQUEST, 14007, "[Complaint] 올바르지 않은 기숙사 유형입니다."),
    COMPLAINT_REQUIRED_FIELD_MISSING(BAD_REQUEST, 14008, "[Complaint] 필수 입력값이 누락되었습니다."),

    // USER_GROUP_ORDER
    USER_GROUP_ORDER_KEYWORD_NOT_FOUND(NOT_FOUND, 15001, "[UserGroupOrderKeyword] 해당 유저공동구매키워드를 찾을 수 없습니다."),
    USER_GROUP_ORDER_KEYWORD_ALREADY_EXISTS(NOT_ACCEPTABLE, 15001, "[UserGroupOrderKeyword] 해당 유저공동구매 키워드가 이미 존재합니다"),
    USER_GROUP_ORDER_CATEGORY_ALREADY_EXISTS(FORBIDDEN, 15001, "[UserGroupOrderKeyword] 해당 유저공동구매 카테고리가 이미 존재합니다"),
    USER_GROUP_ORDER_TYPE_NOT_FOUND(NOT_FOUND, 15004, "[UserGroupOrderType] 해당 유저공동구매타입을 찾을 수 없습니다."),

    // POPUP_NOTIFICATION
    POPUP_NOTIFICATION_NOT_FOUND(NOT_FOUND, 16001, "[PopupNotification] 해당 팝업 공지를 찾을 수 없습니다."),

    // FCM
    FCM_TOKEN_NOT_FOUND(NOT_FOUND, 17001, "[FCM] 전체 사용자에게 보낼 FCM 토큰이 존재하지 않습니다."),
    FCM_SEND_FAILED(INTERNAL_SERVER_ERROR, 17002, "[FCM] FCM 메시지 전송 중 오류가 발생했습니다."),
    FCM_OUTBOX_NOT_FOUND(NOT_FOUND, 17003, "[FCM] 해당 Outbox 레코드를 찾을 수 없습니다."),
    FCM_OUTBOX_NOT_RETRYABLE(BAD_REQUEST, 17004, "[FCM] DEAD_EXHAUSTED 상태인 Outbox 레코드만 재시도할 수 있습니다."),

    // SURVEY
    SURVEY_NOT_FOUND(NOT_FOUND, 18001, "[Survey] 해당 설문을 찾을 수 없습니다."),
    SURVEY_NOT_OWNED_BY_USER(FORBIDDEN, 18002, "[Survey] 설문을 생성한 유저가 아니기 때문에 수정 및 삭제할 권한이 없습니다."),
    SURVEY_CLOSED(BAD_REQUEST, 18003, "[Survey] 해당 설문은 종료되었습니다."),
    SURVEY_NOT_IN_PERIOD(BAD_REQUEST, 18004, "[Survey] 해당 설문의 응답 기간이 아닙니다."),
    SURVEY_QUESTION_NOT_FOUND(NOT_FOUND, 18005, "[Survey] 해당 설문 질문을 찾을 수 없습니다."),
    SURVEY_OPTION_NOT_FOUND(NOT_FOUND, 18006, "[Survey] 해당 설문 선택지를 찾을 수 없습니다."),
    SURVEY_RESPONSE_NOT_FOUND(NOT_FOUND, 18007, "[Survey] 해당 설문 응답을 찾을 수 없습니다."),
    ALREADY_SURVEY_RESPONSE(CONFLICT, 18008, "[Survey] 이미 해당 설문에 응답하였습니다."),
    MULTIPLE_SELECTION_NOT_ALLOWED(BAD_REQUEST, 18009, "[Survey] 해당 질문은 단일 선택만 가능합니다."),
    RECRUITMENT_COUNT_MAX(CONFLICT, 18009, "[Survey] 남은 공석이 없습니다"),

    // COUPON
    COUPON_NOT_FOUND(NOT_FOUND, 19001, "[COUPON] 쿠폰이 존재하지 않습니다."),

    // FEATURE
    FEATURE_NOT_FOUND(NOT_FOUND, 12001, "[FEATURE] feature가 존재하지 않습니다."),
    DUPLICATE_FEATURE_KEY(CONFLICT, 12001, "[FEATURE] 같은 key의 feature가 존재합니다."),

    // OPEN_CHAT
    OPEN_CHAT_ROOM_NOT_FOUND(NOT_FOUND, 22001, "[OpenChat] 채팅방을 찾을 수 없습니다."),
    OPEN_CHAT_ROOM_FORBIDDEN(FORBIDDEN, 22002, "[OpenChat] 채팅방 접근 권한이 없습니다."),
    OPEN_CHAT_ROOM_FULL(BAD_REQUEST, 22003, "[OpenChat] 최대 인원에 도달한 채팅방입니다."),
    OPEN_CHAT_PARTICIPANT_NOT_FOUND(NOT_FOUND, 22004, "[OpenChat] 참여하지 않은 채팅방입니다."),
    OPEN_CHAT_NOT_PARTICIPANT(FORBIDDEN, 22005, "[OpenChat] 채팅 내역 조회 권한이 없습니다."),
    OPEN_CHAT_INVITATION_NOT_FOUND(NOT_FOUND, 22006, "[OpenChat] 초대를 찾을 수 없습니다."),
    OPEN_CHAT_INVITATION_INVALID_TARGET(BAD_REQUEST, 22007, "[OpenChat] 초대 대상이 부모 방의 참여자가 아닙니다."),
    OPEN_CHAT_INVITATION_ALREADY_EXISTS(CONFLICT, 22008, "[OpenChat] 이미 대기 중인 초대가 존재합니다."),
    OPEN_CHAT_PARTICIPANT_ALREADY_EXISTS(CONFLICT, 22009, "[OpenChat] 이미 채팅방에 참여 중인 사용자입니다."),
    VALIDATION_ERROR(BAD_REQUEST, 22010, "[OpenChat] 유효하지 않은 요청입니다."),
    OPEN_CHAT_INVITATION_ALREADY_PROCESSED(BAD_REQUEST, 22011, "[OpenChat] 이미 처리된 초대입니다."),
    OPEN_CHAT_INVITATION_SELF_INVITE(BAD_REQUEST, 22012, "[OpenChat] 자기 자신을 초대할 수 없습니다."),
    OPEN_CHAT_IMAGE_EMPTY(BAD_REQUEST, 22013, "[OpenChat] 전송할 이미지가 없습니다."),
    OPEN_CHAT_IMAGE_COUNT_EXCEEDED(BAD_REQUEST, 22014, "[OpenChat] 이미지는 최대 5장까지 전송 가능합니다."),
    OPEN_CHAT_ALREADY_HOST(BAD_REQUEST, 22015, "[OpenChat] 이미 방장인 사용자입니다."),
    OPEN_CHAT_SOLE_HOST_CANNOT_LEAVE(BAD_REQUEST, 22016, "[OpenChat] 단독 방장은 방 삭제 또는 방장 위임 후 나갈 수 있습니다."),
    OPEN_CHAT_KICK_FORBIDDEN(FORBIDDEN, 22017, "[OpenChat] 강제퇴장 권한이 없습니다."),
    OPEN_CHAT_NEW_HOST_REQUIRED(BAD_REQUEST, 22018, "[OpenChat] 방장 강퇴 시 새 방장을 지정해야 합니다."),
    OPEN_CHAT_SELF_PERSONAL_FORBIDDEN(BAD_REQUEST, 22019, "[OpenChat] 자기 자신과 개인 채팅방을 생성할 수 없습니다."),
    OPEN_CHAT_MESSAGE_NOT_FOUND(NOT_FOUND, 22020, "[OpenChat] 메시지를 찾을 수 없습니다."),
    OPEN_CHAT_REPORT_SELF(BAD_REQUEST, 22021, "[OpenChat] 자신의 메시지는 신고할 수 없습니다."),
    OPEN_CHAT_REPORT_NOT_FOUND(NOT_FOUND, 22022, "[OpenChat] 신고 내역을 찾을 수 없습니다."),
    OPEN_CHAT_REPORT_ALREADY_HANDLED(BAD_REQUEST, 22023, "[OpenChat] 이미 처리된 신고입니다."),
    OPEN_CHAT_REPORT_TARGET_INVALID(BAD_REQUEST, 22024, "[OpenChat] 신고할 수 없는 메시지입니다."),
    OPEN_CHAT_NOT_HOST(FORBIDDEN, 22025, "[OpenChat] 방장만 채팅방 정보를 수정할 수 있습니다."),
    OPEN_CHAT_MAX_PARTICIPANTS_TOO_SMALL(BAD_REQUEST, 22026, "[OpenChat] 최대 인원은 현재 참여자 수 이상이어야 합니다."),
    OPEN_CHAT_DORM_OFFICIAL_ROOM_ALREADY_EXISTS(CONFLICT, 22027, "[OpenChat] 해당 기숙사의 공식 채팅방이 이미 존재합니다."),
    OPEN_CHAT_BOT_TARGET_PERSONAL(BAD_REQUEST, 22028, "[OpenChat] 개인 채팅방에는 챗봇 메시지를 전송할 수 없습니다."),

    // STUDENT_ID_DISCLOSURE
    DISCLOSURE_REQUEST_NOT_FOUND(NOT_FOUND, 23001, "[StudentIdDisclosure] 학번 공개 요청을 찾을 수 없습니다."),
    DISCLOSURE_REQUEST_ALREADY_EXISTS(CONFLICT, 23002, "[StudentIdDisclosure] 이미 진행 중인 요청이 있습니다."),
    DISCLOSURE_REQUEST_FORBIDDEN(FORBIDDEN, 23003, "[StudentIdDisclosure] 권한이 없습니다."),
    DISCLOSURE_CANNOT_REQUEST_SELF(BAD_REQUEST, 23004, "[StudentIdDisclosure] 자기 자신에게 요청할 수 없습니다."),
    DISCLOSURE_NOT_IN_SAME_ROOM(BAD_REQUEST, 23005, "[StudentIdDisclosure] 같은 채팅방에 있는 사용자에게만 요청할 수 있습니다."),
    DISCLOSURE_INVALID_STATUS(BAD_REQUEST, 23006, "[StudentIdDisclosure] 현재 상태에서 허용되지 않는 요청입니다."),

    // PLACE (Kakao Local API)
    KAKAO_API_ERROR(BAD_GATEWAY, 24001, "[Place] 외부 장소 검색 서비스에 일시적인 문제가 있습니다."),
    KAKAO_QUOTA_EXCEEDED(SERVICE_UNAVAILABLE, 24002, "[Place] 일일 외부 API 호출 한도에 도달했습니다."),
    KEYWORD_TOO_SHORT(BAD_REQUEST, 24003, "[Place] 검색 키워드는 2자 이상이어야 합니다."),
    INVALID_PLACE_FOR_TYPE(BAD_REQUEST, 24004, "[Place] FOOD 타입에만 음식점 정보를 입력할 수 있습니다."),
    PLACE_ID_INVALID(BAD_REQUEST, 24005, "[Place] 유효하지 않은 음식점 ID입니다."),
    PLACE_MIGRATION_ALREADY_RUNNING(CONFLICT, 24006, "[Place] 마이그레이션이 이미 실행 중입니다."),

    // RATE LIMIT
    RATE_LIMIT_EXCEEDED(TOO_MANY_REQUESTS, 20001, "[RateLimit] 요청 횟수를 초과했습니다. 잠시 후 다시 시도해주세요."),

    // DISTRIBUTED LOCK
    GROUP_ORDER_VIEW_COUNT_LOCK_FAILED(SERVICE_UNAVAILABLE, 21001, "[GroupOrder] 조회수 업데이트 락 획득에 실패했습니다. 잠시 후 재시도됩니다."),

    // WEATHER
    WEATHER_API_ERROR(INTERNAL_SERVER_ERROR, 25001, "[Weather] 기상청 API 호출 중 오류가 발생했습니다."),
    WEATHER_INVALID_LOCATION(BAD_REQUEST, 25002, "[Weather] 대한민국 범위를 벗어난 위경도입니다."),

    // BLOCK
    USER_BLOCK_CANNOT_BLOCK_SELF(BAD_REQUEST, 26001, "[Block] 자기 자신을 차단할 수 없습니다."),
    USER_BLOCK_ALREADY_EXISTS(CONFLICT, 26002, "[Block] 이미 차단한 사용자입니다."),
    USER_BLOCKED_BY_TARGET(FORBIDDEN, 26003, "[Block] 상대방에 의해 차단된 사용자입니다."),
    USER_BLOCK_NOT_FOUND(NOT_FOUND, 26004, "[Block] 차단 기록이 존재하지 않습니다."),

    // SERVER
    UNHANDLED_EXCEPTION(INTERNAL_SERVER_ERROR, 99999, "[Server] 서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final Integer code;
    private final String message;
}
