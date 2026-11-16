package com.example.appcenter_project.domain.openChat.service;

import com.example.appcenter_project.domain.openChat.entity.OpenChatRoom;
import com.example.appcenter_project.domain.openChat.enums.OpenChatRoomScope;
import com.example.appcenter_project.domain.openChat.fixture.OpenChatScopeInheritFixture;
import com.example.appcenter_project.domain.block.service.BlockService;
import com.example.appcenter_project.domain.openChat.repository.OpenChatMessageQuerydslRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatMessageRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatParticipantRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomQuerydslRepository;
import com.example.appcenter_project.domain.openChat.repository.OpenChatRoomRepository;
import com.example.appcenter_project.domain.roommate.repository.MyRoommateRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingChatRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingRoomRepository;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OpenChatRoomScopeInheritServiceTest {

    @Mock
    OpenChatRoomRepository openChatRoomRepository;

    @Mock
    OpenChatParticipantRepository openChatParticipantRepository;

    @Mock
    OpenChatMessageRepository openChatMessageRepository;

    @Mock
    OpenChatMessageQuerydslRepository openChatMessageQuerydslRepository;

    @Mock
    OpenChatRoomQuerydslRepository openChatRoomQuerydslRepository;

    @Mock
    OpenChatMessageService openChatMessageService;

    @Mock
    UserRepository userRepository;

    @Mock
    RoommateChattingRoomRepository roommateChattingRoomRepository;

    @Mock
    RoommateChattingChatRepository roommateChattingChatRepository;

    @Mock
    MyRoommateRepository myRoommateRepository;

    @Mock
    BlockService blockService;

    @Mock
    OpenChatNotificationService openChatNotificationService;

    @InjectMocks
    OpenChatRoomService openChatRoomService;

    @Test
    @DisplayName("scope=DORMITORY 상속 — BR-729 AC-1: 부모 scope=DORMITORY이면 파생 방도 scope=DORMITORY로 저장")
    void should_inherit_scope_DORMITORY_when_parent_scope_is_DORMITORY() {
        // given
        OpenChatRoom parentRoom = OpenChatScopeInheritFixture.createParentRoomWithScope(OpenChatRoomScope.DORMITORY);
        var request = OpenChatScopeInheritFixture.createDerivedRoomRequest(true);
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
        assertThat(captor.getValue().getScope()).isEqualTo(OpenChatRoomScope.DORMITORY);
    }

    @Test
    @DisplayName("scope=ALL 상속 — BR-729 AC-2: 부모 scope=ALL이면 파생 방도 scope=ALL로 저장")
    void should_inherit_scope_ALL_when_parent_scope_is_ALL() {
        // given
        OpenChatRoom parentRoom = OpenChatScopeInheritFixture.createParentRoomWithScope(OpenChatRoomScope.ALL);
        var request = OpenChatScopeInheritFixture.createDerivedRoomRequest(true);
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
        assertThat(captor.getValue().getScope()).isEqualTo(OpenChatRoomScope.ALL);
    }
}
