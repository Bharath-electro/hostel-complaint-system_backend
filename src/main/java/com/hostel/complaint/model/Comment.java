package com.hostel.complaint.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter @Setter @NoArgsConstructor
public class Comment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;
    @JsonIgnore @ManyToOne(optional = false) @JoinColumn(name = "complaint_id") private Complaint complaint;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(nullable = false, length = 1000) private String message;
    private LocalDateTime createdDate;

    @PrePersist void onCreate() { createdDate = LocalDateTime.now(); }
}
