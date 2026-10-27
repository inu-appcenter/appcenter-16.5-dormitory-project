package com.example.appcenter_project.domain.complaint.repository;

import com.example.appcenter_project.domain.complaint.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long>, JpaSpecificationExecutor<Complaint> {

    // 특정 사용자의 민원 목록 조회
    List<Complaint> findByUserId(Long userId);

    List<Complaint> findAllByOrderByCreatedDateDesc();

    Optional<Complaint> findByIdAndUserId(Long complaintId, Long userId);

}
