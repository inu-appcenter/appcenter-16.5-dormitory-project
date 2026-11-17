package com.example.appcenter_project.domain.complaint.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ResponseComplaintPageDto {
    @Schema(description = "ID 내림차순 민원 목록")
    private List<ResponseComplaintListDto> content;

    @Schema(description = "다음 페이지 존재 여부")
    private boolean hasNext;

    @Schema(description = "다음 요청의 lastId에 전달할 값. 마지막 페이지에서는 null", nullable = true)
    private Long nextCursor;
}
