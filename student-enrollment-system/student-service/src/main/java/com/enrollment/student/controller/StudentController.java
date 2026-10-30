package com.enrollment.student.controller;

import com.enrollment.student.dto.*;
import com.enrollment.student.service.StudentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.createStudent(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<StudentResponse>> getAll() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.token.claims['student_id']")
    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @PreAuthorize("hasRole('ADMIN') or #cnie == authentication.token.claims['cnie']")
    @GetMapping("/cnie/{cnie}")
    public ResponseEntity<StudentResponse> getByCnie(@PathVariable @Pattern(regexp = "^[A-Z]{1,2}[0-9]{4,8}$") String cnie) {
        return ResponseEntity.ok(studentService.getStudentByCnie(cnie));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<StudentResponse> update(@PathVariable @Positive Long id, @Valid @RequestBody StudentRequest request) {
        return ResponseEntity.ok(studentService.updateStudent(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }
}
