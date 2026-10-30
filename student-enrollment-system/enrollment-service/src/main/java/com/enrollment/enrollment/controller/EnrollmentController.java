package com.enrollment.enrollment.controller;

import com.enrollment.enrollment.dto.*;
import com.enrollment.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

  

    @PostMapping
    public ResponseEntity<EnrollmentResponseDTO> enroll(@Valid @RequestBody EnrollmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.enrollStudent(request));
    }

    @GetMapping("/student/{cnie}")
    public ResponseEntity<List<EnrollmentResponseDTO>> getByStudentCnie(@PathVariable @Pattern(regexp = "^[A-Z]{1,2}[0-9]{4,8}$") String cnie) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByStudentCnie(cnie));
    }

    @DeleteMapping("/{enrollmentId}")
    public ResponseEntity<Void> cancel(@PathVariable @Positive Long enrollmentId) {
        enrollmentService.cancelEnrollment(enrollmentId);
        return ResponseEntity.noContent().build();
    }

}
