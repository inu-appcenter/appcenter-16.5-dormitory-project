package com.example.appcenter_project.domain.groupOrder.service;

import com.example.appcenter_project.domain.groupOrder.dto.response.GroupOrderListProjection;
import com.example.appcenter_project.domain.groupOrder.dto.response.ResponseGroupOrderCommentDto;
import com.example.appcenter_project.domain.groupOrder.dto.response.ResponseGroupOrderDetailDto;
import com.example.appcenter_project.domain.groupOrder.dto.response.ResponseGroupOrderDto;
import com.example.appcenter_project.domain.groupOrder.dto.response.ResponseGroupOrderPopularSearch;
import com.example.appcenter_project.domain.groupOrder.entity.*;
import com.example.appcenter_project.domain.groupOrder.repository.*;
import com.example.appcenter_project.domain.place.enums.NormalizationOutcome;
import com.example.appcenter_project.domain.place.service.PlaceResolveResult;
import com.example.appcenter_project.domain.place.service.PlaceResolver;
import com.example.appcenter_project.global.mixpanel.MixpanelService;
import org.json.JSONObject;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.common.image.dto.ImageLinkDto;
import com.example.appcenter_project.domain.groupOrder.dto.request.RequestGroupOrderDto;
import com.example.appcenter_project.common.image.entity.Image;
import com.example.appcenter_project.domain.groupOrder.entity.GroupOrderLike;
import com.example.appcenter_project.global.scheduler.MealTimeChecker;
import com.example.appcenter_project.domain.groupOrder.enums.GroupOrderSort;
import com.example.appcenter_project.domain.groupOrder.enums.GroupOrderType;
import com.example.appcenter_project.common.image.enums.ImageType;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.common.image.repository.ImageRepository;
import com.example.appcenter_project.domain.groupOrder.repository.GroupOrderLikeRepository;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.security.CustomUserDetails;
import com.example.appcenter_project.common.image.service.ImageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.example.appcenter_project.global.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GroupOrderService {

    private final GroupOrderRepository groupOrderRepository;
    private final UserRepository userRepository;
    private final GroupOrderLikeRepository groupOrderLikeRepository;
    private final GroupOrderCommentRepository groupOrderCommentRepository;
    private final ImageRepository imageRepository;
    private final GroupOrderPopularSearchKeywordRepository groupOrderPopularSearchKeywordRepository;
    private final ImageService imageService;
    private final AsyncViewCountService asyncViewCountService;
    private final MealTimeChecker mealTimeChecker;
    private final GroupOrderNotificationService groupOrderNotificationService;
    private final PlaceResolver placeResolver;
    private final GroupOrderCreateService groupOrderCreateService;
    private final MixpanelService mixpanelService;

    private static final int TOP_SEARCH_KEYWORD_COUNT = 10;

    // ========== Public Methods ========== //

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public void saveGroupOrder(Long userId, RequestGroupOrderDto requestGroupOrderDto, List<MultipartFile> images) {
        GroupOrderType groupOrderType = requestGroupOrderDto.getGroupOrderType();
        String placeId = requestGroupOrderDto.getPlaceId();
        String rawPlaceName = requestGroupOrderDto.getRawPlaceName();

        boolean hasPlaceInput = (placeId != null && !placeId.isBlank())
                || (rawPlaceName != null && !rawPlaceName.isBlank());

        if (hasPlaceInput && groupOrderType != GroupOrderType.FOOD) {
            throw new CustomException(INVALID_PLACE_FOR_TYPE);
        }

        PlaceResolveResult resolveResult = null;
        if (groupOrderType == GroupOrderType.FOOD && hasPlaceInput) {
            resolveResult = placeResolver.resolve(placeId, rawPlaceName, groupOrderType);
        }

        groupOrderCreateService.createAndPersist(userId, requestGroupOrderDto, images, resolveResult, rawPlaceName);
    }

    public void addRating(Long groupOrderId, Float ratingScore) {
        GroupOrder groupOrder = groupOrderRepository.findById(groupOrderId)
                .orElseThrow(() -> new CustomException(GROUP_ORDER_NOT_FOUND));
        groupOrder.getUser().addRating(ratingScore);
    }

    public ResponseGroupOrderDetailDto findGroupOrder(CustomUserDetails currentUser, Long groupOrderId, HttpServletRequest request) {
        plusViewCount(groupOrderId);

        GroupOrder groupOrder = groupOrderRepository.findById(groupOrderId)
                .orElseThrow(() -> new CustomException(GROUP_ORDER_NOT_FOUND));
        updateExpiredGroupOrder(groupOrder);

        ResponseGroupOrderDetailDto groupOrderDetailDto = createDto(request, groupOrder);
        checkGroupOrderOwnByCurrentUser(currentUser, groupOrder, groupOrderDetailDto);
        checkCurrentUserLiked(currentUser, groupOrder, groupOrderDetailDto);
        addCommentToDto(groupOrder.getId(), request, groupOrderDetailDto);

        return groupOrderDetailDto;
    }


    public List<String> findGroupOrderSearchLog(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
        return user.getSearchLogs();
    }

    public List<ResponseGroupOrderPopularSearch> findGroupOrderPopularSearch() {
        List<GroupOrderPopularSearchKeyword> topKeywords = groupOrderPopularSearchKeywordRepository
                .findTopKeywords(TOP_SEARCH_KEYWORD_COUNT);

        return createDtoWithRank(topKeywords);
    }

    public List<ImageLinkDto> findGroupOrderImages(Long groupOrderId, HttpServletRequest request) {
        return imageService.findImages(ImageType.GROUP_ORDER, groupOrderId, request);
    }

    public List<ResponseGroupOrderDto> findGroupOrders(CustomUserDetails currentUser, GroupOrderSort sort, GroupOrderType type, String search, int page, int size, HttpServletRequest request) {
        addUserSearchLog(currentUser, search);

        List<GroupOrderListProjection> projections = groupOrderRepository.findGroupOrdersComplex(sort, type, search, PageRequest.of(page, size));

        if (search != null && !search.isBlank() && currentUser != null) {
            try {
                JSONObject props = new JSONObject();
                props.put("search_location", "group_order");
                props.put("result_count", projections.size());
                mixpanelService.trackEvent(currentUser.getId().toString(), "search", props);
            } catch (Exception e) {
                log.warn("Mixpanel search 이벤트 추적 실패 - userId: {}", currentUser.getId());
            }
        }

        if (projections.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> ids = projections.stream().map(GroupOrderListProjection::getId).toList();

        LocalDateTime now = LocalDateTime.now();
        List<Long> expiredIds = projections.stream()
                .filter(p -> !p.isRecruitmentComplete() && now.isAfter(p.getDeadline()))
                .map(GroupOrderListProjection::getId)
                .toList();
        if (!expiredIds.isEmpty()) {
            groupOrderRepository.bulkMarkExpired(expiredIds);
        }

        Map<Long, Image> imageMap = imageRepository.findGroupOrderImagesByEntityIds(ids).stream()
                .collect(Collectors.toMap(
                        Image::getEntityId,
                        image -> image,
                        (existing, replacement) -> existing));

        List<ResponseGroupOrderDto> result = new ArrayList<>();
        for (GroupOrderListProjection p : projections) {
            Image image = imageMap.get(p.getId());
            String imagePath = image != null ? imageService.getImageUrl(ImageType.GROUP_ORDER, image, request) : null;
            result.add(ResponseGroupOrderDto.ofProjection(p, imagePath));
        }
        return result;
    }

    public Integer likeGroupOrder(Long userId, Long groupOrderId) {
        GroupOrder groupOrder = groupOrderRepository.findById(groupOrderId)
                .orElseThrow(() -> new CustomException(GROUP_ORDER_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        if (groupOrder.isLikedBy(user)) {
            throw new CustomException(ALREADY_GROUP_ORDER_LIKE_USER);
        }

        createGroupOrderLike(user, groupOrder);

        return groupOrder.increaseLike();
    }

    public Integer unlikeGroupOrder(Long userId, Long groupOrderId) {
        GroupOrder groupOrder = groupOrderRepository.findById(groupOrderId)
                .orElseThrow(() -> new CustomException(GROUP_ORDER_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        if (!groupOrder.isLikedBy(user)) {
            throw new CustomException(GROUP_ORDER_LIKE_NOT_FOUND);
        }

        deleteGroupOrderLike(user, groupOrder);

        return groupOrder.decreaseLike();
    }

    public void completeGroupOrder(Long userId, Long groupOrderId) {
        GroupOrder groupOrder = groupOrderRepository.findByIdAndUserId(groupOrderId, userId)
                .orElseThrow(() -> new CustomException(GROUP_ORDER_NOT_FOUND));
        groupOrder.updateRecruitmentComplete(true);
    }

    public void unCompleteGroupOrder(Long userId, Long groupOrderId) {
        GroupOrder groupOrder = groupOrderRepository.findByIdAndUserId(groupOrderId, userId)
                .orElseThrow(() -> new CustomException(GROUP_ORDER_NOT_FOUND));
        groupOrder.updateRecruitmentComplete(false);
    }

    public void updateGroupOrder(Long userId, Long groupOrderId, RequestGroupOrderDto requestDto, List<MultipartFile> images) {
        GroupOrder groupOrder = groupOrderRepository.findByIdAndUserId(groupOrderId, userId)
                .orElseThrow(() -> new CustomException(GROUP_ORDER_NOT_OWNED_BY_USER));

        if (isChangeNewOpenChatLink(requestDto, groupOrder)) {
            groupOrderNotificationService.sendOpenChatLinkNotification(groupOrder);
        }

        groupOrder.update(requestDto);

        imageService.updateGroupOrderImages(ImageType.GROUP_ORDER, groupOrderId, images);
    }

    public void deleteGroupOrder(Long userId, Long groupOrderId) {
        GroupOrder groupOrder = groupOrderRepository.findByIdAndUserId(groupOrderId, userId)
                .orElseThrow(() -> new CustomException(GROUP_ORDER_NOT_OWNED_BY_USER));
        imageService.deleteImages(ImageType.GROUP_ORDER, groupOrderId);
        groupOrderRepository.delete(groupOrder);
    }

    public void deleteGroupOrderImage(Long userId, String imageName) {
        Image image = imageRepository.findByImageName(imageName)
                .orElseThrow(() -> new CustomException(IMAGE_NOT_FOUND));
        GroupOrder groupOrder = groupOrderRepository.findById(image.getEntityId())
                .orElseThrow(() -> new CustomException(GROUP_ORDER_NOT_FOUND));

        if (isGroupOrderOwnByUser(userId, groupOrder)) {
            throw new CustomException(IMAGE_UPDATE_NOT_ALLOWED);
        }

        imageService.deleteImage(image);
    }

    // ========== Private Methods ========== //


    private GroupOrder createGroupOrder(RequestGroupOrderDto requestGroupOrderDto, User user) {
        GroupOrder groupOrder = RequestGroupOrderDto.of(requestGroupOrderDto, user);
        groupOrderRepository.save(groupOrder);
        return groupOrder;
    }

    private GroupOrder createGroupOrderWithPlace(RequestGroupOrderDto requestGroupOrderDto, User user,
                                                  PlaceResolveResult resolveResult, String rawPlaceName) {
        GroupOrder groupOrder = RequestGroupOrderDto.of(requestGroupOrderDto, user);
        if (resolveResult != null) {
            String normalizedRaw = rawPlaceName != null && !rawPlaceName.isBlank() ? rawPlaceName : null;
            groupOrder.assignPlace(resolveResult.getPlace(), normalizedRaw);
        }
        groupOrderRepository.save(groupOrder);
        return groupOrder;
    }

    private void plusViewCount(Long groupOrderId) {
        if (mealTimeChecker.isMealTime()) {
            asyncViewCountService.incrementViewCount(groupOrderId);
        } else {
            GroupOrder groupOrder = groupOrderRepository.findByIdWithLock(groupOrderId)
                    .orElseThrow(() -> new CustomException(GROUP_ORDER_NOT_FOUND));
            groupOrder.plusViewCount();
        }
    }

    private static void updateExpiredGroupOrder(GroupOrder groupOrder) {
        LocalDateTime now = LocalDateTime.now();
        updateExpiredGroupOrder(groupOrder, now);
    }

    private ResponseGroupOrderDetailDto createDto(HttpServletRequest request, GroupOrder groupOrder) {
        String writerImageUrl = findRepresentativeUserImageUrl(request, groupOrder.getUser());
        return ResponseGroupOrderDetailDto.of(groupOrder, writerImageUrl);
    }

    private String findRepresentativeUserImageUrl(HttpServletRequest request, User findUser) {
        List<ImageLinkDto> imageLinkDtos = imageService.findImages(ImageType.USER, findUser.getId(), request);

        if (imageLinkDtos.isEmpty()) {
            return null;
        }

        return imageLinkDtos.get(0).getImageUrl();
    }

    private void checkGroupOrderOwnByCurrentUser(CustomUserDetails currentUser, GroupOrder groupOrder, ResponseGroupOrderDetailDto dto) {
        if (isLogin(currentUser)) {
            User user = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
            if (isEqualUser(groupOrder.getUser(), user)) {
                dto.updateIsMyPost(true);
            }
        }
    }

    private static boolean isEqualUser(User writtenUser, User currentUser) {
        return Objects.equals(writtenUser.getId(), currentUser.getId());
    }

    private static boolean isLogin(CustomUserDetails currentUser) {
        return currentUser != null;
    }

    private void checkCurrentUserLiked(CustomUserDetails currentUser, GroupOrder groupOrder, ResponseGroupOrderDetailDto dto) {
        if (isLogin(currentUser)) {
            User user = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
            if (isCurrentUserLiked(groupOrder, user)) {
                dto.updateIsCheckLikeCurrentUser(true);
                return;
            }

            dto.updateIsCheckLikeCurrentUser(false);
        }
    }

    private boolean isCurrentUserLiked(GroupOrder groupOrder, User user) {
        return groupOrderLikeRepository.existsByUserIdAndGroupOrderId(user.getId(), groupOrder.getId());
    }

    private void addCommentToDto(Long groupOrderId, HttpServletRequest request, ResponseGroupOrderDetailDto dto) {
        List<GroupOrderComment> allComments = groupOrderCommentRepository.findByGroupOrder_Id(groupOrderId);
        if (allComments.isEmpty()) {
            dto.updateGroupOrderCommentDtoList(Collections.emptyList());
            return;
        }

        List<Long> authorIds = allComments.stream()
                .filter(c -> !c.isDeleted())
                .map(c -> c.getUser().getId())
                .distinct()
                .toList();
        Map<Long, String> userImageUrlMap = buildUserImageUrlMap(authorIds, request);

        Map<Long, List<GroupOrderComment>> childrenByParentId = allComments.stream()
                .filter(c -> c.getParentGroupOrderComment() != null)
                .collect(Collectors.groupingBy(c -> c.getParentGroupOrderComment().getId()));

        List<ResponseGroupOrderCommentDto> commentDtos = allComments.stream()
                .filter(c -> c.getParentGroupOrderComment() == null)
                .map(parent -> buildCommentDto(parent, childrenByParentId, userImageUrlMap))
                .toList();

        dto.updateGroupOrderCommentDtoList(commentDtos);
    }

    private ResponseGroupOrderCommentDto buildCommentDto(
            GroupOrderComment parent,
            Map<Long, List<GroupOrderComment>> childrenByParentId,
            Map<Long, String> userImageUrlMap) {

        ResponseGroupOrderCommentDto parentDto = convertCommentToDto(parent, userImageUrlMap);

        List<ResponseGroupOrderCommentDto> childDtos = childrenByParentId
                .getOrDefault(parent.getId(), Collections.emptyList()).stream()
                .map(child -> convertCommentToDto(child, userImageUrlMap))
                .toList();
        parentDto.updateChildGroupOrderCommentList(childDtos);

        return parentDto;
    }

    private ResponseGroupOrderCommentDto convertCommentToDto(GroupOrderComment comment, Map<Long, String> userImageUrlMap) {
        ResponseGroupOrderCommentDto dto = ResponseGroupOrderCommentDto.from(comment);

        if (comment.isDeleted()) {
            dto.updateReply("삭제된 메시지입니다.");
            dto.updateUsername("알 수 없는 사용자");
            dto.updateCommentAuthorImagePath(null);
        } else {
            dto.updateReply(comment.getReply());
            dto.updateUsername(comment.getUser().getName());
            dto.updateCommentAuthorImagePath(userImageUrlMap.get(comment.getUser().getId()));
        }

        return dto;
    }

    private static List<ResponseGroupOrderPopularSearch> createDtoWithRank(List<GroupOrderPopularSearchKeyword> topKeywords) {
        return IntStream.range(0, topKeywords.size())
                .mapToObj(i -> ResponseGroupOrderPopularSearch.of(i + 1, topKeywords.get(i).getKeyword()))
                .toList();
    }

    private void addUserSearchLog(CustomUserDetails currentUser, String search) {
        if (currentUser != null && search != null) {
            User user = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
            user.addSearchLog(search);
        }
    }

    private static void updateExpiredGroupOrder(GroupOrder groupOrder, LocalDateTime now) {
        if (isExpired(groupOrder, now)) {
            groupOrder.updateRecruitmentComplete(true);
        }
    }

    private static boolean isExpired(GroupOrder groupOrder, LocalDateTime now) {
        return !groupOrder.isRecruitmentComplete() && now.isAfter(groupOrder.getDeadline());
    }

    private Map<Long, Image> buildRepresentativeImageMap(List<Long> groupOrderIds) {
        return imageRepository.findGroupOrderImagesByEntityIds(groupOrderIds).stream()
                .collect(Collectors.toMap(Image::getEntityId, image -> image));
    }

    private Map<Long, String> buildUserImageUrlMap(List<Long> userIds, HttpServletRequest request) {
        return imageRepository.findByImageTypeAndEntityIdIn(ImageType.USER, userIds).stream()
                .collect(Collectors.toMap(
                        Image::getEntityId,
                        image -> imageService.getImageUrl(ImageType.USER, image, request),
                        (existing, replacement) -> existing
                ));
    }

    private void createGroupOrderLike(User user, GroupOrder groupOrder) {
        GroupOrderLike groupOrderLike = GroupOrderLike.builder()
                .user(user)
                .groupOrder(groupOrder)
                .build();

        groupOrderLikeRepository.save(groupOrderLike);
    }

    private void deleteGroupOrderLike(User user, GroupOrder groupOrder) {
        GroupOrderLike groupOrderLike = groupOrderLikeRepository.findByUserAndGroupOrder(user, groupOrder)
                .orElseThrow(() -> new CustomException(GROUP_ORDER_LIKE_NOT_FOUND));

        groupOrderLikeRepository.delete(groupOrderLike);
    }

    private static boolean isChangeNewOpenChatLink(RequestGroupOrderDto requestGroupOrderDto, GroupOrder groupOrder) {
        String newLink = requestGroupOrderDto.getLink();
        String oldLink = groupOrder.getLink();

        return newLink != null && !newLink.isEmpty() && !newLink.equals(oldLink);
    }

    private static boolean isGroupOrderOwnByUser(Long userId, GroupOrder groupOrder) {
        return !Objects.equals(groupOrder.getUser().getId(), userId);
    }
}