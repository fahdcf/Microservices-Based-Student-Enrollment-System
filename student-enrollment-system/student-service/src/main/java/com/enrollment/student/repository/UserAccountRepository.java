package com.enrollment.student.repository;
import com.enrollment.student.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserAccountRepository extends JpaRepository<UserAccount,Long> {
    Optional<UserAccount> findByUsername(String username);
    boolean existsByRole(String role);
}
