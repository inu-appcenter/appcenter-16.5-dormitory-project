package com.example.appcenter_project.domain.tip.service;

import com.example.appcenter_project.common.image.enums.ImageType;
import com.example.appcenter_project.common.image.service.ImageService;
import com.example.appcenter_project.global.mixpanel.MixpanelService;
import com.example.appcenter_project.domain.tip.dto.request.RequestTipDto;
import com.example.appcenter_project.domain.tip.dto.response.ResponseTipDto;
import com.example.appcenter_project.domain.tip.entity.Tip;
import com.example.appcenter_project.domain.tip.entity.TipLike;
import com.example.appcenter_project.domain.tip.repository.TipCommentRepository;
import com.example.appcenter_project.domain.tip.repository.TipLikeRepository;
import com.example.appcenter_project.domain.tip.repository.TipRepository;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static com.example.appcenter_project.global.exception.ErrorCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TipServiceTest {

    @Mock private TipRepository tipRepository;
    @Mock private UserRepository userRepository;
    @Mock private TipCommentRepository tipCommentRepository;
    @Mock private TipLikeRepository tipLikeRepository;
    @Mock private ImageService imageService;
    @Mock private MixpanelService mixpanelService;

    @InjectMocks
    private TipService tipService;

    // ===== saveTip =====

    @Test
    @DisplayName("팁 저장 - 이미지 포함 정상 저장")
    void saveTip_이미지_포함_정상_저장() {
        User mockUser = mock(User.class);
        Tip mockTip = mock(Tip.class);
        when(mockTip.getId()).thenReturn(1L);

        RequestTipDto requestDto = mock(RequestTipDto.class);
        when(requestDto.getTitle()).thenReturn("팁 제목");
        when(requestDto.getContent()).thenReturn("팁 내용");
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(tipRepository.save(any(Tip.class))).thenReturn(mockTip);

        List<MultipartFile> images = List.of(mock(MultipartFile.class));
        tipService.saveTip(1L, requestDto, images);

        verify(tipRepository).save(any(Tip.class));
        verify(imageService).saveImages(eq(ImageType.TIP), any(), eq(images));
    }

    @Test
    @DisplayName("팁 저장 - 이미지 없으면 이미지 저장 스킵")
    void saveTip_이미지_없으면_저장_스킵() {
        User mockUser = mock(User.class);
        Tip mockTip = mock(Tip.class);
        when(mockTip.getId()).thenReturn(1L);

        RequestTipDto requestDto = mock(RequestTipDto.class);
        when(requestDto.getTitle()).thenReturn("팁 제목");
        when(requestDto.getContent()).thenReturn("팁 내용");
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(tipRepository.save(any(Tip.class))).thenReturn(mockTip);

        tipService.saveTip(1L, requestDto, null);

        verify(tipRepository).save(any(Tip.class));
        verifyNoInteractions(imageService);
    }

    @Test
    @DisplayName("팁 저장 - 신규 저장 성공 시 완료 이벤트 발송")
    void saveTip_신규저장_완료이벤트_발송() {
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(1L);
        RequestTipDto requestDto = mock(RequestTipDto.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        tipService.saveTip(1L, requestDto, null);

        verify(mixpanelService).trackEvent(eq("1"), eq("게시글 작성 완료"), any());
    }

    @Test
    @DisplayName("팁 저장 - 유저 없으면 완료 이벤트 미발송")
    void saveTip_유저없으면_완료이벤트_미발송() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipService.saveTip(99L, mock(RequestTipDto.class), null))
                .isInstanceOf(CustomException.class);

        verify(mixpanelService, never()).trackEvent(any(), eq("게시글 작성 완료"), any());
    }

    @Test
    @DisplayName("팁 저장 - 유저 없으면 예외")
    void saveTip_유저_없으면_예외() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipService.saveTip(99L, mock(RequestTipDto.class), null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", USER_NOT_FOUND);
    }

    // ===== findAllTips =====

    @Test
    @DisplayName("팁 전체 조회 - 최신순 반환")
    void findAllTips_최신순_반환() {
        User mockUser = mock(User.class);
        Tip tip1 = Tip.createTip("제목1", "내용1", mockUser);
        Tip tip2 = Tip.createTip("제목2", "내용2", mockUser);
        when(tipRepository.findAllByOrderByIdDesc()).thenReturn(List.of(tip1, tip2));

        List<ResponseTipDto> result = tipService.findAllTips();

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("팁 전체 조회 - 없으면 빈 리스트 반환")
    void findAllTips_없으면_빈_리스트() {
        when(tipRepository.findAllByOrderByIdDesc()).thenReturn(List.of());

        List<ResponseTipDto> result = tipService.findAllTips();

        assertThat(result).isEmpty();
    }

    // ===== findDailyRandomTips =====

    @Test
    @DisplayName("랜덤 팁 조회 - 3개 미만이면 빈 리스트 반환")
    void findDailyRandomTips_3개_미만이면_빈_리스트() {
        when(tipRepository.findAllTipIds()).thenReturn(List.of(1L, 2L));

        List<ResponseTipDto> result = tipService.findDailyRandomTips();

        assertThat(result).isEmpty();
        verify(tipRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("랜덤 팁 조회 - 3개 이상이면 3개 선택")
    void findDailyRandomTips_3개_이상이면_정상_반환() {
        when(tipRepository.findAllTipIds()).thenReturn(List.of(1L, 2L, 3L, 4L, 5L));
        User mockUser = mock(User.class);
        List<Tip> tips = List.of(
                Tip.createTip("제목1", "내용1", mockUser),
                Tip.createTip("제목2", "내용2", mockUser),
                Tip.createTip("제목3", "내용3", mockUser)
        );
        when(tipRepository.findAllById(any())).thenReturn(tips);

        List<ResponseTipDto> result = tipService.findDailyRandomTips();

        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("랜덤 팁 조회 - 같은 날 동일한 ID 목록 선택 (시드 고정)")
    void findDailyRandomTips_동일한_날_동일한_결과() {
        when(tipRepository.findAllTipIds()).thenReturn(List.of(1L, 2L, 3L, 4L, 5L));
        when(tipRepository.findAllById(any())).thenReturn(List.of());

        tipService.findDailyRandomTips();
        tipService.findDailyRandomTips();

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(tipRepository, times(2)).findAllById(captor.capture());
        assertThat(captor.getAllValues().get(0)).isEqualTo(captor.getAllValues().get(1));
    }

    // ===== findTipImages =====

    @Test
    @DisplayName("팁 이미지 조회 - 팁 없으면 예외")
    void findTipImages_팁_없으면_예외() {
        when(tipRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> tipService.findTipImages(99L, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", TIP_NOT_FOUND);
    }

    // ===== likeTip =====

    @Test
    @DisplayName("팁 좋아요 - 정상 증가")
    void likeTip_정상_증가() {
        User mockUser = mock(User.class);
        Tip mockTip = mock(Tip.class);
        when(mockTip.isLikedBy(mockUser)).thenReturn(false);
        when(mockTip.increaseLike()).thenReturn(1);

        when(tipRepository.findById(1L)).thenReturn(Optional.of(mockTip));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        Integer result = tipService.likeTip(1L, 1L);

        assertThat(result).isEqualTo(1);
        verify(tipLikeRepository).save(any(TipLike.class));
    }

    @Test
    @DisplayName("팁 좋아요 - 이미 눌렀으면 예외")
    void likeTip_이미_눌렀으면_예외() {
        User mockUser = mock(User.class);
        Tip mockTip = mock(Tip.class);
        when(mockTip.isLikedBy(mockUser)).thenReturn(true);

        when(tipRepository.findById(1L)).thenReturn(Optional.of(mockTip));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> tipService.likeTip(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ALREADY_TIP_LIKE_USER);
    }

    @Test
    @DisplayName("팁 좋아요 - 팁 없으면 예외")
    void likeTip_팁_없으면_예외() {
        when(tipRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipService.likeTip(1L, 99L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", TIP_NOT_FOUND);
    }

    // ===== unlikeTip =====

    @Test
    @DisplayName("팁 좋아요 취소 - 정상 감소")
    void unlikeTip_정상_감소() {
        User mockUser = mock(User.class);
        Tip mockTip = mock(Tip.class);
        TipLike mockLike = mock(TipLike.class);

        when(mockTip.isLikedBy(mockUser)).thenReturn(true);
        when(mockTip.decreaseLike()).thenReturn(0);
        when(tipRepository.findById(1L)).thenReturn(Optional.of(mockTip));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(tipLikeRepository.findByUserAndTip(mockUser, mockTip)).thenReturn(Optional.of(mockLike));

        Integer result = tipService.unlikeTip(1L, 1L);

        assertThat(result).isEqualTo(0);
        verify(tipLikeRepository).delete(mockLike);
    }

    @Test
    @DisplayName("팁 좋아요 취소 - 좋아요 안 눌렀으면 예외")
    void unlikeTip_좋아요_안_눌렀으면_예외() {
        User mockUser = mock(User.class);
        Tip mockTip = mock(Tip.class);
        when(mockTip.isLikedBy(mockUser)).thenReturn(false);

        when(tipRepository.findById(1L)).thenReturn(Optional.of(mockTip));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> tipService.unlikeTip(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", NOT_LIKED_TIP);
    }

    // ===== updateTip =====

    @Test
    @DisplayName("팁 수정 - 정상 수정 및 이미지 업데이트")
    void updateTip_정상_수정() {
        RequestTipDto requestDto = mock(RequestTipDto.class);
        Tip mockTip = mock(Tip.class);
        when(tipRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockTip));

        tipService.updateTip(1L, requestDto, null, 1L);

        verify(mockTip).update(requestDto);
        verify(imageService).updateImages(eq(ImageType.TIP), eq(1L), any());
    }

    @Test
    @DisplayName("팁 수정 - 본인 팁 아니면 예외")
    void updateTip_본인_팁_아니면_예외() {
        when(tipRepository.findByIdAndUserId(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipService.updateTip(2L, mock(RequestTipDto.class), null, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", TIP_NOT_OWNED_BY_USER);
    }

    // ===== deleteTip =====

    @Test
    @DisplayName("팁 삭제 - 정상 삭제 및 이미지 삭제")
    void deleteTip_정상_삭제() {
        when(tipRepository.existsByIdAndUserId(1L, 1L)).thenReturn(true);

        tipService.deleteTip(1L, 1L);

        verify(tipRepository).deleteById(1L);
        verify(imageService).deleteImages(ImageType.TIP, 1L);
    }

    @Test
    @DisplayName("팁 삭제 - 본인 팁 아니면 예외")
    void deleteTip_본인_팁_아니면_예외() {
        when(tipRepository.existsByIdAndUserId(1L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> tipService.deleteTip(2L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", TIP_NOT_OWNED_BY_USER);
    }
}
