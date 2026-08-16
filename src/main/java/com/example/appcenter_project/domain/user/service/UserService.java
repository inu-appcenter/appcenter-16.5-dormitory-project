package com.example.appcenter_project.domain.user.service;

import com.example.appcenter_project.domain.groupOrder.service.GroupOrderQueryService;
import com.example.appcenter_project.domain.openChat.service.OpenChatDormOfficialRoomService;
import com.example.appcenter_project.domain.openChat.service.OpenChatMessageReportService;
import com.example.appcenter_project.domain.user.dto.request.*;
import com.example.appcenter_project.common.image.dto.ImageLinkDto;
import com.example.appcenter_project.domain.groupOrder.dto.response.ResponseGroupOrderDto;
import com.example.appcenter_project.domain.roommate.dto.response.ResponseRoommatePostDto;
import com.example.appcenter_project.domain.tip.dto.response.ResponseTipDto;
import com.example.appcenter_project.domain.user.dto.response.ResponseBoardDto;
import com.example.appcenter_project.domain.user.dto.response.ResponseLoginDto;
import com.example.appcenter_project.domain.user.dto.response.ResponseUserDto;
import com.example.appcenter_project.domain.user.dto.response.ResponseUserRole;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.common.image.enums.ImageType;
import com.example.appcenter_project.domain.user.enums.College;
import com.example.appcenter_project.domain.user.enums.DormType;
import com.example.appcenter_project.domain.user.enums.Role;
import com.example.appcenter_project.global.config.TestAccountProperties;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.domain.user.entity.RefreshToken;
import com.example.appcenter_project.domain.user.repository.RefreshTokenRepository;
import com.example.appcenter_project.domain.user.repository.SchoolLoginRepository;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.exception.ErrorCode;
import com.example.appcenter_project.global.mixpanel.MixpanelService;
import com.example.appcenter_project.global.security.jwt.JwtTokenProvider;
import com.example.appcenter_project.domain.fcm.service.FcmMessageService;
import com.example.appcenter_project.common.image.service.ImageService;
import com.example.appcenter_project.domain.notification.service.RoommateNotificationService;
import com.example.appcenter_project.domain.roommate.repository.RoommateBoardRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateCheckListRepository;
import com.example.appcenter_project.domain.roommate.service.MatchingPeriod;
import com.example.appcenter_project.domain.roommate.service.RoommateMatchingPeriodResolver;
import com.example.appcenter_project.domain.roommate.service.RoommateQueryService;
import com.example.appcenter_project.domain.tip.service.TipQueryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.appcenter_project.global.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final SchoolLoginRepository schoolLoginRepository;
    private final GroupOrderQueryService groupOrderQueryService;
    private final TipQueryService tipQueryService;
    private final RoommateQueryService roommateQueryService;
    private final ImageService imageService;
    private final FcmMessageService fcmMessageService;
    private final MixpanelService mixpanelService;
    private final TestAccountProperties testAccountProperties;
    private final OpenChatMessageReportService openChatMessageReportService;
    private final OpenChatDormOfficialRoomService openChatDormOfficialRoomService;
    private final RoommateCheckListRepository roommateCheckListRepository;
    private final RoommateBoardRepository roommateBoardRepository;
    private final RoommateNotificationService roommateNotificationService;
    private final RoommateMatchingPeriodResolver periodResolver;

    // ========== Public Methods ========== //

    public ResponseLoginDto saveUser(SignupUser signupUser) {
        boolean isTestAccount = testAccountProperties.matches(signupUser.getStudentNumber(), signupUser.getPassword());
        if (!isTestAccount) {
            checkINUStudent(signupUser);
        }
        User user = isTestAccount ? createTestAccountUser(signupUser) : createUser(signupUser);
        trackSignupProfile(user);
        trackLoginComplete(user, true);
        return createDto(user);
    }

    public ResponseLoginDto saveFreshman(SignupUser signupUser) {
        User user = createFreshman(signupUser);
        trackSignupProfile(user);
        trackLoginComplete(user, true);
        return createDto(user);
    }

    public ResponseLoginDto loginFreshman(SignupUser signupUser) {
        try {
            User user = findFreshmanForLogin(signupUser);
            trackLoginComplete(user, false);
            return createDto(user);
        } catch (CustomException e) {
            trackLoginFail(signupUser.getStudentNumber(), e.getErrorCode().name());
            throw e;
        }
    }

    public ResponseLoginDto reissueAccessToken(RequestTokenDto request) {
        validateRefreshToken(request.getRefreshToken());
        String refreshToken = extractBearerToken(request);
        return reissueAccessTokenByRefreshToken(refreshToken);
    }

    public void sendPushNotification(RequestUserPushNotification request) {
        String title = request.getTitle();
        String body = request.getBody();

        List<User> userIds = userRepository.findAllById(request.getUserIds());
        sendMessageToUsers(userIds, title, body);
    }

    public void changeUserRole(RequestUserRoleDto request) {
        Role role = Role.from(request.getRole());
        User user = userRepository.findByStudentNumber(request.getStudentNumber()).orElseThrow(() -> new CustomException(USER_NOT_FOUND));
        user.changeRole(role);
    }

    public ResponseUserDto findUser(Long userId) {
        User user = findUserById(userId);
        boolean hasUnreadNotifications = user.hasUnreadNotifications();
        MatchingPeriod period = periodResolver.resolveCurrent(LocalDate.now());
        boolean hasRoommateCheckList = roommateCheckListRepository.existsByUserIdAndYearAndSemester(userId, period.year(), period.semester());
        long reportedCount = openChatMessageReportService.countApprovedReports(user.getStudentNumber());

        return ResponseUserDto.from(user, hasRoommateCheckList, hasUnreadNotifications, reportedCount);
    }

    public List<ResponseUserDto> findAllUsers() {
        Map<String, Long> countMap = openChatMessageReportService.approvedCountMap();
        return userRepository.findAll().stream()
                .map(u -> ResponseUserDto.createBasicDto(u, countMap.getOrDefault(u.getStudentNumber(), 0L)))
                .toList();
    }

    public List<ResponseUserRole> findUsersDormitoryRoles() {
        List<Role> roles = getDormitoryRoles();
        List<User> users = userRepository.findByRoleIn(roles);

        return users.stream()
                .map(ResponseUserRole::from)
                .toList();
    }

    public List<ResponseBoardDto> findUserBoards(Long userId, HttpServletRequest request) {
        List<ResponseRoommatePostDto> roommateBoards = roommateQueryService.findByUser(userId);
        List<ResponseTipDto> tips = tipQueryService.findTipsByUser(userId, request);
        List<ResponseGroupOrderDto> groupOrders = groupOrderQueryService.findGroupOrdersByUser(userId, request);

        return Stream.of(roommateBoards, tips, groupOrders)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(ResponseBoardDto::getCreateDate).reversed())
                .collect(Collectors.toList());
    }

    public List<ResponseBoardDto> findUserLikedBoards(Long userId, HttpServletRequest request) {
        List<ResponseRoommatePostDto> roommateBoards = roommateQueryService.findLikedByUser(userId);
        List<ResponseTipDto> tips = tipQueryService.findLikedByUser(userId, request);
        List<ResponseGroupOrderDto> groupOrders = groupOrderQueryService.findLikedByUser(userId, request);

        return Stream.of(roommateBoards, tips, groupOrders)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(ResponseBoardDto::getCreateDate).reversed())
                .collect(Collectors.toList());
    }

    public ImageLinkDto findUserImage(Long userId, HttpServletRequest request) {
        return imageService.findImage(ImageType.USER, userId, request);
    }

    public ImageLinkDto findUserTimeTableImage(Long userId, HttpServletRequest request) {
        return imageService.findImage(ImageType.TIME_TABLE, userId, request);
    }

    public ResponseLoginDto convertToPermanent(Long userId, SignupUser signupUser) {
        User user = findUserById(userId);
        user.validateFreshman();
        checkINUStudent(signupUser);
        refreshTokenRepository.deleteByUser(user);
        deleteConflictingUser(userId, signupUser.getStudentNumber());
        User updatedUser = convertINUUser(userId, signupUser);

        return createDto(updatedUser);
    }

    private void deleteConflictingUser(Long userId, String studentNumber) {
        userRepository.findByStudentNumber(studentNumber)
                .ifPresent(existing -> {
                    if (existing.getId().equals(userId)) {
                        return;
                    }
                    refreshTokenRepository.deleteByUser(existing);
                    userRepository.delete(existing);
                    userRepository.flush();
                });
    }

    private User convertINUUser(Long userId, SignupUser signupUser) {
        User user = findUserById(userId);
        return user.convertINUUser(signupUser.getStudentNumber(), passwordEncoder.encode(signupUser.getPassword()));
    }

    private void checkAlreadyRegistered(SignupUser signupUser) {
        if (userRepository.findByStudentNumber(signupUser.getStudentNumber()).isPresent()) {
            throw new CustomException(ErrorCode.ALREADY_REGISTERED_USER);
        }
    }

    public ResponseUserDto updateUser(Long userId, RequestUserDto request) {
        User user = findUserById(userId);
        DormType oldDorm = user.getDormType();
        user.update(request);
        if (oldDorm != user.getDormType()) {
            openChatDormOfficialRoomService.reassignDormRoom(userId, oldDorm, user.getDormType());
        }
        MatchingPeriod period = periodResolver.resolveCurrent(LocalDate.now());
        roommateCheckListRepository.findFirstByUserIdAndYearAndSemester(userId, period.year(), period.semester())
                .ifPresent(cl -> cl.syncUserInfo(user.getDormType(), user.getCollege()));
        roommateBoardRepository.findByUserIdAndYearAndSemester(userId, period.year(), period.semester())
                .ifPresent(board -> roommateNotificationService.sendFilteredNotificationsOnUpdate(board, userId));
        boolean hasUnreadNotifications = user.hasUnreadNotifications();
        boolean hasRoommateCheckList = roommateCheckListRepository.existsByUserIdAndYearAndSemester(userId, period.year(), period.semester());
        long reportedCount = openChatMessageReportService.countApprovedReports(user.getStudentNumber());

        return ResponseUserDto.from(user, hasRoommateCheckList, hasUnreadNotifications, reportedCount);
    }

    public void updateUserAgreement(Long userId, boolean isTermsAgreed, boolean isPrivacyAgreed) {
        User user = findUserById(userId);
        user.updateTermsAgreed(isTermsAgreed);
        user.updatePrivacyAgreed(isPrivacyAgreed);
    }

    public void updateUserImage(Long userId, MultipartFile image) {
        imageService.updateImage(ImageType.USER, userId, image);
    }

    public void updateUserTimeTableImage(Long userId, MultipartFile image) {
        imageService.updateImage(ImageType.TIME_TABLE, userId, image);

        try {
            User user = findUserById(userId);
            mixpanelService.trackEvent(user.getId().toString(), "schedule_update", new JSONObject());
        } catch (Exception e) {
            log.warn("Mixpanel schedule_update 이벤트 추적 실패 - userId: {}", userId);
        }
    }

    public void deleteUser(Long userId) {
        User user = findUserById(userId);
        refreshTokenRepository.deleteByUser(user);
        userRepository.deleteById(userId);
    }

    public void deleteUserTimeTableImage(Long userId) {
        checkExistsUser(userId);
        imageService.deleteImage(ImageType.TIME_TABLE, userId);
    }


    // ========== Private Methods ========== //
    private void checkINUStudent(SignupUser signupUser) {
        if (isNotINUStudent(schoolLoginRepository.loginCheck(signupUser.getStudentNumber(), signupUser.getPassword()))) {
            throw new CustomException(USER_NOT_FOUND);
        }
    }

    private static boolean isNotINUStudent(String loginCheck) {
        return Objects.equals(loginCheck, "N");
    }

    private User createUser(SignupUser signupUser) {
        if (existsUser(signupUser)) {
            return userRepository.findByStudentNumber(signupUser.getStudentNumber()).get();
        }

        User user = User.createNewUser(signupUser.getStudentNumber(), passwordEncoder.encode(signupUser.getPassword()));
        userRepository.save(user);

        return user;
    }

    private User createTestAccountUser(SignupUser signupUser) {
        if (existsUser(signupUser)) {
            return userRepository.findByStudentNumber(signupUser.getStudentNumber()).get();
        }

        User user = User.createTestUser(
                signupUser.getStudentNumber(),
                passwordEncoder.encode(signupUser.getPassword()),
                testAccountProperties.getName(),
                DormType.from(testAccountProperties.getDormType()),
                College.from(testAccountProperties.getCollege()),
                Role.from(testAccountProperties.getRole())
        );
        userRepository.save(user);
        return user;
    }

    private User createFreshman(SignupUser signupUser) {
        if (existsUser(signupUser)) {
            throw new CustomException(ALREADY_REGISTERED_USER);
        }

        User user = User.createFreshman(signupUser.getStudentNumber(), passwordEncoder.encode(signupUser.getPassword()));
        userRepository.save(user);
        return user;
    }

    private User findFreshmanForLogin(SignupUser signupUser) {
        User user = userRepository.findByStudentNumber(signupUser.getStudentNumber())
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
        user.validateFreshman();
        if (!passwordEncoder.matches(signupUser.getPassword(), user.getPassword())) {
            throw new CustomException(INVALID_PASSWORD);
        }
        return user;
    }

    private boolean existsUser(SignupUser signupUser) {
        return userRepository.existsByStudentNumber(signupUser.getStudentNumber());
    }

    private ResponseLoginDto createDto(User user) {
        String accessToken = createAccessToken(user);
        String refreshToken = createRefreshToken(user);

        return new ResponseLoginDto(accessToken, refreshToken, user.getRole().toString());
    }

    private String createAccessToken(User user) {
        return jwtTokenProvider.generateAccessToken(user.getId(), user.getStudentNumber(), String.valueOf(user.getRole()));
    }

    private String createRefreshToken(User user) {
        String token = jwtTokenProvider.generateRefreshToken(user.getId(), user.getStudentNumber(), String.valueOf(user.getRole()));
        refreshTokenRepository.save(RefreshToken.builder().user(user).token(token).build());
        return token;
    }

    private void validateRefreshToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new CustomException(INVALID_REFRESH_TOKEN);
        }
    }

    private static String extractBearerToken(RequestTokenDto request) {
        return request.getRefreshToken().substring(7);
    }

    private ResponseLoginDto reissueAccessTokenByRefreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new CustomException(INVALID_REFRESH_TOKEN);
        }

        RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new CustomException(REFRESH_TOKEN_USER_NOT_FOUND));
        User user = refreshTokenEntity.getUser();

        refreshTokenRepository.delete(refreshTokenEntity);

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getStudentNumber(), String.valueOf(user.getRole()));
        String newRefreshToken = createRefreshToken(user);

        return new ResponseLoginDto(newAccessToken, newRefreshToken, user.getRole().toString());
    }

    private void sendMessageToUsers(List<User> userIds, String title, String body) {
        userIds.forEach(user ->
                fcmMessageService.sendNotification(user, title, body)
        );
    }

    private static boolean isNotAdminRole(Role role) {
        return role != Role.ROLE_ADMIN;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

    private void checkExistsUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(USER_NOT_FOUND);
        }
    }

    private static List<Role> getDormitoryRoles() {
        return List.of(
                Role.ROLE_DORM_LIFE_MANAGER,
                Role.ROLE_DORM_ROOMMATE_MANAGER,
                Role.ROLE_DORM_MANAGER,
                Role.ROLE_DORM_EXPEDITED_COMPLAINT_MANAGER
        );
    }

    private void trackSignupProfile(User user) {
        try {
            JSONObject profileProps = new JSONObject();
            profileProps.put("$created", user.getCreatedDate() != null ? user.getCreatedDate().toString() : LocalDate.now().toString());
            if (user.getDormType() != null) {
                profileProps.put("dormitory", user.getDormType().toValue());
            }
            if (user.getCollege() != null) {
                profileProps.put("department", user.getCollege().toValue());
            }
            mixpanelService.setUserProfile(user.getId().toString(), profileProps);
        } catch (Exception e) {
            log.warn("Mixpanel 가입 프로필 설정 실패 - userId: {}", user.getId());
        }
    }

    private void trackLoginComplete(User user, boolean isFirstLogin) {
        try {
            JSONObject eventProps = new JSONObject();
            if (user.getDormType() != null) {
                eventProps.put("dormitory", user.getDormType().toValue());
            }
            if (user.getCollege() != null) {
                eventProps.put("department", user.getCollege().toValue());
            }
            eventProps.put("is_first_login", isFirstLogin);
            mixpanelService.trackEvent(user.getId().toString(), "Login_complete", eventProps);

            if (isFirstLogin) {
                mixpanelService.trackEvent(user.getId().toString(), "first_login_completed", eventProps);
            }

            JSONObject profileProps = new JSONObject();
            profileProps.put("last_active_date", java.time.Instant.now().toString());
            if (user.getDormType() != null) {
                profileProps.put("dormitory", user.getDormType().toValue());
            }
            if (user.getCollege() != null) {
                profileProps.put("department", user.getCollege().toValue());
            }
            mixpanelService.setUserProfile(user.getId().toString(), profileProps);
        } catch (Exception e) {
            log.warn("Mixpanel 로그인 이벤트 추적 실패 - userId: {}", user.getId());
        }
    }

    public void reassignDormRoomOnDormTypeChange(Long userId, DormType oldDorm, DormType newDorm) {
        userRepository.findById(userId);
        if (oldDorm == newDorm) {
            return;
        }
        openChatDormOfficialRoomService.reassignDormRoom(userId, oldDorm, newDorm);
    }

    private void trackLoginFail(String studentNumber, String reason) {
        try {
            JSONObject eventProps = new JSONObject();
            eventProps.put("reason", reason);
            mixpanelService.trackEvent(studentNumber, "Login_fail", eventProps);
        } catch (Exception e) {
            log.warn("Mixpanel 로그인 실패 이벤트 추적 실패 - studentNumber: {}", studentNumber);
        }
    }


}