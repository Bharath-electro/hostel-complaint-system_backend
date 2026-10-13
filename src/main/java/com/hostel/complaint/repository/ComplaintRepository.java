package com.hostel.complaint.repository;

import com.hostel.complaint.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findAllByOrderByCreatedDateDesc();
    List<Complaint> findByUserOrderByCreatedDateDesc(User user);
    List<Complaint> findByAssignedToOrderByCreatedDateDesc(User staff);
    List<Complaint> findByUserHostelNameOrderByCreatedDateDesc(String hostelName);
    boolean existsByCategory(Category category);
}
