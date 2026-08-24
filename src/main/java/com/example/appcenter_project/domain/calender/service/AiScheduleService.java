package com.example.appcenter_project.domain.calender.service;

import com.example.appcenter_project.domain.announcement.entity.CrawledAnnouncement;
import com.example.appcenter_project.domain.calender.client.AiScheduleExtractClient;
import com.example.appcenter_project.domain.calender.dto.ai.AiScheduleExtractItem;
import com.example.appcenter_project.domain.calender.dto.ai.AiScheduleExtractResponse;
import com.example.appcenter_project.domain.calender.entity.Calender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jsoup.Jsoup;

import static org.apache.commons.lang3.StringUtils.abbreviate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiScheduleService {

    public enum ProcessResult { SUCCESS, NO_SCHEDULE, FAILED }

    private static final int TITLE_MAX_LENGTH = 100;
    private static final String DEFAULT_TITLE = "[일정]";
    private static final long MAX_SCHEDULE_DURATION_DAYS = 31;   // 31일 초과 일정은 제외

    private final AiScheduleExtractClient aiClient;
    private final AiCalendarPersistenceService persistenceService;

    public static final int CHANGE_DETECT_WEEKS = 2;

    public ProcessResult process(CrawledAnnouncement announcement) {
        Long id = announcement.getId();

        boolean titleBlank = announcement.getTitle() == null || announcement.getTitle().isBlank();
        boolean contentBlank = announcement.getContent() == null || announcement.getContent().isBlank();
        if (titleBlank && contentBlank) {
            persistenceService.markNoSchedule(id);
            return ProcessResult.NO_SCHEDULE;
        }

        String request = buildRequest(announcement);
        log.info("[AI-EXTRACT][{}] 입력 — title='{}', contentLength={}, contentPreview='{}'",
                id,
                nullSafe(announcement.getTitle()),
                nullSafe(announcement.getContent()).length(),
                abbreviate(nullSafe(announcement.getContent()), 300));

        try {
            AiScheduleExtractResponse response = aiClient.extract(id, request);

            if (isEmpty(response)) {
                persistenceService.markNoSchedule(id);
                return ProcessResult.NO_SCHEDULE;
            }

            List<Calender> calendars = buildCalendars(id, announcement.getTitle(), announcement.getLink(), response);

            if (calendars.isEmpty()) {
                persistenceService.markNoSchedule(id);
                return ProcessResult.NO_SCHEDULE;
            }

            persistenceService.saveSuccess(id, calendars);
            return ProcessResult.SUCCESS;

        } catch (Exception e) {
            log.warn("공지 {} AI 일정 추출 실패: {}", id, e.getMessage());
            try {
                persistenceService.markFailed(id, e.getMessage());
            } catch (Exception persistError) {
                log.error("공지 {} 실패 상태 저장 중 추가 오류 발생", id, persistError);
            }
            return ProcessResult.FAILED;
        }
    }

    private String abbreviate(String s, int max) {
        if (s == null) return "null";
        String flat = s.replace("\n", "\\n");
        return flat.length() <= max ? flat : flat.substring(0, max) + "...";
    }

    private boolean isEmpty(AiScheduleExtractResponse res) {
        return res == null || res.getData() == null || res.getData().isEmpty();
    }

    private String buildRequest(CrawledAnnouncement a) {
        String plainContent = Jsoup.parse(nullSafe(a.getContent())).text();
        return "[제목]\n" + nullSafe(a.getTitle())
                + "\n\n[내용]\n" + plainContent;
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private List<Calender> buildCalendars(Long id, String fallbackTitle, String link,
                                          AiScheduleExtractResponse res) {

        List<Calender> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (AiScheduleExtractItem item : res.getData()) {

            log.info("[AI-EXTRACT][{}] item 원본 — title='{}', start='{}', end='{}', desc={}자",
                    id, item.getTitle(), item.getStartDate(), item.getEndDate(),
                    item.getDescription() == null ? 0 : item.getDescription().length());

            LocalDate start = parse(item.getStartDate());
            if (start == null) {
                log.warn("[AI-EXTRACT][{}] item 스킵 — start_date 파싱 실패: '{}'", id, item.getStartDate());
                continue;
            }

            LocalDate end = Optional.ofNullable(parse(item.getEndDate())).orElse(start);
            if (end.isBefore(start)) {
                end = start; // 잘못된 종료일 → 단일 일정으로 처리
            }

            // 기간이 지나치게 긴(상시성) 일정은 캘린더를 더럽히므로 제외
            long durationDays = ChronoUnit.DAYS.between(start, end);
            if (durationDays > MAX_SCHEDULE_DURATION_DAYS) {
                log.warn("[AI-EXTRACT][{}] 기간 {}일 초과 — 일정 스킵: '{}' {}~{}",
                        id, durationDays, item.getTitle(), start, end);
                continue;
            }

            String rawTitle;
            if (item.getTitle() != null && !item.getTitle().isBlank()) {
                rawTitle = item.getTitle();
            } else {
                rawTitle = fallbackTitle;
                log.warn("[AI-EXTRACT][{}] AI title 빈 값 — 공지 제목으로 대체: '{}'", id, fallbackTitle);
            }
            if (rawTitle == null || rawTitle.isBlank()) {
                rawTitle = DEFAULT_TITLE;
                log.warn("[AI-EXTRACT][{}] 공지 제목도 빈 값 — 기본 제목 '{}' 사용", id, DEFAULT_TITLE);
            }

            String title = rawTitle;
            if (rawTitle.length() > TITLE_MAX_LENGTH) {
                title = rawTitle.substring(0, TITLE_MAX_LENGTH);
                log.warn("[AI-EXTRACT][{}] title {}자 → {}자로 잘림: '{}'",
                        id, rawTitle.length(), TITLE_MAX_LENGTH, title);
            }

            String key = title + "|" + start + "|" + end;
            if (!seen.add(key)) {
                log.info("[AI-EXTRACT][{}] 중복 item 제거 — '{}'", id, key);
                continue;
            }

            result.add(Calender.ofAiGenerated(start, end, title, link, item.getDescription(), id));
        }

        return result;
    }

    private LocalDate parse(String date) {
        if (date == null || date.isBlank()) return null;
        try {
            return LocalDate.parse(date);
        } catch (Exception e) {
            log.warn("AI 응답에 잘못된 날짜 형식: '{}'", date);
            return null;
        }
    }
}