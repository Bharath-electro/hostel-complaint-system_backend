package com.hostel.complaint.service;

import com.hostel.complaint.model.*;
import com.hostel.complaint.repository.*;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
public class NotificationService {
    private final NotificationRepository repo;
    private final UserRepository users;

    public NotificationService(NotificationRepository repo, UserRepository users) {
        this.repo = repo; this.users = users;
    }

    public void notify(User target, String message) {
        if (target == null) return;
        Notification n = new Notification();
        n.setUser(target);
        n.setMessage(message);
        repo.save(n);
    }

    /** Notify every active warden responsible for the student's hostel. */
    public void notifyWardens(User student, String message) {
        for (User w : users.findByRoleIn(java.util.List.of(Role.WARDEN))) {
            boolean sameHostel = w.getHostelName() == null || w.getHostelName().isBlank()
                    || Objects.equals(w.getHostelName(), student.getHostelName());
            if (sameHostel && "ACTIVE".equals(w.getStatus())) notify(w, message);
        }
    }
}
