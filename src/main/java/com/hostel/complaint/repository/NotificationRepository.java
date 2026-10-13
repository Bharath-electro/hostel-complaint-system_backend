package com.hostel.complaint.repository;

import com.hostel.complaint.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedDateDesc(User user);
}
