package com.enrollment.enrollment.repository;

import com.enrollment.enrollment.entity.EnrollmentReference;
import com.enrollment.enrollment.entity.EnrollmentReferenceId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;

public interface EnrollmentReferenceRepository extends JpaRepository<EnrollmentReference, EnrollmentReferenceId> {
    // A row must exist before it can be locked. ON CONFLICT handles simultaneous first requests.
    @Modifying
    @Query(value = "INSERT INTO enrollment_references(resource_type, resource_id, closed) VALUES ('student', :id, false) ON CONFLICT DO NOTHING", nativeQuery = true)
    void createStudentIfMissing(Long id);

    @Modifying
    @Query(value = "INSERT INTO enrollment_references(resource_type, resource_id, closed) VALUES ('course', :id, false) ON CONFLICT DO NOTHING", nativeQuery = true)
    void createCourseIfMissing(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from EnrollmentReference r where r.resourceType = 'student' and r.resourceId = :id")
    EnrollmentReference findStudentForUpdate(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from EnrollmentReference r where r.resourceType = 'course' and r.resourceId = :id")
    EnrollmentReference findCourseForUpdate(Long id);

    default EnrollmentReference lockStudent(Long id) {
        createStudentIfMissing(id);
        return findStudentForUpdate(id);
    }

    default EnrollmentReference lockCourse(Long id) {
        createCourseIfMissing(id);
        return findCourseForUpdate(id);
    }
}
