package com.example.appcenter_project.domain.announcement.dto.response;

import com.example.appcenter_project.domain.announcement.entity.Announcement;
import com.example.appcenter_project.domain.announcement.entity.CrawledAnnouncement;
import com.example.appcenter_project.domain.announcement.entity.ManualAnnouncement;
import com.example.appcenter_project.domain.announcement.enums.AnnouncementCategory;
import com.example.appcenter_project.domain.announcement.enums.AnnouncementType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import org.jsoup.Jsoup;

import java.time.LocalDateTime;

@Schema(description = "공지사항 목록 응답 DTO")
@Builder
@Getter
public class ResponseAnnouncementDto {

    @Schema(description = "공지사항 ID", example = "1")
    private Long id;

    @Schema(description = "카테고리")
    private AnnouncementCategory category;

    @Schema(description = "작성 타입")
    private AnnouncementType type;

    @Schema(description = "공지사항 제목", example = "기숙사 공지사항")
    private String title;

    @Schema(description = "공지사항 내용 (HTML 태그 제거 후 최대 150자)", example = "기숙사 생활 관련 중요한 공지사항입니다. 자세한 내용은...")
    private String content;

    @Schema(description = "생성일", example = "2025-08-05")
    private LocalDateTime createdDate;

    @Schema(description = "수정일", example = "2025-08-05")
    private LocalDateTime updatedDate;

    @Schema(description = "긴급", example = "true")
    private boolean isEmergency;

    @Builder.Default
    private int viewCount = 0;


    public static ResponseAnnouncementDto from(Announcement announcement) {

        // 직접 작성한 공지사항
        if (announcement instanceof ManualAnnouncement) {
            ManualAnnouncement manualAnnouncement = (ManualAnnouncement) announcement;
            String truncatedContent = manualAnnouncement.getContent();

            if (truncatedContent != null) {
                // html 태그 제거 후 텍스트만 추출
                truncatedContent = Jsoup.parse(truncatedContent).text();
                if (truncatedContent.length() > 150) {
                    truncatedContent = truncatedContent.substring(0, 150);
                }
            }

            return ResponseAnnouncementDto.builder()
                    .id(manualAnnouncement.getId())
                    .category(manualAnnouncement.getAnnouncementCategory())
                    .title(manualAnnouncement.getTitle())
                    .content(truncatedContent)
                    .createdDate(manualAnnouncement.getCreatedDate())
                    .updatedDate(manualAnnouncement.getModifiedDate())
                    .isEmergency(manualAnnouncement.isEmergency())
                    .viewCount(manualAnnouncement.getViewCount())
                    .type(manualAnnouncement.getAnnouncementType())
                    .build();
        }

        // 크롤링 공지
        if (announcement instanceof CrawledAnnouncement) {
            CrawledAnnouncement crawledAnnouncement = (CrawledAnnouncement) announcement;
            String truncatedContent = crawledAnnouncement.getContent();
            if (truncatedContent != null) {
                truncatedContent = Jsoup.parse(truncatedContent).text();
                if (truncatedContent.length() > 150) {
                    truncatedContent = truncatedContent.substring(0, 150);
                }
            }

            return ResponseAnnouncementDto.builder()
                    .id(crawledAnnouncement.getId())
                    .category(crawledAnnouncement.getAnnouncementCategory())
                    .title(crawledAnnouncement.getTitle())
                    .content(truncatedContent)
                    .createdDate(crawledAnnouncement.getCreatedDate())
                    .updatedDate(null)
                    .isEmergency(false)
                    .viewCount(crawledAnnouncement.getViewCount())
                    .type(crawledAnnouncement.getAnnouncementType())
                    .build();
        }
        return null;
    }
}
