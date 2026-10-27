package com.example.appcenter_project.domain.complaint.dto.request;

import com.example.appcenter_project.domain.complaint.enums.ComplaintStatus;
import com.example.appcenter_project.domain.complaint.enums.ComplaintType;
import com.example.appcenter_project.domain.user.enums.DormType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class RequestComplaintSearchDto {
    private DormType dormType;
    private String officer;
    private ComplaintStatus status;
    private String keyword;
    private String building;
    private String floor;
    private String bedNumber;
    private String roomNumber;

    @Schema(description = "민원 유형 (NOISE: 소음, SMOKING: 흡연, DRINKING: 음주, ROOMMATE_CHANGE: 호실 변경, COMMON_SPACE: 공용공간)")
    private ComplaintType type;

    @Schema(description = "등록일 시작 날짜 (포함)", example = "2026-10-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @Schema(description = "등록일 종료 날짜 (포함)", example = "2026-10-25")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @Positive
    @Schema(description = "이전 응답의 nextCursor. 첫 요청 시 생략. (CSV에서는 미적용)")
    private Long lastId;

    @NotNull
    @Schema(description = "민원 조회 개수. 생략 시 20개 조회. (CSV에서는 미적용)", defaultValue = "20")
    private Integer size = 20;

    @AssertTrue(message = "시작일은 종료일 이후일 수 없으며 종료일은 다음 날로 변환 가능해야 합니다.")
    @Schema(hidden = true)
    public boolean isDateRangeValid() {
        return (endDate == null || !endDate.equals(LocalDate.MAX))
                && (startDate == null || endDate == null || !startDate.isAfter(endDate));
    }
}
