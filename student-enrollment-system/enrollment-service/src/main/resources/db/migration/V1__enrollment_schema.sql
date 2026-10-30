CREATE TABLE IF NOT EXISTS enrollments (
 id BIGSERIAL PRIMARY KEY, student_id BIGINT NOT NULL, course_id BIGINT NOT NULL,
 enrollment_date TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
-- Existing LocalDateTime values are interpreted as UTC. See upgrade instructions.
ALTER TABLE enrollments ALTER COLUMN enrollment_date TYPE TIMESTAMP WITH TIME ZONE
 USING enrollment_date AT TIME ZONE 'UTC';
CREATE UNIQUE INDEX IF NOT EXISTS uq_enrollment_student_course ON enrollments(student_id, course_id);
CREATE INDEX IF NOT EXISTS ix_enrollment_course ON enrollments(course_id);
CREATE TABLE enrollment_references (
 resource_type VARCHAR(10) NOT NULL CHECK(resource_type IN ('student','course')),
 resource_id BIGINT NOT NULL,
 closed BOOLEAN NOT NULL DEFAULT false,
 PRIMARY KEY(resource_type, resource_id)
);
