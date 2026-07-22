package com.evidencepilot.repository;

import com.evidencepilot.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import com.evidencepilot.model.enums.AccountStatus;
import com.evidencepilot.model.enums.UserRole;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailVerificationTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.email = :email")
    Optional<User> findByEmailForPasswordReset(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.id = :id")
    Optional<User> findByIdForPasswordReset(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.passwordResetTokenHash = :tokenHash")
    Optional<User> findByPasswordResetTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByAccountStatus(AccountStatus status);

    List<User> findByAccountStatusAndRole(AccountStatus status, UserRole role);

    @Query("select user.role, count(user) from User user group by user.role")
    List<Object[]> countByRole();

    @Query("select user.accountStatus, count(user) from User user group by user.accountStatus")
    List<Object[]> countByAccountStatus();
}
