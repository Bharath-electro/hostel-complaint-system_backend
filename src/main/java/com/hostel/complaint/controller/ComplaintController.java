package com.hostel.complaint.controller;

import com.hostel.complaint.dto.Dtos.*;
import com.hostel.complaint.model.*;
import com.hostel.complaint.repository.*;
import com.hostel.complaint.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {
    private final ComplaintRepository complaints;
    private final CommentRepository comments;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final NotificationService notifier;
    @Value("${app.upload-dir}") private String uploadDir;

    public ComplaintController(ComplaintRepository complaints, CommentRepository comments,
                               CategoryRepository categories, UserRepository users, NotificationService notifier) {
        this.complaints = complaints; this.comments = comments;
        this.categories = categories; this.users = users; this.notifier = notifier;
    }

    // ---------- helpers ----------
    private User me(Authentication a) {
        return users.findByEmail(a.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    private List<Complaint> visible(User u) {
        return switch (u.getRole()) {
            case STUDENT -> complaints.findByUserOrderByCreatedDateDesc(u);
            case STAFF -> complaints.findByAssignedToOrderByCreatedDateDesc(u);
            case WARDEN -> blank(u.getHostelName()) ? complaints.findAllByOrderByCreatedDateDesc()
                    : complaints.findByUserHostelNameOrderByCreatedDateDesc(u.getHostelName());
            case ADMIN -> complaints.findAllByOrderByCreatedDateDesc();
        };
    }

    private boolean canView(User u, Complaint c) {
        return switch (u.getRole()) {
            case ADMIN -> true;
            case STUDENT -> c.getUser().getUserId().equals(u.getUserId());
            case STAFF -> c.getAssignedTo() != null && c.getAssignedTo().getUserId().equals(u.getUserId());
            case WARDEN -> blank(u.getHostelName()) || Objects.equals(u.getHostelName(), c.getUser().getHostelName());
        };
    }

    private Complaint load(Long id, User u) {
        Complaint c = complaints.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found"));
        if (!canView(u, c)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this complaint");
        return c;
    }

    private static String pretty(Status s) { return s.name().replace('_', ' ').toLowerCase(); }

    private static String csv(Object v) {
        String s = v == null ? "" : v.toString().replace("\"", "\"\"");
        return "\"" + s + "\"";
    }

    // ---------- endpoints ----------
    @GetMapping
    public List<Complaint> list(Authentication a, @RequestParam(required = false) Status status) {
        return visible(me(a)).stream().filter(c -> status == null || c.getStatus() == status).toList();
    }

    @GetMapping("/stats")
    public Map<String, Long> stats(Authentication a) {
        List<Complaint> list = visible(me(a));
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("total", (long) list.size());
        for (Status s : Status.values())
            m.put(s.name(), list.stream().filter(c -> c.getStatus() == s).count());
        return m;
    }

    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('WARDEN','ADMIN')")
    public ResponseEntity<String> report(Authentication a) {
        StringBuilder sb = new StringBuilder("ID,Student,Hostel,Room,Category,Location,Description,Status,Assigned To,Created,Updated\n");
        for (Complaint c : visible(me(a))) {
            sb.append(String.join(",", csv(c.getComplaintId()), csv(c.getUser().getName()),
                    csv(c.getUser().getHostelName()), csv(c.getUser().getRoomNo()),
                    csv(c.getCategory().getName()), csv(c.getLocation()), csv(c.getDescription()),
                    csv(c.getStatus()), csv(c.getAssignedTo() == null ? "" : c.getAssignedTo().getName()),
                    csv(c.getCreatedDate()), csv(c.getUpdatedDate()))).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=complaints-report.csv")
                .contentType(MediaType.parseMediaType("text/csv")).body(sb.toString());
    }

    @GetMapping("/{id}")
    public Complaint get(Authentication a, @PathVariable Long id) { return load(id, me(a)); }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public Complaint create(Authentication a,
                            @RequestParam Long categoryId,
                            @RequestParam String location,
                            @RequestParam String description,
                            @RequestParam(required = false) MultipartFile image) throws IOException {
        if (blank(location) || blank(description))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location and description are required");
        User student = me(a);
        Complaint c = new Complaint();
        c.setUser(student);
        c.setCategory(categories.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown category")));
        c.setLocation(location.trim());
        c.setDescription(description.trim());
        if (image != null && !image.isEmpty()) c.setImage(storeImage(image));
        complaints.save(c);
        notifier.notifyWardens(student, "New complaint #" + c.getComplaintId() + " (" + c.getCategory().getName()
                + ") from " + student.getName());
        return c;
    }

    private String storeImage(MultipartFile file) throws IOException {
        String type = file.getContentType();
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
        if (type == null || !type.startsWith("image/") || !List.of(".jpg", ".jpeg", ".png", ".gif", ".webp").contains(ext))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPG, PNG, GIF or WEBP images are allowed");
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);
        String stored = UUID.randomUUID() + ext;
        Files.copy(file.getInputStream(), dir.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
        return stored;
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('WARDEN','STAFF','ADMIN')")
    public Complaint updateStatus(Authentication a, @PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        Complaint c = load(id, me(a));
        c.setStatus(r.status());
        complaints.save(c);
        notifier.notify(c.getUser(), "Your complaint #" + id + " is now " + pretty(r.status()));
        return c;
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('WARDEN','ADMIN')")
    public Complaint assign(Authentication a, @PathVariable Long id, @Valid @RequestBody AssignRequest r) {
        Complaint c = load(id, me(a));
        User staff = users.findById(r.staffId())
                .filter(u -> u.getRole() == Role.STAFF && "ACTIVE".equals(u.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an active maintenance staff member"));
        c.setAssignedTo(staff);
        complaints.save(c);
        notifier.notify(staff, "Complaint #" + id + " (" + c.getCategory().getName() + ", " + c.getLocation() + ") was assigned to you");
        notifier.notify(c.getUser(), "Your complaint #" + id + " was assigned to " + staff.getName());
        return c;
    }

    @GetMapping("/{id}/comments")
    public List<Comment> comments(Authentication a, @PathVariable Long id) {
        return comments.findByComplaintOrderByCreatedDateAsc(load(id, me(a)));
    }

    @PostMapping("/{id}/comments")
    public Comment addComment(Authentication a, @PathVariable Long id, @Valid @RequestBody CommentRequest r) {
        User u = me(a);
        Complaint c = load(id, u);
        Comment cm = new Comment();
        cm.setComplaint(c); cm.setUser(u); cm.setMessage(r.message().trim());
        comments.save(cm);
        String text = u.getName() + " commented on complaint #" + id;
        if (u.getRole() == Role.STUDENT) {
            if (c.getAssignedTo() != null) notifier.notify(c.getAssignedTo(), text);
            else notifier.notifyWardens(u, text);
        } else {
            notifier.notify(c.getUser(), text);
        }
        return cm;
    }
}
