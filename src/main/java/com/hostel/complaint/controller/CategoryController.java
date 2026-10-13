package com.hostel.complaint.controller;

import com.hostel.complaint.dto.Dtos.CategoryRequest;
import com.hostel.complaint.model.Category;
import com.hostel.complaint.repository.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryRepository categories;
    private final ComplaintRepository complaints;

    public CategoryController(CategoryRepository categories, ComplaintRepository complaints) {
        this.categories = categories; this.complaints = complaints;
    }

    @GetMapping
    public List<Category> all() { return categories.findAll(); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Category create(@Valid @RequestBody CategoryRequest r) {
        if (categories.existsByName(r.name().trim()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This category already exists");
        Category c = new Category();
        c.setName(r.name().trim()); c.setDescription(r.description());
        return categories.save(c);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        Category c = categories.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        if (complaints.existsByCategory(c))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This category has complaints and cannot be deleted");
        categories.delete(c);
    }
}
