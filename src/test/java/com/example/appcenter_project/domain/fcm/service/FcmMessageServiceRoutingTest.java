package com.example.appcenter_project.domain.fcm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.appcenter_project.domain.fcm.entity.FcmOutbox;
import com.example.appcenter_project.domain.fcm.entity.FcmToken;
import com.example.appcenter_project.domain.fcm.enums.FcmRoutingType;
import com.example.appcenter_project.domain.fcm.repository.FcmOutboxRepository;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.repository.FcmTokenRepository;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmMessageService routing 오버로드 (BR-792)")
class FcmMessageServiceRoutingTest {

    @Mock FcmTokenRepository fcmTokenRepository;
    @Mock UserRepository userRepository;
    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock FcmOutboxRepository fcmOutboxRepository;

    @InjectMocks FcmMessageService fcmMessageService;

    @Test
    @DisplayName("routing 지정 시 outbox에 routingType·routingId가 저장된다")
    void should_save_outbox_with_routing_when_routing_given() {
        // given
        User user = User.createForTest(21L, "유저");
        FcmToken token = new FcmToken(user, "token-1");
        given(userRepository.findById(21L)).willReturn(Optional.of(user));
        given(fcmTokenRepository.findAllByUser(user)).willReturn(List.of(token));

        // when
        fcmMessageService.sendNotification(user, "제목", "내용", FcmRoutingType.ANNOUNCEMENT, 77L);

        // then
        ArgumentCaptor<FcmOutbox> captor = ArgumentCaptor.forClass(FcmOutbox.class);
        verify(fcmOutboxRepository).save(captor.capture());
        FcmOutbox saved = captor.getValue();
        assertThat(saved.getRoutingType()).isEqualTo(FcmRoutingType.ANNOUNCEMENT);
        assertThat(saved.getRoutingId()).isEqualTo(77L);
        assertThat(saved.getUserId()).isEqualTo(21L);
    }
}
