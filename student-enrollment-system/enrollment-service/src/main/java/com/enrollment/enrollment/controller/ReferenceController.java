package com.enrollment.enrollment.controller;
import com.enrollment.enrollment.service.EnrollmentService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/references")
@RequiredArgsConstructor
public class ReferenceController {
    private final EnrollmentService service;
    @PostMapping("/students/{id}/close")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    void student(@PathVariable @Positive Long id) { service.closeStudent(id); }
    @PostMapping("/courses/{id}/close")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    void course(@PathVariable @Positive Long id) { service.closeCourse(id); }
}
