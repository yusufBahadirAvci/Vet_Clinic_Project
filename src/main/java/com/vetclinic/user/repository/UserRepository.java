package com.vetclinic.user.repository;

import com.vetclinic.user.domain.Role;
import com.vetclinic.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** Contacts passed here must already be normalized by {@code common.ContactNormalizer}. */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    List<User> findAllByRoleOrderByFullName(Role role);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    /** Duplicate check when editing: is the e-mail used by any other user? */
    boolean existsByEmailAndIdNot(String email, Long id);

    /** Duplicate check when editing: is the phone used by any other user? */
    boolean existsByPhoneAndIdNot(String phone, Long id);

    long countByRoleAndActiveTrue(Role role);
}
