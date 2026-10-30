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
    private final ReferenceGuard references;
    private final Clock clock;

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or #request.studentCnie == authentication.token.claims['cnie']")
    public EnrollmentResponseDTO enrollStudent(EnrollmentRequest request) {
        StudentDTO student = studentClient.getStudentByCnie(request.getStudentCnie());
        var auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        if (auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                && !student.getId().toString().equals(auth.getToken().getClaimAsString("student_id"))) {
            throw new AccessDeniedException("Student identity does not match the account");
        }
        // Locks are held until the insertion commits, including the course count check.
        references.lockOpen("student", student.getId());
        references.lockOpen("course", request.getCourseId());
        // Recheck after taking the locks: deletion might have completed during the first GET.
        studentClient.getStudentById(student.getId());
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
        var auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !enrollment.getStudentId().toString().equals(auth.getToken().getClaimAsString("student_id"))) {
            throw new AccessDeniedException("You can only cancel your own enrollments");
        }
        if (!isDeletable(enrollment.getEnrollmentDate())) {
            throw new ResponseStatusException(CONFLICT, "Cancellation window has passed (24 hours)");
        }
        enrollmentRepository.delete(enrollment);
        enrollmentRepository.flush();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void closeStudent(Long id) {
        references.close("student", id);
        if (enrollmentRepository.existsByStudentId(id)) {
            throw new ResponseStatusException(CONFLICT, "Student has active enrollments");
        }
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void closeCourse(Long id) {
        references.close("course", id);
        if (enrollmentRepository.existsByCourseId(id)) {
            throw new ResponseStatusException(CONFLICT, "Course has active enrollments");
        }
    }

    private boolean isDeletable(Instant enrolledAt) {
        return clock.instant().isBefore(enrolledAt.plus(Duration.ofHours(24)));
    }

    private EnrollmentResponseDTO buildResponseDTO(Enrollment e, String cnie, CourseDTO course) {
        return EnrollmentResponseDTO.builder().enrollmentId(e.getId()).studentCnie(cnie)
            .courseName(course.getTitle()).courseCredits(course.getCredits())
            .enrollmentDate(e.getEnrollmentDate()).deletable(isDeletable(e.getEnrollmentDate())).build();
    }
}
