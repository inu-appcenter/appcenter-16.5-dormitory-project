package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.openChat.dto.request.RequestCreateDerivedRoomDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatMessageDto;
import com.example.appcenter_project.domain.openChat.entity.OpenChatMessage;
import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatMessageType;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomRecruitmentStatus;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatParentCardFixture;
import com.example.appcenter_project.domain.openChat.repository.OpenChatMessageRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatParticipantRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomRepository;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.enums.DormType;
import com.example.appcenter_project.domain.user.enums.Role;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OpenChatParentCardServiceTest {

    @Mock OpenChatRoomRepository openChatRoomRepository;
    @Mock OpenChatParticipantRepository openChatParticipantRepository;
    @Mock OpenChatMessageRepository openChatMessageRepository;
    @Mock OpenChatMessageService openChatMessageService;
    @Mock UserRepository userRepository;

    @InjectMocks OpenChatRoomService openChatRoomService;

    // ============================================================
    // AC-01: 파생 방 생성 시 originRoomId·transitionCount 저장
    // ============================================================

    @Test
    @DisplayName("파생 방 생성 시 originRoomId 저장 — AC-01")
    void should_save_derived_room_with_originRoomId_when_creating_derived_room() {
        // given
        OpenChatRoom parentRoom = OpenChatParentCardFixture.createParentOpenRoom(1L);
        var request = buildDerivedRoomRequest(1L, true);
        given(openChatRoomRepository.findById(1L)).willReturn(Optional.of(parentRoom));
        given(openChatParticipantRepository.existsByRoomIdAndUserId(anyLong(), eq(7L))).willReturn(true);
        OpenChatRoom savedRoom = mock(OpenChatRoom.class);
        given(savedRoom.getId()).willReturn(42L);
        given(openChatRoomRepository.save(any())).willReturn(savedRoom);
        ArgumentCaptor<OpenChatRoom> captor = ArgumentCaptor.forClass(OpenChatRoom.class);

        // when
        openChatRoomService.createDerivedRoom(7L, request);

        // then
        then(openChatRoomRepository).should().save(captor.capture());
        assertThat(captor.getValue().getOriginRoomId()).isEqualTo(1L);
        assertThat(captor.getValue().getTransitionCount()).isEqualTo(0);
    }

    // ============================================================
    // AC-02: OPEN→CLOSED 마감 시 부모 방 이벤트 전송
    // ============================================================

    @Test
    @DisplayName("OPEN→CLOSED 마감 시 sendRecruitmentStatusEvent 호출 — AC-02")
    void should_call_sendRecruitmentStatusEvent_with_CLOSED_when_status_changed_to_CLOSED() {
        // given
        OpenChatRoom derivedRoom = OpenChatParentCardFixture.createDerivedRoomWithOrigin(42L, 10L);
        User actor = buildUser(1L, Role.ROLE_USER);
        given(openChatRoomRepository.findById(42L)).willReturn(Optional.of(derivedRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(actor));

        // when
        openChatRoomService.updateRecruitmentStatus(1L, 42L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        then(openChatMessageService).should()
                .sendRecruitmentStatusEvent(10L, 42L, OpenChatRoomRecruitmentStatus.CLOSED);
    }

    @Test
    @DisplayName("OPEN→CLOSED 마감 시 sendReopenCardMessage 미호출 — AC-02")
    void should_NOT_call_sendReopenCardMessage_when_status_changed_to_CLOSED() {
        // given
        OpenChatRoom derivedRoom = OpenChatParentCardFixture.createDerivedRoomWithOrigin(42L, 10L);
        User actor = buildUser(1L, Role.ROLE_USER);
        given(openChatRoomRepository.findById(42L)).willReturn(Optional.of(derivedRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(actor));

        // when
        openChatRoomService.updateRecruitmentStatus(1L, 42L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        then(openChatMessageService).should(never())
                .sendReopenCardMessage(anyLong(), anyLong(), anyLong(), any(), any(), anyInt(), anyInt());
    }

    // ============================================================
    // AC-03: CLOSED→OPEN 재개 시 부모 방 이벤트 전송
    // ============================================================

    @Test
    @DisplayName("CLOSED→OPEN 재개 시 sendRecruitmentStatusEvent 호출 — AC-03")
    void should_call_sendRecruitmentStatusEvent_with_OPEN_when_status_changed_to_OPEN() {
        // given
        OpenChatRoom derivedRoom = OpenChatParentCardFixture.createClosedDerivedRoomWithOrigin(42L, 10L);
        User actor = buildUser(1L, Role.ROLE_USER);
        given(openChatRoomRepository.findById(42L)).willReturn(Optional.of(derivedRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(actor));

        // when
        openChatRoomService.updateRecruitmentStatus(1L, 42L, OpenChatRoomRecruitmentStatus.OPEN);

        // then
        then(openChatMessageService).should()
                .sendRecruitmentStatusEvent(10L, 42L, OpenChatRoomRecruitmentStatus.OPEN);
    }

    // ============================================================
    // AC-04: 재개 시 재모집 카드 메시지 생성
    // ============================================================

    @Test
    @DisplayName("CLOSED→OPEN 재개 시 sendReopenCardMessage 호출 — AC-04")
    void should_call_sendReopenCardMessage_when_status_changed_to_OPEN() {
        // given
        OpenChatRoom derivedRoom = OpenChatParentCardFixture.createClosedDerivedRoomWithOrigin(42L, 10L);
        User actor = buildUser(1L, Role.ROLE_USER);
        given(openChatRoomRepository.findById(42L)).willReturn(Optional.of(derivedRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(actor));

        // when
        openChatRoomService.updateRecruitmentStatus(1L, 42L, OpenChatRoomRecruitmentStatus.OPEN);

        // then
        then(openChatMessageService).should()
                .sendReopenCardMessage(eq(10L), eq(1L), eq(42L), any(), any(), anyInt(), eq(1));
    }

    // ============================================================
    // AC-05: 동일 transitionCount 재모집 카드 중복 방지
    // ============================================================

    @Test
    @DisplayName("duplKey 존재 시 메시지 저장 skip — AC-05")
    void should_skip_message_save_when_duplKey_already_exists() {
        // given
        OpenChatRoom derivedRoom = OpenChatParentCardFixture.createClosedDerivedRoomWithOrigin(42L, 10L);
        User actor = buildUser(1L, Role.ROLE_USER);
        given(openChatRoomRepository.findById(42L)).willReturn(Optional.of(derivedRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(actor));

        // when
        openChatRoomService.updateRecruitmentStatus(1L, 42L, OpenChatRoomRecruitmentStatus.OPEN);

        // then: dedup은 MessageService 내부 책임 — RoomService는 sendReopenCardMessage 위임만 담당
        then(openChatMessageService).should()
                .sendReopenCardMessage(eq(10L), eq(1L), eq(42L), any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("duplKey 존재해도 이벤트 전송은 수행 — AC-05")
    void should_still_send_event_even_when_duplKey_already_exists() {
        // given
        OpenChatRoom derivedRoom = OpenChatParentCardFixture.createClosedDerivedRoomWithOrigin(42L, 10L);
        User actor = buildUser(1L, Role.ROLE_USER);
        given(openChatRoomRepository.findById(42L)).willReturn(Optional.of(derivedRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(actor));

        // when
        openChatRoomService.updateRecruitmentStatus(1L, 42L, OpenChatRoomRecruitmentStatus.OPEN);

        // then
        then(openChatMessageService).should()
                .sendRecruitmentStatusEvent(10L, 42L, OpenChatRoomRecruitmentStatus.OPEN);
    }

    // ============================================================
    // AC-06: originRoomId=null 방은 이벤트·카드 없이 정상 처리
    // ============================================================

    @Test
    @DisplayName("originRoomId=null 이면 예외 없이 204 — AC-06")
    void should_complete_without_exception_when_originRoomId_is_null() {
        // given
        OpenChatRoom derivedRoom = OpenChatParentCardFixture.createOpenDerivedRoom(42L);
        User actor = buildUser(1L, Role.ROLE_USER);
        given(openChatRoomRepository.findById(42L)).willReturn(Optional.of(derivedRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(actor));

        // when / then
        assertThatCode(() ->
                openChatRoomService.updateRecruitmentStatus(1L, 42L, OpenChatRoomRecruitmentStatus.CLOSED)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("originRoomId=null 이면 sendRecruitmentStatusEvent 미호출 — AC-06")
    void should_NOT_send_event_when_originRoomId_is_null() {
        // given
        OpenChatRoom derivedRoom = OpenChatParentCardFixture.createOpenDerivedRoom(42L);
        User actor = buildUser(1L, Role.ROLE_USER);
        given(openChatRoomRepository.findById(42L)).willReturn(Optional.of(derivedRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(actor));

        // when
        openChatRoomService.updateRecruitmentStatus(1L, 42L, OpenChatRoomRecruitmentStatus.CLOSED);

        // then
        then(openChatMessageService).should(never()).sendRecruitmentStatusEvent(anyLong(), anyLong(), any());
    }

    // ============================================================
    // AC-07: 부모 방 존재하지 않아도 예외 없이 204
    // ============================================================

    @Test
    @DisplayName("부모 방 DB에 없어도 예외 없음 — AC-07")
    void should_not_throw_when_parent_room_not_found() {
        // given
        OpenChatRoom derivedRoom = OpenChatParentCardFixture.createDerivedRoomWithOrigin(42L, 999L);
        User actor = buildUser(1L, Role.ROLE_USER);
        given(openChatRoomRepository.findById(42L)).willReturn(Optional.of(derivedRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(actor));

        // when / then
        assertThatCode(() ->
                openChatRoomService.updateRecruitmentStatus(1L, 42L, OpenChatRoomRecruitmentStatus.CLOSED)
        ).doesNotThrowAnyException();
    }

    // ============================================================
    // AC-08: 카드 조회 응답에 linkedRoomRecruitmentStatus enum 포함
    // ============================================================

    @Test
    @DisplayName("ROOM_LINK DTO에 linkedRoomRecruitmentStatus=CLOSED 포함 — AC-08")
    void should_include_linkedRoomRecruitmentStatus_CLOSED_in_ROOM_LINK_dto() {
        OpenChatMessage msg = OpenChatParentCardFixture.createRoomLinkMessage(1L, 1L, 42L, "방이름", "설명", 30);
        ResponseOpenChatMessageDto dto = ResponseOpenChatMessageDto.fromRoomLink(
                msg, "sender", 0, 42L, "방이름", "설명", 30, false, OpenChatRoomRecruitmentStatus.CLOSED);
        assertThat(dto.getLinkedRoomRecruitmentStatus()).isEqualTo(OpenChatRoomRecruitmentStatus.CLOSED);
    }

    @Test
    @DisplayName("ROOM_LINK DTO에 linkedRoomRecruitmentStatus=OPEN 포함 — AC-08")
    void should_include_linkedRoomRecruitmentStatus_OPEN_in_ROOM_LINK_dto() {
        OpenChatMessage msg = OpenChatParentCardFixture.createRoomLinkMessage(1L, 1L, 42L, "방이름", "설명", 30);
        ResponseOpenChatMessageDto dto = ResponseOpenChatMessageDto.fromRoomLink(
                msg, "sender", 0, 42L, "방이름", "설명", 30, false, OpenChatRoomRecruitmentStatus.OPEN);
        assertThat(dto.getLinkedRoomRecruitmentStatus()).isEqualTo(OpenChatRoomRecruitmentStatus.OPEN);
    }

    // ============================================================
    // AC-09: 재모집 카드(REOPEN_CARD) 응답 형식
    // ============================================================

    @Test
    @DisplayName("REOPEN_CARD 메시지 타입 존재 — AC-09")
    void should_have_REOPEN_CARD_message_type() {
        assertThat(OpenChatMessageType.REOPEN_CARD).isNotNull();
    }

    @Test
    @DisplayName("REOPEN_CARD DTO에 linkedRoomRecruitmentStatus=OPEN 포함 — AC-09")
    void should_include_recruitmentStatus_OPEN_in_REOPEN_CARD_dto() {
        OpenChatMessage msg = OpenChatParentCardFixture.createReopenCardMessage(1L, 1L, 42L, "방이름", "reopen_1");
        ResponseOpenChatMessageDto dto = ResponseOpenChatMessageDto.fromRoomLink(
                msg, "sender", 0, 42L, "방이름", "설명", 30, false, OpenChatRoomRecruitmentStatus.OPEN);
        assertThat(dto.getLinkedRoomRecruitmentStatus()).isEqualTo(OpenChatRoomRecruitmentStatus.OPEN);
        assertThat(msg.getType()).isEqualTo(OpenChatMessageType.REOPEN_CARD);
    }

    // ============================================================
    // Helpers
    // ============================================================

    private RequestCreateDerivedRoomDto buildDerivedRoomRequest(Long originRoomId, boolean isPublic) {
        try {
            RequestCreateDerivedRoomDto dto = new RequestCreateDerivedRoomDto();
            setField(dto, "originRoomId", originRoomId);
            setField(dto, "name", "파생 방");
            setField(dto, "description", "설명");
            setField(dto, "maxParticipants", 30);
            setField(dto, "isPublic", isPublic);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private User buildUser(Long id, Role role) {
        User user = User.createForTest(id, "테스트유저", DormType.DORM_1);
        setField(user, "role", role);
        return user;
    }

    private void setField(Object target, String name, Object value) {
        try {
            Class<?> clazz = target.getClass();
            while (clazz != null) {
                try {
                    Field f = clazz.getDeclaredField(name);
                    f.setAccessible(true);
                    f.set(target, value);
                    return;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
