package com.example.appcenter_project.domain.fcm.service;

import com.example.appcenter_project.domain.fcm.entity.FcmToken;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.exception.ErrorCode;
import com.example.appcenter_project.domain.user.repository.FcmTokenRepository;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final UserRepository userRepository;
    private final FcmTokenRepository fcmTokenRepository;

    @Transactional
    public void saveToken(Long userId, String token) {
        if (token == null || token.isBlank()) {
            log.warn("[FCM] saveToken(userId) 거부 - null 또는 빈 토큰 (userId={})", userId);
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (fcmTokenRepository.existsByToken(token)) {
            log.debug("[FCM] 토큰 이미 존재 - 저장 스킵 (userId={})", userId);
            return;
        }

        fcmTokenRepository.findByUser(user)
                .ifPresentOrElse(
                        existing -> {
                            existing.updateToken(token);
                            log.info("[FCM] 기존 토큰 갱신 (userId={})", userId);
                        },
                        () -> {
                            FcmToken newToken = FcmToken.builder()
                                    .user(user)
                                    .token(token)
                                    .build();
                            fcmTokenRepository.save(newToken);
                            user.addFcmToken(newToken);
                            log.info("[FCM] 신규 토큰 저장 (userId={})", userId);
                        }
                );
    }

    @Transactional
    public void saveToken(CustomUserDetails userDetails, String token) {
        if (token == null || token.isBlank()) {
            Long uid = userDetails != null ? userDetails.getId() : null;
            log.warn("[FCM] saveToken 거부 - null 또는 빈 토큰 (userId={})", uid);
            return;
        }

        if (userDetails != null) {
            User user = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            fcmTokenRepository.findFirstByToken(token).ifPresentOrElse(
                    fcmToken -> {
                        if (fcmToken.getUser() == null || !fcmToken.getUser().getId().equals(user.getId())) {
                            fcmToken.updateUser(user);
                            log.info("[FCM] 토큰 소유자 교정 (userId={}, 기존 소유자={})",
                                    user.getId(), fcmToken.getUser() == null ? "none" : fcmToken.getUser().getId());
                        } else {
                            log.debug("[FCM] 토큰 이미 존재 - 저장 스킵 (userId={})", user.getId());
                        }
                    },
                    () -> fcmTokenRepository.findByUser(user).ifPresentOrElse(
                            existing -> {
                                existing.updateToken(token);
                                log.info("[FCM] 기존 토큰 갱신 (userId={})", user.getId());
                            },
                            () -> {
                                fcmTokenRepository.save(FcmToken.builder().user(user).token(token).build());
                                log.info("[FCM] 신규 토큰 저장 (userId={})", user.getId());
                            }
                    )
            );

        } else {
            if (fcmTokenRepository.existsByToken(token)) {
                log.debug("[FCM] 비로그인 토큰 이미 존재 - 저장 스킵");
                return;
            }
            fcmTokenRepository.save(FcmToken.builder().token(token).build());
            log.info("[FCM] 비로그인 신규 토큰 저장");
        }
    }
}
