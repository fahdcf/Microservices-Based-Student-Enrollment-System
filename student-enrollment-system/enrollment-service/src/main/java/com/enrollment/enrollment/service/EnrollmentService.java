package com.enrollment.enrollment.service;

import com.enrollment.enrollment.client.*;
import com.enrollment.enrollment.dto.*;
import com.enrollment.enrollment.entity.Enrollment;
import com.enrollment.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private static final int MAX_STUDENTS_PER_COURSE = 3;
    private static final int CANCELLATION_WINDOW_HOURS = 24;

    private final EnrollmentRepository enrollmentRepository;
    private final StudentClient studentClient;
    private final CourseClient courseClient;

    public EnrollmentResponseDTO enrollStudent(EnrollmentRequest request) {
        StudentDTO student = studentClient.getStudentByCnie(request.getStudentCnie());
        CourseDTO course = courseClient.getCourseById(request.getCourseId());

        long currentCount = enrollmentRepository.countByCourseId(request.getCourseId());
        if (currentCount >= MAX_STUDENTS_PER_COURSE) {
            throw new IllegalArgumentException(
                    "Course '" + course.getTitle() + "' is full (max " + MAX_STUDENTS_PER_COURSE + " students).");
        }

        if (enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), request.getCourseId())) {
            throw new IllegalArgumentException(
                    "Student is already enrolled in '" + course.getTitle() + "'.");
        }

        Enrollment enrollment = Enrollment.builder()
                .studentId(student.getId())
                .courseId(course.getId())
                .enrollmentDate(LocalDateTime.now())
                .build();
        Enrollment saved = enrollmentRepository.save(enrollment);

        return buildResponseDTO(saved, student.getCnie(), course);
    }

    public List<EnrollmentResponseDTO> getEnrollmentsByStudentCnie(String cnie) {
        StudentDTO student = studentClient.getStudentByCnie(cnie);
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());

        return enrollments.stream().map(enrollment -> {
            CourseDTO course = courseClient.getCourseById(enrollment.getCourseId());
            return buildResponseDTO(enrollment, student.getCnie(), course);
        }).toList();
    }

    public void cancelEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found with id: " + enrollmentId));

        if (!isDeletable(enrollment.getEnrollmentDate())) {
            throw new IllegalArgumentException(
                    "Cancellation window has passed. Enrollments can only be cancelled within "
                    + CANCELLATION_WINDOW_HOURS + " hours.");
        }

        enrollmentRepository.deleteById(enrollmentId);
    }

    private boolean isDeletable(LocalDateTime enrollmentDate) {
        return LocalDateTime.now().isBefore(enrollmentDate.plusHours(CANCELLATION_WINDOW_HOURS));
    }

    private EnrollmentResponseDTO buildResponseDTO(Enrollment enrollment, String cnie, CourseDTO course) {
        return EnrollmentResponseDTO.builder()
                .enrollmentId(enrollment.getId())
                .studentCnie(cnie)
                .courseName(course.getTitle())
                .courseCredits(course.getCredits())
                .enrollmentDate(enrollment.getEnrollmentDate())
                .deletable(isDeletable(enrollment.getEnrollmentDate()))
                .build();
    }
}
