package com.example.appcenter_project.domain.groupOrder.service;

import com.example.appcenter_project.common.image.entity.Image;
import com.example.appcenter_project.common.image.enums.ImageType;
import com.example.appcenter_project.common.image.repository.ImageRepository;
import com.example.appcenter_project.common.image.service.ImageService;
import com.example.appcenter_project.domain.groupOrder.dto.request.RequestGroupOrderDto;
import com.example.appcenter_project.domain.groupOrder.dto.response.GroupOrderListProjection;
import com.example.appcenter_project.domain.groupOrder.dto.response.ResponseGroupOrderDetailDto;
import com.example.appcenter_project.domain.groupOrder.dto.response.ResponseGroupOrderDto;
import com.example.appcenter_project.domain.groupOrder.dto.response.ResponseGroupOrderPopularSearch;
import com.example.appcenter_project.domain.groupOrder.entity.GroupOrder;
import com.example.appcenter_project.domain.groupOrder.entity.GroupOrderComment;
import com.example.appcenter_project.domain.groupOrder.entity.GroupOrderLike;
import com.example.appcenter_project.domain.groupOrder.entity.GroupOrderPopularSearchKeyword;
import com.example.appcenter_project.domain.groupOrder.enums.GroupOrderSort;
import com.example.appcenter_project.domain.groupOrder.enums.GroupOrderType;
import com.example.appcenter_project.domain.groupOrder.repository.*;
import com.example.appcenter_project.domain.place.service.PlaceResolver;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.mixpanel.MixpanelService;
import com.example.appcenter_project.global.scheduler.MealTimeChecker;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.example.appcenter_project.global.exception.ErrorCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroupOrderServiceTest {

    @Mock private GroupOrderRepository groupOrderRepository;
    @Mock private UserRepository userRepository;
    @Mock private GroupOrderLikeRepository groupOrderLikeRepository;
    @Mock private GroupOrderCommentRepository groupOrderCommentRepository;
    @Mock private ImageRepository imageRepository;
    @Mock private GroupOrderPopularSearchKeywordRepository groupOrderPopularSearchKeywordRepository;
    @Mock private ImageService imageService;
    @Mock private AsyncViewCountService asyncViewCountService;
    @Mock private MealTimeChecker mealTimeChecker;
    @Mock private GroupOrderNotificationService groupOrderNotificationService;
    @Mock private GroupOrderCreateService groupOrderCreateService;
    @Mock private MixpanelService mixpanelService;
    @Mock private HttpServletRequest httpServletRequest;

    @InjectMocks
    private GroupOrderService groupOrderService;

    @BeforeEach
    void setUp() {
        groupOrderService = new GroupOrderService(
                groupOrderRepository, userRepository, groupOrderLikeRepository,
                groupOrderCommentRepository, imageRepository,
                groupOrderPopularSearchKeywordRepository, imageService,
                asyncViewCountService, mealTimeChecker, groupOrderNotificationService,
                null, groupOrderCreateService, mixpanelService);
    }

    // ===== findGroupOrders - N+1 해소 검증 =====

    @Test
    @DisplayName("목록 조회 - 이미지 batch IN 쿼리 1회만 호출 (N+1 해소)")
    void findGroupOrders_이미지_batch_조회_1회() {
        GroupOrderListProjection g1 = mock(GroupOrderListProjection.class);
        GroupOrderListProjection g2 = mock(GroupOrderListProjection.class);
        GroupOrderListProjection g3 = mock(GroupOrderListProjection.class);
        when(g1.getId()).thenReturn(1L);
        when(g2.getId()).thenReturn(2L);
        when(g3.getId()).thenReturn(3L);
        when(g1.isRecruitmentComplete()).thenReturn(false);
        when(g2.isRecruitmentComplete()).thenReturn(false);
        when(g3.isRecruitmentComplete()).thenReturn(false);
        when(g1.getDeadline()).thenReturn(LocalDateTime.now().plusDays(1));
        when(g2.getDeadline()).thenReturn(LocalDateTime.now().plusDays(1));
        when(g3.getDeadline()).thenReturn(LocalDateTime.now().plusDays(1));

        when(groupOrderRepository.findGroupOrdersComplex(any(), any(), any(), any()))
                .thenReturn(List.of(g1, g2, g3));
        when(imageRepository.findGroupOrderImagesByEntityIds(anyList()))
                .thenReturn(Collections.emptyList());

        List<ResponseGroupOrderDto> result = groupOrderService.findGroupOrders(
                null, GroupOrderSort.LATEST, GroupOrderType.ALL, null, 0, 20, httpServletRequest);

        assertThat(result).hasSize(3);
        // 핵심: 이미지 조회가 게시글 수(3)번이 아닌 1번만 호출되어야 함
        verify(imageRepository, times(1)).findGroupOrderImagesByEntityIds(anyList());
        verify(imageService, never()).findImages(any(), anyLong(), any());
    }

    @Test
    @DisplayName("목록 조회 - 이미지 Map으로 게시글별 올바르게 매핑")
    void findGroupOrders_이미지_Map_매핑_정확() {
        GroupOrderListProjection g1 = mock(GroupOrderListProjection.class);
        GroupOrderListProjection g2 = mock(GroupOrderListProjection.class);
        when(g1.getId()).thenReturn(1L);
        when(g2.getId()).thenReturn(2L);
        when(g1.isRecruitmentComplete()).thenReturn(false);
        when(g2.isRecruitmentComplete()).thenReturn(false);
        when(g1.getDeadline()).thenReturn(LocalDateTime.now().plusDays(1));
        when(g2.getDeadline()).thenReturn(LocalDateTime.now().plusDays(1));

        Image image1 = mock(Image.class);
        when(image1.getEntityId()).thenReturn(1L);

        when(groupOrderRepository.findGroupOrdersComplex(any(), any(), any(), any()))
                .thenReturn(List.of(g1, g2));
        when(imageRepository.findGroupOrderImagesByEntityIds(List.of(1L, 2L)))
                .thenReturn(List.of(image1));
        when(imageService.getImageUrl(eq(ImageType.GROUP_ORDER), eq(image1), any()))
                .thenReturn("http://example.com/image1.jpg");

        List<ResponseGroupOrderDto> result = groupOrderService.findGroupOrders(
                null, GroupOrderSort.LATEST, GroupOrderType.ALL, null, 0, 20, httpServletRequest);

        assertThat(result).hasSize(2);
        verify(imageService, times(1)).getImageUrl(eq(ImageType.GROUP_ORDER), eq(image1), any());
        // g2는 이미지 없으므로 getImageUrl 호출 안 됨 — 총 1회
        verify(imageService, times(1)).getImageUrl(any(), any(), any());
    }

    @Test
    @DisplayName("목록 조회 - 게시글 없으면 빈 리스트 반환")
    void findGroupOrders_빈_목록_반환() {
        when(groupOrderRepository.findGroupOrdersComplex(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        List<ResponseGroupOrderDto> result = groupOrderService.findGroupOrders(
                null, GroupOrderSort.LATEST, GroupOrderType.ALL, null, 0, 20, httpServletRequest);

        assertThat(result).isEmpty();
        verify(imageRepository, never()).findGroupOrderImagesByEntityIds(any());
    }

    @Test
    @DisplayName("상세 조회 - 댓글 작성자 이미지 batch 조회 1회 (N+1 해소)")
    void findGroupOrder_댓글_작성자_이미지_batch_조회_1회() {
        User author1 = mock(User.class);
        User author2 = mock(User.class);
        when(author1.getId()).thenReturn(10L);
        when(author2.getId()).thenReturn(20L);
        when(author1.getName()).thenReturn("유저1");
        when(author2.getName()).thenReturn("유저2");

        GroupOrderComment comment1 = mock(GroupOrderComment.class);
        GroupOrderComment comment2 = mock(GroupOrderComment.class);
        when(comment1.isDeleted()).thenReturn(false);
        when(comment2.isDeleted()).thenReturn(false);
        when(comment1.getUser()).thenReturn(author1);
        when(comment2.getUser()).thenReturn(author2);
        when(comment1.getParentGroupOrderComment()).thenReturn(null);
        when(comment2.getParentGroupOrderComment()).thenReturn(null);

        User writer = mock(User.class);
        when(writer.getId()).thenReturn(1L);
        when(writer.getName()).thenReturn("작성자");

        GroupOrder groupOrder = mock(GroupOrder.class);
        when(groupOrder.getId()).thenReturn(1L);
        when(groupOrder.getUser()).thenReturn(writer);
        when(groupOrder.isRecruitmentComplete()).thenReturn(false);
        when(groupOrder.getDeadline()).thenReturn(LocalDateTime.now().plusDays(1));

        when(mealTimeChecker.isMealTime()).thenReturn(true);
        when(groupOrderRepository.findById(1L)).thenReturn(Optional.of(groupOrder));
        when(groupOrderCommentRepository.findByGroupOrder_Id(1L))
                .thenReturn(List.of(comment1, comment2));
        when(imageRepository.findByImageTypeAndEntityIdIn(eq(ImageType.USER), anyList()))
                .thenReturn(Collections.emptyList());

        groupOrderService.findGroupOrder(null, 1L, httpServletRequest);

        // 핵심: 댓글 작성자 이미지 조회가 댓글 수(2)번이 아닌 1번만 호출되어야 함
        verify(imageRepository, times(1)).findByImageTypeAndEntityIdIn(eq(ImageType.USER), anyList());
        verify(imageService, never()).findStaticImageUrl(any(), anyLong(), any());
    }

    // ===== saveGroupOrder =====

    @Test
    @DisplayName("공동구매 저장 - 정상 저장 및 알림 발송")
    void saveGroupOrder_정상_저장() {
        RequestGroupOrderDto requestDto = mock(RequestGroupOrderDto.class);

        groupOrderService.saveGroupOrder(1L, requestDto, null);

        verify(groupOrderCreateService).createAndPersist(eq(1L), eq(requestDto), any(), any(), any());
    }

    @Test
    @DisplayName("공동구매 저장 - 유저 없으면 예외")
    void saveGroupOrder_유저_없으면_예외() {
        doThrow(new CustomException(USER_NOT_FOUND))
                .when(groupOrderCreateService).createAndPersist(eq(99L), any(), any(), any(), any());

        assertThatThrownBy(() -> groupOrderService.saveGroupOrder(99L, mock(RequestGroupOrderDto.class), null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", USER_NOT_FOUND);
    }

    // ===== addRating =====

    @Test
    @DisplayName("평점 추가 - 게시글 작성자에 평점 반영")
    void addRating_정상_반영() {
        User mockUser = mock(User.class);
        GroupOrder mockGroupOrder = mock(GroupOrder.class);
        when(mockGroupOrder.getUser()).thenReturn(mockUser);
        when(groupOrderRepository.findById(1L)).thenReturn(Optional.of(mockGroupOrder));

        groupOrderService.addRating(1L, 4.5f);

        verify(mockUser).addRating(4.5f);
    }

    @Test
    @DisplayName("평점 추가 - 공동구매 없으면 예외")
    void addRating_공동구매_없으면_예외() {
        when(groupOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupOrderService.addRating(99L, 4.5f))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GROUP_ORDER_NOT_FOUND);
    }

    // ===== findGroupOrder =====

    @Test
    @DisplayName("공동구매 조회 - 식사 시간에는 비동기 조회수 증가")
    void findGroupOrder_식사시간_비동기_조회수_증가() {
        User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn("테스트유저");

        GroupOrder mockGroupOrder = mock(GroupOrder.class);
        when(mockGroupOrder.getId()).thenReturn(1L);
        when(mockGroupOrder.getUser()).thenReturn(mockUser);
        when(mockGroupOrder.isRecruitmentComplete()).thenReturn(false);
        when(mockGroupOrder.getDeadline()).thenReturn(LocalDateTime.now().plusDays(1));

        when(mealTimeChecker.isMealTime()).thenReturn(true);
        when(groupOrderRepository.findById(1L)).thenReturn(Optional.of(mockGroupOrder));
        when(groupOrderCommentRepository.findByGroupOrder_Id(1L))
                .thenReturn(new ArrayList<>());

        ResponseGroupOrderDetailDto result = groupOrderService.findGroupOrder(null, 1L, httpServletRequest);

        verify(asyncViewCountService).incrementViewCount(1L);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("공동구매 조회 - 비식사 시간에는 비관적 락 조회수 증가")
    void findGroupOrder_비식사시간_락_조회수_증가() {
        User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn("테스트유저");

        GroupOrder mockGroupOrder = mock(GroupOrder.class);
        when(mockGroupOrder.getId()).thenReturn(1L);
        when(mockGroupOrder.getUser()).thenReturn(mockUser);
        when(mockGroupOrder.isRecruitmentComplete()).thenReturn(false);
        when(mockGroupOrder.getDeadline()).thenReturn(LocalDateTime.now().plusDays(1));

        when(mealTimeChecker.isMealTime()).thenReturn(false);
        when(groupOrderRepository.findByIdWithLock(1L)).thenReturn(Optional.of(mockGroupOrder));
        when(groupOrderRepository.findById(1L)).thenReturn(Optional.of(mockGroupOrder));
        when(groupOrderCommentRepository.findByGroupOrder_Id(1L))
                .thenReturn(new ArrayList<>());

        groupOrderService.findGroupOrder(null, 1L, httpServletRequest);

        verify(groupOrderRepository).findByIdWithLock(1L);
        verify(mockGroupOrder).plusViewCount();
    }

    @Test
    @DisplayName("공동구매 조회 - 존재하지 않으면 예외")
    void findGroupOrder_없으면_예외() {
        when(mealTimeChecker.isMealTime()).thenReturn(true);
        when(groupOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupOrderService.findGroupOrder(null, 99L, httpServletRequest))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GROUP_ORDER_NOT_FOUND);
    }

    // ===== findGroupOrderSearchLog =====

    @Test
    @DisplayName("검색 기록 조회 - 정상 반환")
    void findGroupOrderSearchLog_정상_반환() {
        User mockUser = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(mockUser.getSearchLogs()).thenReturn(List.of("치킨", "피자"));

        List<String> result = groupOrderService.findGroupOrderSearchLog(1L);

        assertThat(result).containsExactly("치킨", "피자");
    }

    // ===== findGroupOrderPopularSearch =====

    @Test
    @DisplayName("인기 검색어 조회 - 순위와 함께 반환")
    void findGroupOrderPopularSearch_순위_포함_반환() {
        GroupOrderPopularSearchKeyword kw1 = mock(GroupOrderPopularSearchKeyword.class);
        GroupOrderPopularSearchKeyword kw2 = mock(GroupOrderPopularSearchKeyword.class);
        when(kw1.getKeyword()).thenReturn("치킨");
        when(kw2.getKeyword()).thenReturn("피자");
        when(groupOrderPopularSearchKeywordRepository.findTopKeywords(10))
                .thenReturn(List.of(kw1, kw2));

        List<ResponseGroupOrderPopularSearch> result = groupOrderService.findGroupOrderPopularSearch();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRanking()).isEqualTo(1);
        assertThat(result.get(0).getKeyword()).isEqualTo("치킨");
        assertThat(result.get(1).getRanking()).isEqualTo(2);
    }

    // ===== likeGroupOrder =====

    @Test
    @DisplayName("공동구매 좋아요 - 정상 증가")
    void likeGroupOrder_정상_증가() {
        User mockUser = mock(User.class);
        GroupOrder mockGroupOrder = mock(GroupOrder.class);
        when(mockGroupOrder.isLikedBy(mockUser)).thenReturn(false);
        when(mockGroupOrder.increaseLike()).thenReturn(1);
        when(mockGroupOrder.getGroupOrderLikeList()).thenReturn(new ArrayList<>());

        when(groupOrderRepository.findById(1L)).thenReturn(Optional.of(mockGroupOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        Integer result = groupOrderService.likeGroupOrder(1L, 1L);

        assertThat(result).isEqualTo(1);
        verify(groupOrderLikeRepository).save(any(GroupOrderLike.class));
    }

    @Test
    @DisplayName("공동구매 좋아요 - 이미 눌렀으면 예외")
    void likeGroupOrder_이미_눌렀으면_예외() {
        User mockUser = mock(User.class);
        GroupOrder mockGroupOrder = mock(GroupOrder.class);
        when(mockGroupOrder.isLikedBy(mockUser)).thenReturn(true);

        when(groupOrderRepository.findById(1L)).thenReturn(Optional.of(mockGroupOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> groupOrderService.likeGroupOrder(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ALREADY_GROUP_ORDER_LIKE_USER);
    }

    @Test
    @DisplayName("공동구매 좋아요 - 공동구매 없으면 예외")
    void likeGroupOrder_공동구매_없으면_예외() {
        when(groupOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupOrderService.likeGroupOrder(1L, 99L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GROUP_ORDER_NOT_FOUND);
    }

    // ===== unlikeGroupOrder =====

    @Test
    @DisplayName("공동구매 좋아요 취소 - 정상 감소")
    void unlikeGroupOrder_정상_감소() {
        User mockUser = mock(User.class);
        GroupOrder mockGroupOrder = mock(GroupOrder.class);
        GroupOrderLike mockLike = mock(GroupOrderLike.class);

        when(mockGroupOrder.isLikedBy(mockUser)).thenReturn(true);
        when(mockGroupOrder.decreaseLike()).thenReturn(0);
        when(groupOrderRepository.findById(1L)).thenReturn(Optional.of(mockGroupOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(groupOrderLikeRepository.findByUserAndGroupOrder(mockUser, mockGroupOrder))
                .thenReturn(Optional.of(mockLike));

        Integer result = groupOrderService.unlikeGroupOrder(1L, 1L);

        assertThat(result).isEqualTo(0);
        verify(groupOrderLikeRepository).delete(mockLike);
    }

    @Test
    @DisplayName("공동구매 좋아요 취소 - 좋아요 안 눌렀으면 예외")
    void unlikeGroupOrder_좋아요_안_눌렀으면_예외() {
        User mockUser = mock(User.class);
        GroupOrder mockGroupOrder = mock(GroupOrder.class);
        when(mockGroupOrder.isLikedBy(mockUser)).thenReturn(false);

        when(groupOrderRepository.findById(1L)).thenReturn(Optional.of(mockGroupOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> groupOrderService.unlikeGroupOrder(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GROUP_ORDER_LIKE_NOT_FOUND);
    }

    // ===== completeGroupOrder / unCompleteGroupOrder =====

    @Test
    @DisplayName("모집 완료 처리 - 정상 상태 변경")
    void completeGroupOrder_정상_상태_변경() {
        GroupOrder mockGroupOrder = mock(GroupOrder.class);
        when(groupOrderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockGroupOrder));

        groupOrderService.completeGroupOrder(1L, 1L);

        verify(mockGroupOrder).updateRecruitmentComplete(true);
    }

    @Test
    @DisplayName("모집 완료 해제 - 정상 상태 변경")
    void unCompleteGroupOrder_정상_상태_변경() {
        GroupOrder mockGroupOrder = mock(GroupOrder.class);
        when(groupOrderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockGroupOrder));

        groupOrderService.unCompleteGroupOrder(1L, 1L);

        verify(mockGroupOrder).updateRecruitmentComplete(false);
    }

    // ===== deleteGroupOrder =====

    @Test
    @DisplayName("공동구매 삭제 - 정상 삭제")
    void deleteGroupOrder_정상_삭제() {
        GroupOrder mockGroupOrder = mock(GroupOrder.class);
        when(groupOrderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockGroupOrder));

        groupOrderService.deleteGroupOrder(1L, 1L);

        verify(groupOrderRepository).delete(mockGroupOrder);
        verify(imageService).deleteImages(any(), eq(1L));
    }

    @Test
    @DisplayName("공동구매 삭제 - 본인 게시글 아니면 예외")
    void deleteGroupOrder_본인_게시글_아니면_예외() {
        when(groupOrderRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupOrderService.deleteGroupOrder(1L, 99L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GROUP_ORDER_NOT_OWNED_BY_USER);
    }
}
