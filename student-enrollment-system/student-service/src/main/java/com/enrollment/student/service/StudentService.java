package com.enrollment.student.service;

import com.enrollment.student.client.EnrollmentClient;
import com.enrollment.student.dto.*;
import com.enrollment.student.entity.Student;
import com.enrollment.student.mapper.StudentMapper;
import com.enrollment.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final EnrollmentClient enrollmentClient;

    public StudentResponse createStudent(StudentRequest request) {
        if (studentRepository.existsByCnie(request.getCnie())) {
            throw new ResponseStatusException(CONFLICT, "A student with CNIE " + request.getCnie() + " already exists.");
        }
        if (studentRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(CONFLICT, "A student with this email already exists.");
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
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Student not found with id: " + id));
        return studentMapper.toResponse(student);
    }

    @org.springframework.security.access.prepost.PostAuthorize("hasRole('ADMIN') or returnObject.id.toString() == authentication.token.claims['student_id']")
    public StudentResponse getStudentByCnie(String cnie) {
        Student student = studentRepository.findByCnie(cnie)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Student not found with CNIE: " + cnie));
        return studentMapper.toResponse(student);
    }

    @Transactional
    public StudentResponse updateStudent(Long id, StudentRequest request) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Student not found"));
        if (!student.getCnie().equals(request.getCnie())) {
            throw new ResponseStatusException(CONFLICT, "CNIE cannot be changed");
        }
        if (!student.getEmail().equals(request.getEmail()) && studentRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(CONFLICT, "A student with this email already exists");
        }
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setEmail(request.getEmail());
        return studentMapper.toResponse(studentRepository.save(student));
    }

    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Student not found with id: " + id);
        }
        enrollmentClient.closeReference(id);
        studentRepository.deleteById(id);
    }
}
