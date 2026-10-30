package com.enrollment.student.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name="user_accounts")
@Getter @Setter @NoArgsConstructor
public class UserAccount {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, unique=true)
    private String username;
    @Column(nullable=false)
    private String passwordHash;
    @Column(nullable=false)
    private String role;
    @Column(unique=true)
    private Long studentId;
}
