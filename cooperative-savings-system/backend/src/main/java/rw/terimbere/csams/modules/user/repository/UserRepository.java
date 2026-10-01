package rw.terimbere.csams.modules.user.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.terimbere.csams.modules.user.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByUsernameIgnoreCaseAndDeletedFalse(String username);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByEmailIgnoreCaseAndDeletedFalse(String email);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findWithRolesById(UUID id);

    boolean existsByUsernameIgnoreCaseAndDeletedFalse(String username);

    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    boolean existsByUsernameIgnoreCaseAndDeletedFalseAndIdNot(String username, UUID id);

    boolean existsByEmailIgnoreCaseAndDeletedFalseAndIdNot(String email, UUID id);

    Optional<User> findByNationalIdAndDeletedFalse(String nationalId);

    boolean existsByNationalIdAndDeletedFalse(String nationalId);

    boolean existsByNationalIdAndDeletedFalseAndIdNot(String nationalId, UUID id);

    Optional<User> findByIdAndDeletedFalse(UUID id);

    /** Ids (from {@code userIds}) of non-deleted users holding the global SUPER_ADMIN role. */
    @Query(
            """
            SELECT DISTINCT u.id FROM User u
            JOIN u.roles r
            WHERE u.deleted = false
              AND r.code = 'SUPER_ADMIN'
              AND u.id IN :userIds
            """)
    List<UUID> findSuperAdminIdsAmong(@Param("userIds") Collection<UUID> userIds);

    long countByDeletedFalse();
}
