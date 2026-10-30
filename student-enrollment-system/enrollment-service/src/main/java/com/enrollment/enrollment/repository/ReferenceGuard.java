package com.enrollment.enrollment.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * Stable rows owned by Enrollment Service. PostgreSQL row locks coordinate every
 * instance of the service. Always lock student before course when both are needed.
 */
@Repository
@RequiredArgsConstructor
public class ReferenceGuard {
    private final JdbcTemplate jdbc;

    @Transactional(propagation = Propagation.MANDATORY)
    public void lockOpen(String type, Long id) {
        jdbc.execute("SET LOCAL lock_timeout = '5s'");
        jdbc.update("INSERT INTO enrollment_references(resource_type, resource_id, closed) VALUES (?, ?, false) ON CONFLICT DO NOTHING", type, id);
        Boolean closed = jdbc.queryForObject(
            "SELECT closed FROM enrollment_references WHERE resource_type = ? AND resource_id = ? FOR UPDATE",
            Boolean.class, type, id);
        if (Boolean.TRUE.equals(closed)) {
            throw new ResponseStatusException(CONFLICT, "This " + type + " is being deleted or has been deleted");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void close(String type, Long id) {
        // Lock even a previously closed row, making deletion retries idempotent.
        jdbc.execute("SET LOCAL lock_timeout = '5s'");
        jdbc.update("INSERT INTO enrollment_references(resource_type, resource_id, closed) VALUES (?, ?, false) ON CONFLICT DO NOTHING", type, id);
        jdbc.queryForObject("SELECT closed FROM enrollment_references WHERE resource_type = ? AND resource_id = ? FOR UPDATE", Boolean.class, type, id);
        jdbc.update("UPDATE enrollment_references SET closed = true WHERE resource_type = ? AND resource_id = ?", type, id);
    }
}
