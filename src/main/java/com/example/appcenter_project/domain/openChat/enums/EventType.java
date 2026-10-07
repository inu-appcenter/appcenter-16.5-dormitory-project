package com.example.appcenter_project.domain.openChat.enums;

import lombok.Getter;

@Getter
public enum EventType {
    MESSAGE_CREATED, //메시지 생성
    MESSAGE_READ, //메시지 읽음
    MESSAGE_UPDATED, //메시지 수정
    MESSAGE_DELETED, //메시지 삭제
    RECRUITMENT_STATUS_CHANGED, //오픈채팅방 모집 상태 변경
    STUDENT_ID_REQUEST, //학번 요청
    ROOM_LINK_CREATED, //카드 생성
}
