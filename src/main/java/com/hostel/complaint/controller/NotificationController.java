package com.hostel.complaint.controller;

import com.hostel.complaint.model.*;
import com.hostel.complaint.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationRepository repo;
    private final UserRepository users;

    public NotificationController(NotificationRepository repo, UserRepository users) {
        this.repo = repo; this.users = users;
    }

    private User me(Authentication a) {
        return users.findByEmail(a.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    @GetMapping
    public List<Notification> mine(Authentication a) { return repo.findByUserOrderByCreatedDateDesc(me(a)); }

    @PutMapping("/{id}/read")
    public void read(Authentication a, @PathVariable Long id) {
        User u = me(a);
        repo.findById(id).filter(n -> n.getUser().getUserId().equals(u.getUserId())).ifPresent(n -> {
            n.setStatus("READ"); repo.save(n);
        });
    }

    @PutMapping("/read-all")
    public void readAll(Authentication a) {
        List<Notification> list = repo.findByUserOrderByCreatedDateDesc(me(a));
        list.forEach(n -> n.setStatus("READ"));
        repo.saveAll(list);
    }
}
