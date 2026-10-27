package com.example.appcenter_project.domain.complaint.controller;

import com.example.appcenter_project.domain.complaint.dto.request.RequestComplaintDto;
import com.example.appcenter_project.domain.complaint.dto.request.RequestComplaintSearchDto;
import com.example.appcenter_project.domain.complaint.dto.response.ResponseComplaintDetailDto;
import com.example.appcenter_project.domain.complaint.dto.response.ResponseComplaintDto;
import com.example.appcenter_project.domain.complaint.dto.response.ResponseComplaintPageDto;
import com.example.appcenter_project.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Complaint API", description = "민원 관련 API")
public interface ComplaintApiSpecification {

    @Operation(
            summary = "민원 등록",
            description = "로그인한 사용자가 민원을 등록합니다. 상태는 기본 대기중으로 저장됩니다. 파일 첨부가 가능합니다. " +
            "구체적 장소, 사건 발생 날짜, 사건 발생 시간은 필수 입력 항목입니다. " +
            "type : 소음,흡연,음주,호실 변경,공용공간",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "등록 성공",
                            content = @Content(schema = @Schema(implementation = ResponseComplaintDto.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
                    @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
            }
    )
    ResponseEntity<ResponseComplaintDto> createComplaint(
            @AuthenticationPrincipal
            @Parameter(description = "로그인 사용자", required = true)
            CustomUserDetails userDetails,

            @Valid @RequestPart
            @Parameter(description = "민원 등록 정보", required = true)
            RequestComplaintDto dto,

            @RequestPart(value = "files", required = false)
            @Parameter(description = "첨부 파일 목록 (선택사항)")
            List<MultipartFile> files
    );

    @Operation(
            summary = "내 민원 목록 조회 및 검색 (커서 페이징)",
            description = """
                    본인 민원을 검색 조건에 따라 **ID 내림차순**으로 조회합니다.

                    **등록일 검색**
                    - `startDate`, `endDate`: `yyyy-MM-dd` 형식
                    - 시작일과 종료일 모두 포함합니다.
                    - 생략한 날짜 조건은 적용하지 않습니다.
                    - 모두 생략하면 전체 조회합니다.

                    **커서 페이지네이션**
                    - `size`: 조회 개수 (기본 20개)
                    - `lastId`: 이전 응답의 `nextCursor` 값 (첫 요청 시 생략)

                    **응답**
                    - `content`: 민원 목록
                    - `hasNext`: 다음 페이지 존재 여부
                    - `nextCursor`: 다음 요청의 `lastId`에 전달할 값

                    마지막 페이지에서는 `hasNext`가 `false`, `nextCursor`가 `null`입니다.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseComplaintPageDto.class)
                            )
                    )
            }
    )
    ResponseEntity<ResponseComplaintPageDto> getAllComplaints(
            @AuthenticationPrincipal
            @Parameter(description = "로그인 사용자", required = true)
            CustomUserDetails userDetails,
            @ParameterObject RequestComplaintSearchDto dto
    );

    @Operation(
            summary = "민원 상세 조회",
            description = "민원 ID로 상세 정보를 조회합니다. 본인이 작성한 민원만 조회 가능하며, 답변이 존재하면 함께 반환됩니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = ResponseComplaintDetailDto.class))
                    ),
                    @ApiResponse(responseCode = "404", description = "민원을 찾을 수 없음"),
                    @ApiResponse(responseCode = "403", description = "본인의 민원이 아님")
            }
    )
    ResponseEntity<ResponseComplaintDetailDto> getComplaint(
            @AuthenticationPrincipal
            @Parameter(description = "로그인 사용자", required = true)
            CustomUserDetails userDetails,

            @PathVariable
            @Parameter(description = "민원 ID", required = true, example = "1")
            Long complaintId,

            @Parameter(description = "HTTP 요청 정보", hidden = true)
            HttpServletRequest request
    );

    @Operation(
            summary = "민원 수정",
            description = "본인이 작성한 민원을 수정합니다. 처리 완료된 민원은 수정할 수 없습니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "수정 성공"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
                    @ApiResponse(responseCode = "404", description = "민원을 찾을 수 없음"),
                    @ApiResponse(responseCode = "403", description = "본인의 민원이 아니거나 수정할 수 없는 상태")
            }
    )
    ResponseEntity<Void> updateComplaint(
            @AuthenticationPrincipal
            @Parameter(description = "로그인 사용자", required = true)
            CustomUserDetails userDetails,

            @PathVariable
            @Parameter(description = "민원 ID", required = true, example = "1")
            Long complaintId,

            @RequestPart
            @Parameter(description = "민원 수정 정보", required = true)
            RequestComplaintDto dto,

            @RequestPart(value = "files", required = false)
            @Parameter(description = "첨부 파일 목록 (선택사항)")
            List<MultipartFile> files
    );

    @Operation(
            summary = "민원 삭제",
            description = "본인이 작성한 민원을 삭제합니다. 처리 완료된 민원은 삭제할 수 없습니다.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "삭제 성공"),
                    @ApiResponse(responseCode = "404", description = "민원을 찾을 수 없음"),
                    @ApiResponse(responseCode = "403", description = "본인의 민원이 아니거나 삭제할 수 없는 상태")
            }
    )
    ResponseEntity<Void> deleteComplaint(
            @AuthenticationPrincipal
            @Parameter(description = "로그인 사용자", required = true)
            CustomUserDetails userDetails,

            @PathVariable
            @Parameter(description = "민원 ID", required = true, example = "1")
            Long complaintId
    );
}
