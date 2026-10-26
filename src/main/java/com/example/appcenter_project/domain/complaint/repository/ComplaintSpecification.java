package com.example.appcenter_project.domain.complaint.repository;

import com.example.appcenter_project.domain.complaint.entity.Complaint;
import com.example.appcenter_project.domain.complaint.enums.ComplaintStatus;
import com.example.appcenter_project.domain.complaint.enums.ComplaintType;
import com.example.appcenter_project.domain.complaint.enums.DormBuilding;
import com.example.appcenter_project.domain.user.enums.DormType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class ComplaintSpecification {

    public static Specification<Complaint> hasDormType(DormType dormType) {
        return (root, query, cb) ->
                dormType == null ? null : cb.equal(root.get("dormType"), dormType);
    }

    public static Specification<Complaint> hasOfficer(String officer) {
        return (root, query, cb) ->
                (officer == null || officer.isBlank()) ? null : cb.like(root.get("officer"), "%" + officer + "%");
    }


    public static Specification<Complaint> hasStatus(ComplaintStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Complaint> hasKeyword(String keyword) {
        return (root, query, cb) ->
                (keyword == null || keyword.isBlank()) ? null :
                        cb.or(
                                cb.like(root.get("title"), "%" + keyword + "%"),
                                cb.like(root.get("content"), "%" + keyword + "%")
                        );
    }

    public static Specification<Complaint> hasType(ComplaintType type) {
        return (root, query, cb) ->
                type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<Complaint> hasBuilding(DormBuilding building) {
        return (root, query, cb) ->
                building == null ? null : cb.equal(root.get("building"), building);
    }

    public static Specification<Complaint> hasFloor(String floor) {
        return (root, query, cb) ->
                (floor == null || floor.isBlank()) ? null : cb.equal(root.get("floor"), floor);
    }

    public static Specification<Complaint> hasRoomNumber(String roomNumber) {
        return (root, query, cb) ->
                (roomNumber == null || roomNumber.isBlank()) ? null : cb.equal(root.get("roomNumber"), roomNumber);
    }

    public static Specification<Complaint> hasBedNumber(String bedNumber) {
        return (root, query, cb) ->
                (bedNumber == null || bedNumber.isBlank()) ? null : cb.equal(root.get("bedNumber"), bedNumber);
    }

    public static Specification<Complaint> createdOnOrAfter(LocalDate startDate) {
        return (root, query, cb) -> startDate == null ? null
                : cb.greaterThanOrEqualTo(root.get("createdDate"), startDate.atStartOfDay());
    }

    public static Specification<Complaint> createdOnOrBefore(LocalDate endDate) {
        return (root, query, cb) -> endDate == null ? null
                : cb.lessThan(root.get("createdDate"), endDate.plusDays(1).atStartOfDay());
    }

    public static Specification<Complaint> beforeId(Long lastId) {
        return (root, query, cb) -> lastId == null ? null : cb.lessThan(root.get("id"), lastId);
    }
}
