package com.example.appcenter_project.domain.complaint.controller;

import com.example.appcenter_project.domain.complaint.dto.request.RequestComplaintReplyDto;
import com.example.appcenter_project.domain.complaint.dto.request.RequestComplaintSearchDto;
import com.example.appcenter_project.domain.complaint.dto.request.RequestComplaintStatusDto;
import com.example.appcenter_project.domain.complaint.dto.response.ResponseComplaintDetailDto;
import com.example.appcenter_project.domain.complaint.dto.response.ResponseComplaintCursorPageDto;
import com.example.appcenter_project.domain.complaint.dto.response.ResponseComplaintListDto;
import com.example.appcenter_project.domain.complaint.dto.response.ResponseComplaintReplyDto;
import com.example.appcenter_project.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Admin Complaint API", description = "관리자 전용 민원 처리 API")
public interface AdminComplaintApiSpecification {

    @Operation(
            summary = "민원 목록 조회 및 검색 (관리자)",
            description = "검색 조건 생략 시 전체 범위에서 ID 내림차순으로 조회합니다. startDate/endDate는 등록일 기준 yyyy-MM-dd이며 종료일을 포함합니다. lastId/cursor는 이전 응답 마지막 ID, size는 생략 시 20입니다. 커서 페이징(content, hasNext, nextCursor) 객체로 반환합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseComplaintCursorPageDto.class)
                            )
                    )
            }
    )
    ResponseEntity<ResponseComplaintCursorPageDto> getAllComplaints(@ParameterObject RequestComplaintSearchDto dto);

    @Operation(
            summary = "민원 상세 조회 (관리자)",
            description = "민원 ID로 상세 정보를 조회합니다. 답변이 존재하면 함께 반환됩니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = ResponseComplaintDetailDto.class))
                    ),
                    @ApiResponse(responseCode = "404", description = "민원을 찾을 수 없음")
            }
    )
    ResponseEntity<ResponseComplaintDetailDto> getComplaintDetail(
            @PathVariable
            @Parameter(description = "민원 ID", required = true, example = "1")
            Long complaintId,

            @Parameter(description = "HTTP 요청 정보", hidden = true)
            HttpServletRequest request
    );

    @Operation(
            summary = "민원 답변 등록",
            description = "관리자가 민원에 대한 답변을 등록합니다. 등록 시 민원 상태는 자동으로 처리완료로 변경됩니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "등록 성공",
                            content = @Content(schema = @Schema(implementation = ResponseComplaintReplyDto.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
                    @ApiResponse(responseCode = "404", description = "민원 또는 사용자(관리자)를 찾을 수 없음"),
                    @ApiResponse(responseCode = "409", description = "이미 답변이 등록된 민원")
            }
    )
    ResponseEntity<ResponseComplaintReplyDto> addReply(
            @AuthenticationPrincipal
            @Parameter(description = "로그인 관리자", required = true)
            CustomUserDetails admin,

            @PathVariable
            @Parameter(description = "민원 ID", required = true, example = "1")
            Long complaintId,

            @RequestPart
            @Parameter(description = "답변 등록 정보", required = true)
            RequestComplaintReplyDto dto,

            @RequestPart(value = "files", required = false)
            @Parameter(description = "첨부 파일 목록 (선택사항)")
            List<MultipartFile> files
    );

    @Operation(
            summary = "민원 상태 변경",
            description = "관리자가 민원의 상태를 변경합니다. (예: 대기중 → 담당자 배정 → 처리중 → 처리완료 -> 반려)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
                    @ApiResponse(responseCode = "400", description = "잘못된 상태 값"),
                    @ApiResponse(responseCode = "404", description = "민원을 찾을 수 없음")
            }
    )
    ResponseEntity<Void> updateStatus(
            @PathVariable
            @Parameter(description = "민원 ID", required = true, example = "1")
            Long complaintId,

            @RequestBody
            @Parameter(description = "변경할 민원 상태", required = true)
            RequestComplaintStatusDto dto
    );

    @Operation(
            summary = "민원 답변 수정",
            description = "관리자가 등록한 민원 답변을 수정합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "답변 수정 성공"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
                    @ApiResponse(responseCode = "404", description = "민원 또는 답변을 찾을 수 없음"),
                    @ApiResponse(responseCode = "403", description = "권한 없음")
            }
    )
    ResponseEntity<ResponseComplaintReplyDto> updateReply(
            @AuthenticationPrincipal
            @Parameter(description = "로그인 관리자", required = true)
            CustomUserDetails admin,

            @PathVariable
            @Parameter(description = "민원 ID", required = true, example = "1")
            Long complaintId,

            @RequestPart
            @Parameter(description = "답변 수정 정보", required = true)
            RequestComplaintReplyDto dto,

            @RequestPart(value = "files", required = false)
            @Parameter(description = "첨부 파일 목록 (선택사항)")
            List<MultipartFile> files
    );

    @Operation(
            summary = "민원 답변 삭제",
            description = "관리자가 등록한 민원 답변을 삭제합니다.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "답변 삭제 성공"),
                    @ApiResponse(responseCode = "404", description = "민원 또는 답변을 찾을 수 없음"),
                    @ApiResponse(responseCode = "403", description = "권한 없음")
            }
    )
    ResponseEntity<Void> deleteReply(
            @PathVariable
            @Parameter(description = "민원 ID", required = true, example = "1")
            Long complaintId
    );

    @Operation(
            summary = "민원 담당자 배정",
            description = "관리자가 민원에 담당자를 배정합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "담당자 배정 성공"),
                    @ApiResponse(responseCode = "404", description = "민원을 찾을 수 없음"),
                    @ApiResponse(responseCode = "400", description = "잘못된 담당자 정보")
            }
    )
    ResponseEntity<Void> updateComplaintOfficer(
            @PathVariable
            @Parameter(description = "민원 ID", required = true, example = "1")
            Long complaintId,

            @PathVariable
            @Parameter(description = "담당자명", required = true, example = "김관리")
            String officer
    );

    @Operation(
            summary = "민원 목록 CSV 다운로드 (전체)",
            description = "등록된 모든 민원을 CSV 파일로 다운로드합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "CSV 다운로드 성공",
                            content = @Content(mediaType = "application/octet-stream")
                    )
            }
    )
    ResponseEntity<byte[]> exportComplaintsToCsv();

    @Operation(
            summary = "민원 목록 CSV 다운로드 (필터링)",
            description = "검색 조건에 맞는 민원을 CSV 파일로 다운로드합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "CSV 다운로드 성공",
                            content = @Content(mediaType = "application/octet-stream")
                    )
            }
    )
    ResponseEntity<byte[]> exportComplaintsToCsvWithFilter(
            @Parameter(description = "검색 조건", required = true)
            RequestComplaintSearchDto dto
    );
}
