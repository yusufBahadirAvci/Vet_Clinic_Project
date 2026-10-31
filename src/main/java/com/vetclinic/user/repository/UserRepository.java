package com.vetclinic.user.repository;

import com.vetclinic.user.domain.Role;
import com.vetclinic.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Contacts passed here must already be normalized by {@code common.ContactNormalizer}. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    List<User> findAllByRoleOrderByFullName(Role role);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
