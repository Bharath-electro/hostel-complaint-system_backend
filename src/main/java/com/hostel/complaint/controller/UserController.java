package com.hostel.complaint.controller;

import com.hostel.complaint.dto.Dtos.CreateUserRequest;
import com.hostel.complaint.model.*;
import com.hostel.complaint.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserController(UserRepository users, PasswordEncoder encoder) {
        this.users = users; this.encoder = encoder;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> all() { return users.findAll(); }

    /** Maintenance staff list used by wardens when assigning complaints. */
    @GetMapping("/staff")
    @PreAuthorize("hasAnyRole('WARDEN','ADMIN')")
    public List<User> staff() {
        return users.findByRoleIn(List.of(Role.STAFF)).stream().filter(u -> "ACTIVE".equals(u.getStatus())).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public User create(@Valid @RequestBody CreateUserRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        User u = new User();
        u.setName(r.name().trim()); u.setEmail(email); u.setRole(r.role());
        u.setPassword(encoder.encode(r.password()));
        u.setPhone(r.phone()); u.setHostelName(r.hostelName()); u.setRoomNo(r.roomNo());
        return users.save(u);
    }

    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public User toggle(Authentication a, @PathVariable Long id) {
        User u = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (u.getEmail().equals(a.getName()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot deactivate your own account");
        u.setStatus("ACTIVE".equals(u.getStatus()) ? "INACTIVE" : "ACTIVE");
        return users.save(u);
    }
}
