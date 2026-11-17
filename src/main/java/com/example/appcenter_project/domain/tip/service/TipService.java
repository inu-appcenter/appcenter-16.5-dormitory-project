package com.example.appcenter_project.domain.tip.service;

import com.example.appcenter_project.common.image.dto.ImageLinkDto;
import com.example.appcenter_project.domain.tip.dto.request.RequestTipDto;
import com.example.appcenter_project.domain.tip.dto.response.ResponseTipCommentDto;
import com.example.appcenter_project.domain.tip.dto.response.ResponseTipDetailDto;
import com.example.appcenter_project.domain.tip.dto.response.ResponseTipDto;
import com.example.appcenter_project.domain.tip.entity.TipLike;
import com.example.appcenter_project.domain.tip.entity.Tip;
import com.example.appcenter_project.domain.tip.entity.TipComment;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.common.image.enums.ImageType;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.domain.tip.repository.TipLikeRepository;
import com.example.appcenter_project.domain.tip.repository.TipCommentRepository;
import com.example.appcenter_project.domain.tip.repository.TipRepository;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.security.CustomUserDetails;
import com.example.appcenter_project.common.image.service.ImageService;
import com.example.appcenter_project.global.mixpanel.MixpanelService;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

import static com.example.appcenter_project.global.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TipService {

    private final TipRepository tipRepository;
    private final UserRepository userRepository;
    private final TipCommentRepository tipCommentRepository;
    private final TipLikeRepository tipLikeRepository;
    private final ImageService imageService;
    private final MixpanelService mixpanelService;

    private static final int DAILY_RANDOM_TIP_COUNT = 3;

    // ========== Public Methods ========== //

    public void saveTip(Long userId, RequestTipDto requestDto, List<MultipartFile> images) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(USER_NOT_FOUND));
        try {
            JSONObject props = new JSONObject();
            props.put("board_type", "꿀팁");
            mixpanelService.trackEvent(user.getId().toString(), "게시글 작성 시작", props);
        } catch (Exception e) {
            log.warn("Mixpanel 게시글 작성 시작 이벤트 추적 실패 - userId: {}", userId);
        }
        Tip tip = createTip(requestDto, user);
        saveTipImages(images, tip);
        try {
            JSONObject props = new JSONObject();
            props.put("board_type", "꿀팁");
            mixpanelService.trackEvent(user.getId().toString(), "게시글 작성 완료", props);
        } catch (Exception e) {
            log.warn("Mixpanel 게시글 작성 완료 이벤트 추적 실패 - userId: {}", userId);
        }
    }

    public ResponseTipDetailDto findTip(CustomUserDetails currentUser, Long tipId, HttpServletRequest request) {
        Tip tip = tipRepository.findById(tipId).orElseThrow(() -> new CustomException(TIP_NOT_FOUND));

        ResponseTipDetailDto tipDetailDto = ResponseTipDetailDto.from(tip);

        addWriterInfoToDto(tipDetailDto, tip.getUser(), request);
        addCurrentUserLikeStatusToDto(tipDetailDto, currentUser, tip);
        addCommentToDto(tipId, request, tipDetailDto);

        return tipDetailDto;
    }

    public List<ResponseTipDto> findAllTips() {
        return tipRepository.findAllByOrderByIdDesc().stream().map(ResponseTipDto::from).toList();
    }

    public List<ResponseTipDto> findDailyRandomTips() {
        List<Long> allTipIds = tipRepository.findAllTipIds();

        if (allTipIds.size() < DAILY_RANDOM_TIP_COUNT) {
            return new ArrayList<>();
        }

        List<Long> selectedIds = createRandomIds(allTipIds);

        return tipRepository.findAllById(selectedIds).stream().map(ResponseTipDto::from).toList();
    }

    public List<ImageLinkDto> findTipImages(Long tipId, HttpServletRequest request) {
        checkTipExists(tipId);
        return imageService.findImages(ImageType.TIP, tipId, request);
    }

    private void checkTipExists(Long tipId) {
        if (!tipRepository.existsById(tipId)) {
            throw new CustomException(TIP_NOT_FOUND);
        }
    }

    public Integer likeTip(Long userId, Long tipId) {
        Tip tip = tipRepository.findById(tipId).orElseThrow(() -> new CustomException(TIP_NOT_FOUND));
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        checkAlreadyLiked(tip, user);

        createTipLike(user, tip);

        return tip.increaseLike();
    }

    public Integer unlikeTip(Long userId, Long tipId) {
        Tip tip = tipRepository.findById(tipId).orElseThrow(() -> new CustomException(TIP_NOT_FOUND));
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        checkNotAlreadyLiked(tip, user);

        deleteTipLike(user, tip);

        return tip.decreaseLike();
    }

    public void updateTip(Long userId, RequestTipDto requestTipDto, List<MultipartFile> images, Long tipId) {
        Tip tip = tipRepository.findByIdAndUserId(tipId, userId).orElseThrow(() -> new CustomException(TIP_NOT_OWNED_BY_USER));

        tip.update(requestTipDto);
        imageService.updateImages(ImageType.TIP, tipId, images);
    }

    public void deleteTip(Long userId, Long tipId) {
        checkUserOwnsTip(userId, tipId);

        tipRepository.deleteById(tipId);
        imageService.deleteImages(ImageType.TIP, tipId);
    }

    public void deleteTipImages(Long userId, Long tipId) {
        checkUserOwnsTip(userId, tipId);

        imageService.deleteImages(ImageType.TIP, tipId);
    }



    // ========== Private Methods ========== //

    private Tip createTip(RequestTipDto requestTipDto, User user) {
        Tip tip = Tip.createTip(requestTipDto.getTitle(), requestTipDto.getContent(), user);
        tipRepository.save(tip);
        return tip;
    }

    private static void checkAlreadyLiked(Tip tip, User user) {
        if (tip.isLikedBy(user)) {
            throw new CustomException(ALREADY_TIP_LIKE_USER);
        }
    }

    private static void checkNotAlreadyLiked(Tip tip, User user) {
        if (!tip.isLikedBy(user)) {
            throw new CustomException(NOT_LIKED_TIP);
        }
    }

    private void saveTipImages(List<MultipartFile> images, Tip tip) {
        if (images != null && !images.isEmpty()) {
            imageService.saveImages(ImageType.TIP, tip.getId(), images);
        }
    }

    private void addWriterInfoToDto(ResponseTipDetailDto tipDetailDto, User writer, HttpServletRequest request) {
        tipDetailDto.updateWriterName(writer.getName());

        String writerImageUrl = imageService.findStaticImageUrl(ImageType.USER, writer.getId(), request);
        tipDetailDto.updateWriterImageUrl(writerImageUrl);
    }

    private void addCurrentUserLikeStatusToDto(ResponseTipDetailDto tipDetailDto, CustomUserDetails currentUser, Tip tip) {
        if (isNotLogin(currentUser)) {
            tipDetailDto.updateIsCheckLikeCurrentUser(false);
            return;
        }

        Long currentUserId = currentUser.getId();

        boolean isCurrentUserLiked = tip.getTipLikeList().stream().anyMatch(tipLike -> tipLike.getUser().getId().equals(currentUserId));

        tipDetailDto.updateIsCheckLikeCurrentUser(isCurrentUserLiked);
    }

    private static boolean isNotLogin(CustomUserDetails currentUser) {
        return currentUser == null;
    }

    private void addCommentToDto(Long tipId, HttpServletRequest request, ResponseTipDetailDto tipDetailDto) {
        List<ResponseTipCommentDto> commentDtos = buildCommentHierarchy(tipId, request);
        tipDetailDto.updateTipCommentDtoList(commentDtos);
    }

    private List<ResponseTipCommentDto> buildCommentHierarchy(Long tipId, HttpServletRequest request) {
        List<TipComment> parentComments = tipCommentRepository.findByTipIdAndParentTipCommentIsNull(tipId);

        if (parentComments.isEmpty()) {
            return new ArrayList<>();
        }

        return parentComments.stream()
                .map(parentComment -> buildCommentDto(parentComment, request))
                .toList();
    }

    private ResponseTipCommentDto buildCommentDto(TipComment parentComment, HttpServletRequest request) {
        ResponseTipCommentDto parentDto = convertCommentToDto(parentComment, request);
        addChildCommentToParentComment(request, parentComment, parentDto);
        return parentDto;
    }

    private void addChildCommentToParentComment(HttpServletRequest request, TipComment parentComment, ResponseTipCommentDto parentDto) {
        List<ResponseTipCommentDto> childDtos = parentComment.getChildTipComments().stream()
                .map(childComment -> convertCommentToDto(childComment, request))
                .toList();

        parentDto.updateChildTipCommentList(childDtos);
    }

    private ResponseTipCommentDto convertCommentToDto(TipComment comment, HttpServletRequest request) {
        ResponseTipCommentDto dto = ResponseTipCommentDto.from(comment);

        if (comment.isDeleted()) {
            setDeletedCommentInfo(dto); // dto에 생성 메서드를 넣지 않은 이유는 setNotDeletedCommentInfo를 image 때문에 dto에서 만들기 어렵기 때문
        } else {
            setNotDeletedCommentInfo(comment, request, dto);
        }
        return dto;
    }


    private void setDeletedCommentInfo(ResponseTipCommentDto dto) {
        dto.updateReply("삭제된 메시지입니다.");
        dto.updateName("알 수 없는 사용자");
        dto.updateWriterImageFile(null);
    }

    private void setNotDeletedCommentInfo(TipComment comment, HttpServletRequest request, ResponseTipCommentDto dto) {
        dto.updateReply(comment.getReply());
        dto.updateName(comment.getUser().getName());

        String writerImageUrl = imageService.findStaticImageUrl(ImageType.USER, comment.getUser().getId(), request);
        dto.updateWriterImageFile(writerImageUrl);
    }

    private List<Long> createRandomIds(List<Long> allTipIds) {
        long todaySeed = LocalDate.now().toEpochDay();
        Random random = new Random(todaySeed);

        List<Long> shuffled = new ArrayList<>(allTipIds);
        Collections.shuffle(shuffled, random);
        return shuffled.subList(0, DAILY_RANDOM_TIP_COUNT);
    }

    private void createTipLike(User user, Tip tip) {
        TipLike tipLike = TipLike.builder().user(user).tip(tip).build();

        tipLikeRepository.save(tipLike);
    }

    private void deleteTipLike(User user, Tip tip) {
        TipLike tipLike = tipLikeRepository.findByUserAndTip(user, tip).orElseThrow(() -> new CustomException(TIP_LIKE_NOT_FOUND));
        tipLikeRepository.delete(tipLike);
    }

    private void checkUserOwnsTip(Long userId, Long tipId) {
        if (!tipRepository.existsByIdAndUserId(tipId, userId)) {
            throw new CustomException(TIP_NOT_OWNED_BY_USER);
        }
    }
}