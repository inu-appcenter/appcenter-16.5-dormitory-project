package com.example.appcenter_project.domain.complaint.controller;

import com.example.appcenter_project.domain.complaint.dto.request.RequestComplaintReplyDto;
import com.example.appcenter_project.domain.complaint.dto.request.RequestComplaintSearchDto;
import com.example.appcenter_project.domain.complaint.dto.request.RequestComplaintStatusDto;
import com.example.appcenter_project.domain.complaint.dto.response.ResponseComplaintDetailDto;
import com.example.appcenter_project.domain.complaint.dto.response.ResponseComplaintCursorPageDto;
import com.example.appcenter_project.domain.complaint.dto.response.ResponseComplaintListDto;
import com.example.appcenter_project.domain.complaint.dto.response.ResponseComplaintReplyDto;
import com.example.appcenter_project.domain.complaint.service.AdminComplaintService;
import com.example.appcenter_project.domain.complaint.service.ComplaintService;
import com.example.appcenter_project.global.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/admin/complaints")
@RequiredArgsConstructor
public class AdminComplaintController implements AdminComplaintApiSpecification {

    private final ComplaintService complaintService;         // 기존 조회 로직 재사용
    private final AdminComplaintService adminComplaintService; // 관리자 전용 기능

    // 민원 목록 조회 및 검색
    @GetMapping
    public ResponseEntity<ResponseComplaintCursorPageDto> getAllComplaints(@Valid @ParameterObject @ModelAttribute RequestComplaintSearchDto dto) {
        return ResponseEntity.ok(complaintService.searchComplaints(null, dto));
    }

    // 민원 상세 조회
    @GetMapping("/{complaintId}")
    public ResponseEntity<ResponseComplaintDetailDto> getComplaintDetail(
            @PathVariable Long complaintId, HttpServletRequest request
    ) {
        return ResponseEntity.ok(complaintService.getComplaintDetail(complaintId, request));
    }

    // 민원 답변 등록
    @PostMapping(value = "/{complaintId}/reply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseComplaintReplyDto> addReply(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable Long complaintId,
            @RequestPart RequestComplaintReplyDto dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> images
    ) {
        ResponseComplaintReplyDto response =
                adminComplaintService.addReply(admin.getId(), complaintId, dto, images);
        return ResponseEntity.ok(response);
    }

    // 민원 상태 변경
    @PatchMapping("/{complaintId}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long complaintId,
            @RequestBody RequestComplaintStatusDto dto
    ) {
        adminComplaintService.updateStatus(complaintId, dto);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{complaintId}/officer/{officer}")
    public ResponseEntity<Void> updateComplaintOfficer(
            @PathVariable Long complaintId,
            @PathVariable String officer
    ) {
        adminComplaintService.updateComplaintOfficer(complaintId, officer);
        return ResponseEntity.ok().build();
    }

    // 민원 답변 수정
    @PutMapping(value = "/{complaintId}/reply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseComplaintReplyDto> updateReply(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable Long complaintId,
            @RequestPart RequestComplaintReplyDto dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> images
    ) {
        adminComplaintService.updateReply(admin.getId(), complaintId, dto, images);
        return ResponseEntity.status(OK).build();
    }

    // 민원 답변 삭제
    @DeleteMapping("/{complaintId}")
    public ResponseEntity<Void> deleteReply(
            @PathVariable Long complaintId) {
        adminComplaintService.deleteReply(complaintId);
        return ResponseEntity.status(NO_CONTENT).build();
    }

    // 민원 목록 CSV 다운로드 (전체)
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportComplaintsToCsv() {
        byte[] csvData = complaintService.exportComplaintsToCsv();

        String fileName = "민원목록_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv; charset=UTF-8"));
        headers.set("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
        headers.setContentLength(csvData.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }

    // 민원 목록 CSV 다운로드 (필터링된)
    @GetMapping("/export/csv/search")
    public ResponseEntity<byte[]> exportComplaintsToCsvWithFilter(
            @Valid @ParameterObject @ModelAttribute RequestComplaintSearchDto dto
    ) {
        byte[] csvData = complaintService.exportComplaintsToCsv(dto);

        String fileName = "민원목록_필터링_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv; charset=UTF-8"));
        headers.set("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
        headers.setContentLength(csvData.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }


}
