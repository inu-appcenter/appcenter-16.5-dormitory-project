package com.example.appcenter_project.domain.complaint.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "민원 목록 커서 기반 페이징 응답")
public class ResponseComplaintCursorPageDto {

    @Schema(description = "민원 목록 데이터")
    private List<ResponseComplaintListDto> content;

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;

    @Schema(description = "다음 요청에 사용할 커서 ID (hasNext가 false이면 null)", example = "21")
    private Long nextCursor;
}
