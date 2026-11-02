package com.enrollment.enrollment.entity;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@EqualsAndHashCode
public class EnrollmentReferenceId implements Serializable {
    private String resourceType;
    private Long resourceId;
}
