package com.enrollment.enrollment.service;

import com.enrollment.enrollment.client.*;
import com.enrollment.enrollment.dto.*;
import com.enrollment.enrollment.entity.Enrollment;
import com.enrollment.enrollment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {
    private static final int MAX_STUDENTS_PER_COURSE = 3;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentClient studentClient;
    private final CourseClient courseClient;
    private final EnrollmentReferenceRepository references;
    private final Clock clock;

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or #request.studentCnie == authentication.token.claims['cnie']")
    public EnrollmentResponseDTO enrollStudent(EnrollmentRequest request) {
        StudentDTO student = studentClient.getStudentByCnie(request.getStudentCnie());
        checkStudentOwnership(student.getId());
        // Always lock student before course; locks last until this transaction commits.
        if (references.lockStudent(student.getId()).isClosed()) {
            throw new ResponseStatusException(CONFLICT, "This student is being deleted or has been deleted");
        }
        if (references.lockCourse(request.getCourseId()).isClosed()) {
            throw new ResponseStatusException(CONFLICT, "This course is being deleted or has been deleted");
        }
        CourseDTO course = courseClient.getCourseById(request.getCourseId());
        if (enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            throw new ResponseStatusException(CONFLICT, "Student is already enrolled in this course");
        }
        if (enrollmentRepository.countByCourseId(course.getId()) >= MAX_STUDENTS_PER_COURSE) {
            throw new ResponseStatusException(CONFLICT, "Course is full (max 3 students)");
        }
        Enrollment saved = enrollmentRepository.saveAndFlush(Enrollment.builder()
            .studentId(student.getId()).courseId(course.getId()).enrollmentDate(clock.instant()).build());
        return buildResponseDTO(saved, student.getCnie(), course);
    }

    @PreAuthorize("hasRole('ADMIN') or #cnie == authentication.token.claims['cnie']")
    public List<EnrollmentResponseDTO> getEnrollmentsByStudentCnie(String cnie) {
        StudentDTO student = studentClient.getStudentByCnie(cnie);
        return enrollmentRepository.findByStudentId(student.getId()).stream()
            .map(e -> buildResponseDTO(e, cnie, courseClient.getCourseById(e.getCourseId()))).toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public void cancelEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findLockedById(enrollmentId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Enrollment not found"));
        checkStudentOwnership(enrollment.getStudentId());
        if (!isDeletable(enrollment.getEnrollmentDate())) {
            throw new ResponseStatusException(CONFLICT, "Cancellation window has passed (24 hours)");
        }
        enrollmentRepository.delete(enrollment);
        enrollmentRepository.flush();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void closeStudent(Long id) {
        var student = references.lockStudent(id);
        if (enrollmentRepository.existsByStudentId(id)) {
            throw new ResponseStatusException(CONFLICT, "Student has active enrollments");
        }
        student.setClosed(true);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void closeCourse(Long id) {
        var course = references.lockCourse(id);
        if (enrollmentRepository.existsByCourseId(id)) {
            throw new ResponseStatusException(CONFLICT, "Course has active enrollments");
        }
        course.setClosed(true);
    }

    private void checkStudentOwnership(Long studentId) {
        JwtAuthenticationToken authentication =
            (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        boolean admin = authentication.getAuthorities().contains(
            new SimpleGrantedAuthority("ROLE_ADMIN"));
        if (!admin && !studentId.toString().equals(authentication.getToken().getClaimAsString("student_id"))) {
            throw new AccessDeniedException("You can only access your own enrollments");
        }
    }

    private boolean isDeletable(Instant enrolledAt) {
        return clock.instant().isBefore(enrolledAt.plus(Duration.ofHours(24)));
    }

    private EnrollmentResponseDTO buildResponseDTO(Enrollment enrollment, String cnie, CourseDTO course) {
        return EnrollmentResponseDTO.builder().enrollmentId(enrollment.getId()).studentCnie(cnie)
            .courseName(course.getTitle()).courseCredits(course.getCredits())
            .enrollmentDate(enrollment.getEnrollmentDate()).deletable(isDeletable(enrollment.getEnrollmentDate())).build();
    }
}
