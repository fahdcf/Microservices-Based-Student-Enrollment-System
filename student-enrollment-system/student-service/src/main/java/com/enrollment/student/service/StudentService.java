package com.enrollment.student.service;

import com.enrollment.student.client.EnrollmentClient;
import com.enrollment.student.dto.*;
import com.enrollment.student.entity.Student;
import com.enrollment.student.mapper.StudentMapper;
import com.enrollment.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final EnrollmentClient enrollmentClient;

    public StudentResponse createStudent(StudentRequest request) {
        if (studentRepository.existsByCnie(request.getCnie())) {
            throw new IllegalArgumentException("A student with CNIE " + request.getCnie() + " already exists.");
        }
        if (studentRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("A student with this email already exists.");
        }
        Student saved = studentRepository.save(studentMapper.toEntity(request));
        return studentMapper.toResponse(saved);
    }

    public List<StudentResponse> getAllStudents() {
        return studentRepository.findAll()
                .stream()
                .map(studentMapper::toResponse)
                .toList();
    }

    public StudentResponse getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + id));
        return studentMapper.toResponse(student);
    }

    public StudentResponse getStudentByCnie(String cnie) {
        Student student = studentRepository.findByCnie(cnie)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with CNIE: " + cnie));
        return studentMapper.toResponse(student);
    }

    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new IllegalArgumentException("Student not found with id: " + id);
        }
        enrollmentClient.deleteEnrollmentsByStudentId(id);
        studentRepository.deleteById(id);
    }
}
