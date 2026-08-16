package com.example.appcenter_project.domain.roommate.service;

import com.example.appcenter_project.domain.notification.entity.Notification;
import com.example.appcenter_project.domain.notification.service.NotificationService;
import com.example.appcenter_project.domain.roommate.dto.response.ResponseReceivedRoommateMatchingDto;
import com.example.appcenter_project.domain.roommate.dto.response.ResponseRoommateMatchingDto;
import com.example.appcenter_project.domain.roommate.entity.MyRoommate;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom;
import com.example.appcenter_project.domain.roommate.entity.RoommateMatching;
import com.example.appcenter_project.domain.roommate.enums.SemesterType;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.roommate.enums.MatchingStatus;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.exception.ErrorCode;
import com.example.appcenter_project.domain.roommate.repository.MyRoommateRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateChattingRoomRepository;
import com.example.appcenter_project.domain.roommate.repository.RoommateMatchingRepository;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.domain.fcm.service.FcmMessageService;
import com.example.appcenter_project.global.mixpanel.MixpanelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Transactional
@Slf4j
@Service
@RequiredArgsConstructor
public class RoommateMatchingService {

    private final RoommateMatchingRepository roommateMatchingRepository;
    private final UserRepository userRepository;
    private final MyRoommateRepository myRoommateRepository;
    private final RoommateChattingRoomRepository roommateChattingRoomRepository;
    private final FcmMessageService fcmMessageService;
    private final NotificationService notificationService;
    private final MixpanelService mixpanelService;
    private final RoommateMatchingPeriodResolver periodResolver;

    //매칭요청 학번
    @Transactional
    public ResponseRoommateMatchingDto requestMatching(Long senderId, String receiverStudentNumber) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOMMATE_USER_NOT_FOUND));

        User receiver = userRepository.findByStudentNumber(receiverStudentNumber)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOMMATE_USER_NOT_FOUND));

        return processMatchingRequest(sender, receiver);
    }

    // 매칭 요청 채팅방id
    public ResponseRoommateMatchingDto requestMatchingByChatRoom(Long senderId, Long chatRoomId) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOMMATE_USER_NOT_FOUND));

        // 채팅방 조회
        RoommateChattingRoom chattingRoom = roommateChattingRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOMMATE_CHAT_ROOM_NOT_FOUND));

        // 상대방 유저 추출 (본인을 sender로 가정, 상대는 host or guest)
        User receiver;
        if (chattingRoom.getHost().getId().equals(senderId)) {
            receiver = chattingRoom.getGuest();
        } else if (chattingRoom.getGuest().getId().equals(senderId)) {
            receiver = chattingRoom.getHost();
        } else {
            throw new CustomException(ErrorCode.ROOMMATE_FORBIDDEN_ACCESS);
        }

        return processMatchingRequest(sender, receiver);
    }

    // 매칭 수락
    public void acceptMatching(Long matchingId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOMMATE_USER_NOT_FOUND));

        RoommateMatching matching = roommateMatchingRepository.findById(matchingId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOMMATE_MATCHING_NOT_FOUND));

        // 매칭 요청의 수신자가 현재 로그인한 사용자인지 확인
        if (!matching.getReceiver().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.ROOMMATE_MATCHING_NOT_FOR_USER);
        }

        // 이미 완료된 요청인지 확인
        if (matching.getStatus() != MatchingStatus.REQUEST) {
            throw new CustomException(ErrorCode.ROOMMATE_MATCHING_ALREADY_COMPLETED);
        }

        validateNotAlreadyMatched(user);

        matching.complete();
        User sender = matching.getSender();
        User receiver = matching.getReceiver();
        registerMyRoommate(sender, receiver);

        cleanUpOldMatchingRecords(sender, receiver);
        sendAcceptNotification(sender, receiver, matching.getId());
        sendAcceptNotification(receiver, sender, matching.getId());

        try {
            JSONObject acceptProps = new JSONObject();
            acceptProps.put("matching_id", matching.getId());
            mixpanelService.trackEvent(user.getId().toString(), "matching_request_accepted", acceptProps);

            JSONObject completedProps = new JSONObject();
            completedProps.put("matching_id", matching.getId());
            mixpanelService.trackEvent(sender.getId().toString(), "matching_completed", completedProps);
            mixpanelService.trackEvent(receiver.getId().toString(), "matching_completed", completedProps);

            JSONObject profileProps = new JSONObject();
            profileProps.put("matching_completed", true);
            mixpanelService.setUserProfile(user.getId().toString(), profileProps);
        } catch (Exception e) {
            log.warn("Mixpanel 매칭 수락 이벤트 추적 실패 - userId: {}", userId);
        }
    }

    // 매칭 거절
    public void rejectMatching(Long matchingId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOMMATE_USER_NOT_FOUND));

        RoommateMatching matching = roommateMatchingRepository.findById(matchingId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOMMATE_MATCHING_NOT_FOUND));

        if (!matching.getReceiver().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.ROOMMATE_MATCHING_NOT_FOR_USER);
        }

        if (matching.getStatus() != MatchingStatus.REQUEST) {
            throw new CustomException(ErrorCode.ROOMMATE_MATCHING_ALREADY_COMPLETED);
        }

        matching.fail();

        try {
            mixpanelService.trackEvent(user.getId().toString(), "matching_reject", new JSONObject());
        } catch (Exception e) {
            log.warn("Mixpanel 매칭 거절 이벤트 추적 실패 - userId: {}", userId);
        }
    }

    // 매칭조회
    @Transactional(readOnly = true)
    public List<ResponseReceivedRoommateMatchingDto> getReceivedMatchings(Long receiverId) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOMMATE_USER_NOT_FOUND));

        List<RoommateMatching> matchings = roommateMatchingRepository.findAllByReceiverAndStatus(receiver, MatchingStatus.REQUEST);

        return matchings.stream()
                .map(matching -> ResponseReceivedRoommateMatchingDto.builder()
                        .matchingId(matching.getId())
                        .senderId(matching.getSender().getId())
                        .senderName(matching.getSender().getName())
                        .status(matching.getStatus())
                        .build())
                .toList();
    }

    // 매칭 취소 (완료된 상태에서만 가능)
    @Transactional
    public void cancelMatching(Long matchingId, Long userId) {
        RoommateMatching matching = roommateMatchingRepository.findById(matchingId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOMMATE_MATCHING_NOT_FOUND));

        // 매칭이 COMPLETED 상태인지 확인
        if (matching.getStatus() != MatchingStatus.COMPLETED) {
            throw new CustomException(ErrorCode.ROOMMATE_MATCHING_NOT_COMPLETED);
        }

        // sender나 receiver 중 한 명만 가능
        if (!matching.getSender().getId().equals(userId) && !matching.getReceiver().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ROOMMATE_MATCHING_NOT_FOR_USER);
        }

        // MyRoommate 관계도 해제
        User sender = matching.getSender();
        User receiver = matching.getReceiver();

        log.info("=== cancelMatching START ===");
        log.info("matchingId: {}, userId: {}", matchingId, userId);
        log.info("senderId: {}, receiverId: {}", sender.getId(), receiver.getId());

        int deletedCount1;
        int deletedCount2;
        if (periodResolver == null) {
            deletedCount1 = myRoommateRepository.deleteByUserIdAndRoommateId(sender.getId(), receiver.getId());
            deletedCount2 = myRoommateRepository.deleteByUserIdAndRoommateId(receiver.getId(), sender.getId());
        } else {
            MatchingPeriod period = periodResolver.resolveCurrent(LocalDate.now());
            Integer year = period.year();
            SemesterType semester = period.semester();
            deletedCount1 = myRoommateRepository.deleteByUserIdAndRoommateIdAndYearAndSemester(sender.getId(), receiver.getId(), year, semester);
            deletedCount2 = myRoommateRepository.deleteByUserIdAndRoommateIdAndYearAndSemester(receiver.getId(), sender.getId(), year, semester);
        }
        log.info("Deleted {} rows for sender {} -> receiver {}", deletedCount1, sender.getId(), receiver.getId());
        log.info("Deleted {} rows for receiver {} -> sender {}", deletedCount2, receiver.getId(), sender.getId());

        log.info("Total deleted MyRoommate rows: {}", deletedCount1 + deletedCount2);

        // RoommateMatching 레코드 완전 삭제
        roommateMatchingRepository.delete(matching);
        log.info("RoommateMatching record deleted for matchingId: {}", matchingId);

        try {
            JSONObject props = new JSONObject();
            props.put("matching_id", matchingId);
            mixpanelService.trackEvent(userId.toString(), "matching_cancelled", props);
        } catch (Exception e) {
            log.warn("Mixpanel 매칭 취소 이벤트 추적 실패 - userId: {}", userId);
        }

        log.info("=== cancelMatching END ===");
    }

    // ========== Private Methods ========== //
    private ResponseRoommateMatchingDto processMatchingRequest(User sender, User receiver) {
        validateNotAlreadyMatched(sender);
        validateNotAlreadyMatched(receiver);

        Optional<RoommateMatching> reverseRequest =
                roommateMatchingRepository.findBySenderAndReceiverAndStatus(receiver, sender, MatchingStatus.REQUEST);

        if (reverseRequest.isPresent()) {
            return completeMatchingFromReverseRequest(reverseRequest.get(), sender, receiver);
        }

        boolean exists = roommateMatchingRepository
                .existsBySenderAndReceiverAndStatus(sender, receiver, MatchingStatus.REQUEST);
        if (exists) {
            throw new CustomException(ErrorCode.ROOMMATE_MATCHING_ALREADY_REQUESTED);
        }

        RoommateMatching matching = RoommateMatching.builder()
                .check(MatchingStatus.REQUEST)
                .sender(sender)
                .receiver(receiver)
                .build();

        roommateMatchingRepository.save(matching);
        sendRequestNotification(sender, receiver, matching.getId());

        try {
            JSONObject props = new JSONObject();
            props.put("receiver_id", receiver.getId());
            mixpanelService.trackEvent(sender.getId().toString(), "matching_request_sent", props);
        } catch (Exception e) {
            log.warn("Mixpanel 매칭 요청 이벤트 추적 실패 - senderId: {}", sender.getId());
        }

        return ResponseRoommateMatchingDto.builder()
                .matchingId(matching.getId())
                .receiverId(matching.getReceiver().getId())
                .status(matching.getStatus())
                .build();
    }

    private void validateNotAlreadyMatched(User user) {
        boolean alreadyMatched =
                roommateMatchingRepository.existsBySenderAndStatus(user, MatchingStatus.COMPLETED) ||
                        roommateMatchingRepository.existsByReceiverAndStatus(user, MatchingStatus.COMPLETED);

        if (alreadyMatched) {
            throw new CustomException(ErrorCode.ROOMMATE_ALREADY_MATCHED);
        }
    }

    private ResponseRoommateMatchingDto completeMatchingFromReverseRequest(
            RoommateMatching reverseRequest, User sender, User receiver) {

        reverseRequest.complete();

        User originalSender = reverseRequest.getSender();   // 먼저 신청한 유저
        User originalReceiver = reverseRequest.getReceiver(); // 나중에 신청한 유저

        registerMyRoommate(originalSender, originalReceiver);
        cleanUpOldMatchingRecords(originalSender, originalReceiver);
        sendAcceptNotification(originalSender, originalReceiver, reverseRequest.getId());
        sendAcceptNotification(originalReceiver, originalSender, reverseRequest.getId());

        log.info("상호 신청 감지로 매칭 완료 처리: matchingId={}, sender={}, receiver={}",
                reverseRequest.getId(), originalSender.getId(), originalReceiver.getId());

        try {
            JSONObject props = new JSONObject();
            props.put("matching_id", reverseRequest.getId());
            mixpanelService.trackEvent(originalSender.getId().toString(), "matching_completed", props);
            mixpanelService.trackEvent(originalReceiver.getId().toString(), "matching_completed", props);
        } catch (Exception e) {
            log.warn("Mixpanel 상호 매칭 완료 이벤트 추적 실패 - matchingId: {}", reverseRequest.getId());
        }

        return ResponseRoommateMatchingDto.builder()
                .matchingId(reverseRequest.getId())
                .receiverId(originalReceiver.getId())
                .status(reverseRequest.getStatus())
                .build();
    }

    private void registerMyRoommate(User sender, User receiver) {
        if (periodResolver == null) {
            myRoommateRepository.save(MyRoommate.builder().user(sender).roommate(receiver).build());
            myRoommateRepository.save(MyRoommate.builder().user(receiver).roommate(sender).build());
            return;
        }
        MatchingPeriod period = periodResolver.resolveCurrent(LocalDate.now());
        Integer year = period.year();
        SemesterType semester = period.semester();

        if (myRoommateRepository.findByUserIdAndYearAndSemester(sender.getId(), year, semester).isEmpty()) {
            MyRoommate myRoommate1 = MyRoommate.builder()
                    .user(sender)
                    .roommate(receiver)
                    .year(year)
                    .semester(semester)
                    .build();
            myRoommateRepository.save(myRoommate1);
        }

        if (myRoommateRepository.findByUserIdAndYearAndSemester(receiver.getId(), year, semester).isEmpty()) {
            MyRoommate myRoommate2 = MyRoommate.builder()
                    .user(receiver)
                    .roommate(sender)
                    .year(year)
                    .semester(semester)
                    .build();
            myRoommateRepository.save(myRoommate2);
        }
    }

    private void cleanUpOldMatchingRecords(User sender, User receiver) {
        List<RoommateMatching> senderToReceiverOldRecords =
                roommateMatchingRepository.findAllBySenderAndReceiverAndStatusNot(sender, receiver, MatchingStatus.COMPLETED);
        roommateMatchingRepository.deleteAll(senderToReceiverOldRecords);

        List<RoommateMatching> receiverToSenderOldRecords =
                roommateMatchingRepository.findAllBySenderAndReceiverAndStatusNot(receiver, sender, MatchingStatus.COMPLETED);
        roommateMatchingRepository.deleteAll(receiverToSenderOldRecords);

        if (!senderToReceiverOldRecords.isEmpty() || !receiverToSenderOldRecords.isEmpty()) {
            log.info("매칭 이전 신청 기록 정리: sender={}, receiver={}, 삭제 건수={}",
                    sender.getId(), receiver.getId(),
                    senderToReceiverOldRecords.size() + receiverToSenderOldRecords.size());
        }
    }

    private void sendRequestNotification(User sender, User receiver, Long matchingId) {
        Notification requestNotification = notificationService.createRoommateRequestNotification(sender.getName(), matchingId);
        notificationService.createUserNotification(receiver, requestNotification);
        fcmMessageService.sendNotification(receiver, requestNotification.getTitle(), requestNotification.getBody());
    }

    // 공통 FCM 발송 메서드
    private void sendFcmToReceiver(User sender, User receiver) {
        // 기존 코드
/*        for (FcmToken fcmToken : receiver.getFcmTokenList()) {
            if (fcmToken != null) {
                try {
                    fcmMessageService.sendNotification(
                            fcmToken.getToken(),
                            "룸메이트 매칭 요청",
                            sender.getName() + "님이 룸메이트 매칭을 요청했습니다."
                    );
                } catch (Exception e) {
                    log.error("FCM 발송 실패: {}", e.getMessage());
                }
            } else {
                log.warn("수신자 {}의 FCM 토큰이 없음", receiver.getId());
            }
        }*/
        fcmMessageService.sendNotification(
                receiver,
                "룸메이트 매칭 요청",
                sender.getName() + "님이 룸메이트 매칭을 요청했습니다."
        );
    }

    private void sendAcceptNotification(User sender, User receiver, Long matchingId) {
        Notification acceptNotification = notificationService.createRoommateAcceptNotification(sender.getName(), matchingId);
        notificationService.createUserNotification(receiver, acceptNotification);
        fcmMessageService.sendNotification(receiver, acceptNotification.getTitle(), acceptNotification.getBody());
    }
}
