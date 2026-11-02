package com.enrollment.enrollment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "enrollment_references")
@IdClass(EnrollmentReferenceId.class)
@Getter
@Setter
@NoArgsConstructor
public class EnrollmentReference {
    @Id
    @Column(name = "resource_type", length = 10)
    private String resourceType;

    @Id
    @Column(name = "resource_id")
    private Long resourceId;

    @Column(nullable = false)
    private boolean closed;
}
