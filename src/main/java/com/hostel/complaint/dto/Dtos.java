package com.hostel.complaint.dto;

import com.hostel.complaint.model.*;
import jakarta.validation.constraints.*;

public class Dtos {
    public record RegisterRequest(
            @NotBlank String name,
            @Email @NotBlank String email,
            @Size(min = 6, message = "Password must be at least 6 characters") String password,
            String phone, String hostelName, String roomNo) {}

    public record CreateUserRequest(
            @NotBlank String name,
            @Email @NotBlank String email,
            @Size(min = 6, message = "Password must be at least 6 characters") String password,
            @NotNull Role role, String phone, String hostelName, String roomNo) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record AuthResponse(String token, User user) {}
    public record ProfileRequest(@NotBlank String name, String phone, String hostelName, String roomNo) {}
    public record StatusRequest(@NotNull Status status) {}
    public record AssignRequest(@NotNull Long staffId) {}
    public record CommentRequest(@NotBlank @Size(max = 1000) String message) {}
    public record CategoryRequest(@NotBlank String name, String description) {}
}
