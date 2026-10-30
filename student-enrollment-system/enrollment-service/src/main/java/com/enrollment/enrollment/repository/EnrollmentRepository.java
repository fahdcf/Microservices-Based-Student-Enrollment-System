package com.enrollment.enrollment.repository;

import com.enrollment.enrollment.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Enrollment e where e.id = :id")
    java.util.Optional<Enrollment> findLockedById(Long id);
    boolean existsByStudentId(Long studentId);
    boolean existsByCourseId(Long courseId);
    List<Enrollment> findByStudentId(Long studentId);
    long countByCourseId(Long courseId);
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
}
