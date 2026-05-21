package com.enrollment.student.repository;

import com.enrollment.student.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByCnie(String cnie);
    boolean existsByCnie(String cnie);
    boolean existsByEmail(String email);
}
