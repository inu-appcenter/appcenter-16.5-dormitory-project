package com.example.appcenter_project.domain.openChat.entity;

import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatMessageDeleteEventDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatMessageDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatMessageEditEventDto;
import com.example.appcenter_project.domain.openChat.dto.response.ResponseOpenChatReadEventDto;
import com.example.appcenter_project.domain.openChat.dto.response.*;
import com.example.appcenter_project.domain.openChat.enums.OpenChatMessageType;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomRecruitmentStatus;
import com.example.appcenter_project.domain.roommate.dto.response.ResponseRoommateChatCreateEventDto;
import com.example.appcenter_project.domain.roommate.dto.response.ResponseRoommateChatReadEventDto;
import com.example.appcenter_project.domain.roommate.dto.response.ResponseRoommateChatDeleteEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;

class ChatEventSerializationTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void creationPayloadSeparatesReadStateAndPreservesSpecialMessageFields() {
        OpenChatMessage message = OpenChatMessage.create(7L, 2L, "request", OpenChatMessageType.STUDENT_ID_REQUEST);
        var request = ResponseOpenChatMessageCreateEventDto.fromStudentIdRequest(message, "sender", 42L);
        var json = mapper.valueToTree(request);
        assertThat(json.path("eventType").asText()).isEqualTo("STUDENT_ID_REQUEST");
        assertThat(json.path("disclosureRequestId").asLong()).isEqualTo(42L);
        assertThat(json.has("unreadCount")).isFalse();
        assertThat(json.has("isEdited")).isFalse();
        assertThat(json.has("isDeleted")).isFalse();
        assertThat(json.has("isBot")).isTrue();

        OpenChatMessage card = OpenChatMessage.create(7L, 2L, "card", OpenChatMessageType.REOPEN_CARD);
        var link = ResponseOpenChatMessageCreateEventDto.fromRoomLink(card, "sender", 8L, "room", "description", 10);
        assertThat(link.getEventType().name()).isEqualTo("ROOM_LINK_CREATED");
        assertThat(link.getType()).isEqualTo(OpenChatMessageType.REOPEN_CARD);
        assertThat(link.getLinkedRoomId()).isEqualTo(8L);
        assertThat(link.getLinkedRoomDescription()).isEqualTo("description");
    }

    @Test
    void eventTypeDistinguishesPayloadsOnSharedTopic() {
        Object[][] cases = {
                {ResponseOpenChatMessageCreateEventDto.builder().type(OpenChatMessageType.TEXT).build(), "MESSAGE_CREATED"},
                {ResponseRoommateChatCreateEventDto.builder().build(), "MESSAGE_CREATED"},
                {ResponseOpenChatMessageEditEventDto.builder().build(), "MESSAGE_UPDATED"},
                {new ResponseOpenChatMessageDeleteEventDto(10L, 7L), "MESSAGE_DELETED"},
                {new ResponseRoommateChatDeleteEventDto(10L, 7L), "MESSAGE_DELETED"},
                {ResponseOpenChatReadEventDto.of(10L, 0), "MESSAGE_READ"},
                {new ResponseRoommateChatReadEventDto(7L, 2L, List.of(10L)), "MESSAGE_READ"},
                {ResponseRecruitmentStatusEventDto.of(8L, OpenChatRoomRecruitmentStatus.CLOSED), "RECRUITMENT_STATUS_CHANGED"}
        };
        for (Object[] entry : cases) {
            assertThat(mapper.valueToTree(entry[0]).path("eventType").asText()).isEqualTo(entry[1]);
        }
        assertThat(mapper.valueToTree(cases[0][0]).path("type").asText()).isEqualTo("TEXT");
    }
}
