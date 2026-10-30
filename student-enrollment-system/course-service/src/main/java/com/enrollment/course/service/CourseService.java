package com.enrollment.course.service;

import com.enrollment.course.dto.*;
import com.enrollment.course.entity.Course;
import com.enrollment.course.mapper.CourseMapper;
import com.enrollment.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final com.enrollment.course.client.EnrollmentClient enrollmentClient;

    public CourseResponse createCourse(CourseRequest request) {
        if (courseRepository.existsByTitle(request.getTitle())) {
            throw new ResponseStatusException(CONFLICT, "A course with title '" + request.getTitle() + "' already exists.");
        }
        Course saved = courseRepository.save(courseMapper.toEntity(request));
        return courseMapper.toResponse(saved);
    }

    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll()
                .stream()
                .map(courseMapper::toResponse)
                .toList();
    }

    public CourseResponse getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Course not found with id: " + id));
        return courseMapper.toResponse(course);
    }

    public CourseResponse updateCourse(Long id, CourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Course not found with id: " + id));
        if (!course.getTitle().equals(request.getTitle()) && courseRepository.existsByTitle(request.getTitle())) {
            throw new ResponseStatusException(CONFLICT, "A course with this title already exists");
        }
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCredits(request.getCredits());
        return courseMapper.toResponse(courseRepository.save(course));
    }

    public void deleteCourse(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Course not found with id: " + id);
        }
        enrollmentClient.closeReference(id);
        courseRepository.deleteById(id);
    }
}
