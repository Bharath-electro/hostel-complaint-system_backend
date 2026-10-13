package com.hostel.complaint.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
@Getter @Setter @NoArgsConstructor
public class Complaint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long complaintId;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id") private User user;
    @ManyToOne(optional = false) @JoinColumn(name = "category_id") private Category category;
    @Column(nullable = false) private String location;
    @Column(nullable = false, length = 2000) private String description;
    private String image;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.SUBMITTED;
    @ManyToOne @JoinColumn(name = "assigned_to") private User assignedTo;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    @PrePersist void onCreate() { createdDate = updatedDate = LocalDateTime.now(); }
    @PreUpdate void onUpdate() { updatedDate = LocalDateTime.now(); }
}
