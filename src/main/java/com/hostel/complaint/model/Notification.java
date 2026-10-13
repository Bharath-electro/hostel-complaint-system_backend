package com.hostel.complaint.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;
    @JsonIgnore @ManyToOne(optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(nullable = false, length = 500) private String message;
    private LocalDateTime createdDate;
    @Column(nullable = false) private String status = "UNREAD";

    @PrePersist void onCreate() { createdDate = LocalDateTime.now(); }
}
