package com.example.appcenter_project.domain.complaint.controller;

import com.example.appcenter_project.domain.complaint.dto.request.RequestComplaintDto;
import com.example.appcenter_project.domain.complaint.dto.request.RequestComplaintSearchDto;
import com.example.appcenter_project.domain.complaint.dto.response.ResponseComplaintDetailDto;
import com.example.appcenter_project.domain.complaint.dto.response.ResponseComplaintDto;
import com.example.appcenter_project.domain.complaint.dto.response.ResponseComplaintPageDto;
import com.example.appcenter_project.domain.complaint.service.ComplaintService;
import com.example.appcenter_project.global.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/complaints")
@RequiredArgsConstructor
public class ComplaintController implements ComplaintApiSpecification {

    private final ComplaintService complaintService;

    // 민원 등록
    @Override
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseComplaintDto> createComplaint(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart RequestComplaintDto dto, @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        ResponseComplaintDto response = complaintService.createComplaint(userDetails.getId(), dto, files);
        return ResponseEntity.ok(response);
    }

    // 내 민원 목록 조회 및 검색 (커서 페이징)
    @Override
    @GetMapping
    public ResponseEntity<ResponseComplaintPageDto> getAllComplaints(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ParameterObject @ModelAttribute RequestComplaintSearchDto dto) {
        return ResponseEntity.ok(complaintService.searchComplaints(userDetails.getId(), dto));
    }

    // 민원 상세 조회
    @Override
    @GetMapping("/{complaintId}")
    public ResponseEntity<ResponseComplaintDetailDto> getComplaint(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long complaintId,
            HttpServletRequest request) {
        return ResponseEntity.ok(complaintService.getComplaintDetailByUserId(userDetails.getId(), complaintId, request));
    }

    @Override
    @PutMapping(value = "/{complaintId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateComplaint(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long complaintId,
            @RequestPart RequestComplaintDto dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        complaintService.updateComplaint(userDetails.getId(), dto, complaintId, files);
        return ResponseEntity.status(OK).build();
    }

    @Override
    @DeleteMapping("/{complaintId}")
    public ResponseEntity<Void> deleteComplaint(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long complaintId) {
        complaintService.deleteComplaint(userDetails.getId(), complaintId);
        return ResponseEntity.status(NO_CONTENT).build();
    }
}
