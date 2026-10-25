package com.example.appcenter_project.domain.complaint.service;

import com.example.appcenter_project.common.file.repository.AttachedFileRepository;
import com.example.appcenter_project.common.image.dto.ImageLinkDto;
import com.example.appcenter_project.common.image.enums.ImageType;
import com.example.appcenter_project.common.image.repository.ImageRepository;
import com.example.appcenter_project.common.image.service.ImageService;
import com.example.appcenter_project.domain.complaint.dto.request.RequestComplaintDto;
import com.example.appcenter_project.domain.complaint.dto.request.RequestComplaintSearchDto;
import com.example.appcenter_project.domain.complaint.dto.response.ResponseComplaintDto;
import com.example.appcenter_project.domain.complaint.dto.response.ResponseComplaintListDto;
import com.example.appcenter_project.domain.complaint.entity.Complaint;
import com.example.appcenter_project.domain.complaint.entity.ComplaintReply;
import com.example.appcenter_project.domain.complaint.enums.ComplaintStatus;
import com.example.appcenter_project.domain.complaint.enums.ComplaintType;
import com.example.appcenter_project.domain.complaint.enums.DormBuilding;
import com.example.appcenter_project.domain.complaint.repository.ComplaintRepository;
import com.example.appcenter_project.domain.notification.service.AdminComplaintNotificationService;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.domain.user.enums.DormType;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.shared.utils.CsvUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.appcenter_project.global.exception.ErrorCode.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ComplaintServiceTest {

    @Mock
    ComplaintRepository complaintRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    ImageRepository imageRepository;

    @Mock
    AttachedFileRepository attachedFileRepository;

    @Mock
    AdminComplaintNotificationService adminComplaintNotificationService;

    @Mock
    ImageService imageService;

    @Mock
    CsvUtils csvUtils;

    @InjectMocks
    ComplaintService complaintService;

    private Complaint buildMockComplaint() {
        Complaint complaint = mock(Complaint.class);
        when(complaint.getId()).thenReturn(1L);
        when(complaint.getTitle()).thenReturn("소음 민원");
        when(complaint.getContent()).thenReturn("소음이 심합니다");
        when(complaint.getType()).thenReturn(ComplaintType.NOISE);
        when(complaint.getDormType()).thenReturn(DormType.DORM_1);
        when(complaint.getBuilding()).thenReturn(DormBuilding.A);
        when(complaint.getFloor()).thenReturn("3층");
        when(complaint.getRoomNumber()).thenReturn("301호");
        when(complaint.getBedNumber()).thenReturn("1번");
        when(complaint.getSpecificLocation()).thenReturn("3층 복도");
        when(complaint.getIncidentDate()).thenReturn("2025-01-01");
        when(complaint.getIncidentTime()).thenReturn("14:00");
        when(complaint.isPrivacyAgreed()).thenReturn(true);
        when(complaint.getStatus()).thenReturn(ComplaintStatus.PENDING);
        when(complaint.getCreatedDate()).thenReturn(LocalDateTime.of(2025, 1, 1, 12, 0));
        User mockUser = mock(User.class);
        when(mockUser.getStudentNumber()).thenReturn("20250001");
        when(complaint.getUser()).thenReturn(mockUser);
        return complaint;
    }

    private RequestComplaintDto buildMockDto() {
        RequestComplaintDto dto = mock(RequestComplaintDto.class);
        when(dto.getTitle()).thenReturn("소음 민원");
        when(dto.getContent()).thenReturn("소음이 심합니다");
        when(dto.getType()).thenReturn("소음");
        when(dto.getDormType()).thenReturn("1기숙사");
        when(dto.getBuilding()).thenReturn("A동");
        when(dto.getFloor()).thenReturn("3층");
        when(dto.getRoomNumber()).thenReturn("301호");
        when(dto.getBedNumber()).thenReturn("1번");
        when(dto.getSpecificLocation()).thenReturn("3층 복도");
        when(dto.getIncidentDate()).thenReturn("2025-01-01");
        when(dto.getIncidentTime()).thenReturn("14:00");
        when(dto.isPrivacyAgreed()).thenReturn(true);
        return dto;
    }

    @Test
    @DisplayName("민원 등록 - 정상 등록")
    void createComplaint_정상_등록() {
        User mockUser = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        RequestComplaintDto dto = buildMockDto();
        Complaint savedComplaint = buildMockComplaint();
        when(complaintRepository.save(any(Complaint.class))).thenReturn(savedComplaint);

        ResponseComplaintDto result = complaintService.createComplaint(1L, dto, null);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("소음 민원");
        verify(complaintRepository).save(any(Complaint.class));
    }

    @Test
    @DisplayName("민원 등록 - 유저 없으면 예외")
    void createComplaint_유저없으면_예외() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> complaintService.createComplaint(99L, buildMockDto(), null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", USER_NOT_FOUND);
    }

    @Test
    @DisplayName("민원 등록 - 제목 누락시 예외")
    void createComplaint_제목누락_예외() {
        User mockUser = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        RequestComplaintDto dto = mock(RequestComplaintDto.class);
        when(dto.getTitle()).thenReturn(""); // 빈 제목
        when(dto.getContent()).thenReturn("소음이 심합니다");
        when(dto.getDormType()).thenReturn("1기숙사");
        when(dto.getBuilding()).thenReturn("A동");
        when(dto.getSpecificLocation()).thenReturn("3층 복도");
        when(dto.getIncidentDate()).thenReturn("2025-01-01");
        when(dto.getIncidentTime()).thenReturn("14:00");

        assertThatThrownBy(() -> complaintService.createComplaint(1L, dto, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", COMPLAINT_REQUIRED_FIELD_MISSING);
    }

    @Test
    @DisplayName("민원 등록 - 필수항목 null 시 예외")
    void createComplaint_필수항목null_예외() {
        User mockUser = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        RequestComplaintDto dto = mock(RequestComplaintDto.class);
        when(dto.getTitle()).thenReturn(null);

        assertThatThrownBy(() -> complaintService.createComplaint(1L, dto, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", COMPLAINT_REQUIRED_FIELD_MISSING);
    }

    @Test
    @DisplayName("민원 전체 조회 - 정상 반환")
    void getAllComplaints_정상_반환() {
        Complaint mockComplaint = buildMockComplaint();
        when(complaintRepository.findAllByOrderByCreatedDateDesc()).thenReturn(List.of(mockComplaint));

        List<ResponseComplaintListDto> result = complaintService.getAllComplaints();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("소음 민원");
    }

    @Test
    @DisplayName("민원 전체 조회 - 빈 목록이면 빈 리스트 반환")
    void getAllComplaints_빈목록_빈리스트() {
        when(complaintRepository.findAllByOrderByCreatedDateDesc()).thenReturn(List.of());

        List<ResponseComplaintListDto> result = complaintService.getAllComplaints();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자별 민원 조회 - 정상 반환")
    void getAllComplaintsUserId_정상_반환() {
        Complaint mockComplaint = buildMockComplaint();
        when(complaintRepository.findAllByUserIdOrderByCreatedDateDesc(1L))
                .thenReturn(List.of(mockComplaint));

        List<ResponseComplaintListDto> result = complaintService.getAllComplaintsUserId(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("사용자별 민원 조회 - 없으면 빈 리스트")
    void getAllComplaintsUserId_없으면_빈리스트() {
        when(complaintRepository.findAllByUserIdOrderByCreatedDateDesc(99L)).thenReturn(List.of());

        List<ResponseComplaintListDto> result = complaintService.getAllComplaintsUserId(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("민원 상세 조회(유저) - 없으면 예외")
    void getComplaintDetailByUserId_없으면_예외() {
        when(complaintRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                complaintService.getComplaintDetailByUserId(1L, 99L, mock(HttpServletRequest.class)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", COMPLAINT_NOT_FOUND);
    }

    @Test
    @DisplayName("민원 상세 조회(유저) - 답변 없을 때 정상 반환")
    void getComplaintDetailByUserId_답변없음_정상반환() {
        Complaint mockComplaint = buildMockComplaint();
        when(mockComplaint.getReply()).thenReturn(null);
        when(complaintRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockComplaint));
        when(imageService.findStaticImageUrls(any(ImageType.class), anyLong(), any(HttpServletRequest.class)))
                .thenReturn(List.of());

        var result = complaintService.getComplaintDetailByUserId(1L, 1L, mock(HttpServletRequest.class));

        assertThat(result).isNotNull();
        assertThat(result.getReply()).isNull();
    }

    @Test
    @DisplayName("관리자 민원 상세 조회 - 없으면 예외")
    void getComplaintDetail_없으면_예외() {
        when(complaintRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                complaintService.getComplaintDetail(99L, mock(HttpServletRequest.class)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", COMPLAINT_NOT_FOUND);
    }

    @Test
    @DisplayName("민원 수정 - 정상 수정")
    void updateComplaint_정상_수정() {
        Complaint mockComplaint = buildMockComplaint();
        when(complaintRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockComplaint));

        RequestComplaintDto dto = buildMockDto();
        complaintService.updateComplaint(1L, dto, 1L, null);

        verify(mockComplaint).update(dto);
    }

    @Test
    @DisplayName("민원 수정 - 본인 민원이 아니면 예외")
    void updateComplaint_소유자아님_예외() {
        when(complaintRepository.findByIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                complaintService.updateComplaint(99L, buildMockDto(), 1L, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", COMPLAINT_NOT_OWNED_BY_USER);
    }

    @Test
    @DisplayName("민원 삭제 - 정상 삭제")
    void deleteComplaint_정상_삭제() {
        complaintService.deleteComplaint(1L, 1L);

        verify(complaintRepository).deleteById(1L);
        verify(imageService).deleteImages(ImageType.COMPLAINT, 1L);
    }

    @Test
    @DisplayName("민원 검색 - 정상 반환")
    void searchComplaints_정상_반환() {
        Complaint first = buildMockComplaint();
        Complaint second = buildMockComplaint();
        when(complaintRepository.findAll(any(Specification.class))).thenReturn(List.of(first, second));

        RequestComplaintSearchDto searchDto = mock(RequestComplaintSearchDto.class);
        when(searchDto.getDormType()).thenReturn(null);
        when(searchDto.getOfficer()).thenReturn(null);
        when(searchDto.getStatus()).thenReturn(null);
        when(searchDto.getKeyword()).thenReturn(null);
        when(searchDto.getType()).thenReturn(null);
        when(searchDto.getBuilding()).thenReturn(null);
        when(searchDto.getFloor()).thenReturn(null);
        when(searchDto.getRoomNumber()).thenReturn(null);
        when(searchDto.getBedNumber()).thenReturn(null);

        List<ResponseComplaintListDto> result = complaintService.searchComplaints(null, searchDto);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("민원 검색 - 사용자 ID 있으면 해당 사용자 민원만 반환")
    void searchComplaints_userId_있으면_해당사용자만() {
        when(complaintRepository.findAll(any(Specification.class))).thenReturn(List.of());

        RequestComplaintSearchDto searchDto = mock(RequestComplaintSearchDto.class);
        when(searchDto.getDormType()).thenReturn(null);
        when(searchDto.getOfficer()).thenReturn(null);
        when(searchDto.getStatus()).thenReturn(null);
        when(searchDto.getKeyword()).thenReturn(null);
        when(searchDto.getType()).thenReturn(null);
        when(searchDto.getBuilding()).thenReturn(null);
        when(searchDto.getFloor()).thenReturn(null);
        when(searchDto.getRoomNumber()).thenReturn(null);
        when(searchDto.getBedNumber()).thenReturn(null);

        List<ResponseComplaintListDto> result = complaintService.searchComplaints(1L, searchDto);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("민원 CSV 내보내기 - 정상 반환")
    void exportComplaintsToCsv_정상_반환() {
        when(complaintRepository.findAllByOrderByCreatedDateDesc()).thenReturn(List.of());
        when(csvUtils.generateComplaintCsv(any())).thenReturn(new byte[]{});

        byte[] result = complaintService.exportComplaintsToCsv();

        assertThat(result).isNotNull();
        verify(csvUtils).generateComplaintCsv(any());
    }
}
