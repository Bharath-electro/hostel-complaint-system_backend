package com.hostel.complaint.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String email;
    @JsonIgnore @Column(nullable = false) private String password;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role;
    private String phone;
    private String hostelName;
    private String roomNo;
    @Column(nullable = false) private String status = "ACTIVE";
}
