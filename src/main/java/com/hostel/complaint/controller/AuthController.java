package com.hostel.complaint.controller;

import com.hostel.complaint.dto.Dtos.*;
import com.hostel.complaint.model.*;
import com.hostel.complaint.repository.UserRepository;
import com.hostel.complaint.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtUtil jwt) {
        this.users = users; this.encoder = encoder; this.jwt = jwt;
    }

    /** Public registration always creates a STUDENT. Other roles are created by an admin. */
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        User u = new User();
        u.setName(r.name().trim()); u.setEmail(email); u.setRole(Role.STUDENT);
        u.setPassword(encoder.encode(r.password()));
        u.setPhone(r.phone()); u.setHostelName(r.hostelName()); u.setRoomNo(r.roomNo());
        users.save(u);
        return new AuthResponse(jwt.generate(email), u);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        String email = r.email().trim().toLowerCase();
        User u = users.findByEmail(email).orElse(null);
        if (u == null || !encoder.matches(r.password(), u.getPassword()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect email or password");
        if (!"ACTIVE".equals(u.getStatus()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is deactivated. Contact the administrator");
        return new AuthResponse(jwt.generate(email), u);
    }

    @GetMapping("/me")
    public User me(Authentication a) {
        return users.findByEmail(a.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    @PutMapping("/profile")
    public User profile(Authentication a, @Valid @RequestBody ProfileRequest r) {
        User u = me(a);
        u.setName(r.name().trim()); u.setPhone(r.phone());
        u.setHostelName(r.hostelName()); u.setRoomNo(r.roomNo());
        return users.save(u);
    }
}
