package com.vetclinic.user.repository;

import com.vetclinic.AbstractIntegrationTest;
import com.vetclinic.user.domain.Role;
import com.vetclinic.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class UserRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void seedDataIsNotLoadedInTests() {
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void savesAndFindsByEmailAndPhone() {
        User saved = userRepository.saveAndFlush(user("Ayşe Demir", "ayse@example.com", "+905321234567", Role.OWNER));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.isActive()).isTrue();

        assertThat(userRepository.findByEmail("ayse@example.com")).get()
                .extracting(User::getId).isEqualTo(saved.getId());
        assertThat(userRepository.findByPhone("+905321234567")).get()
                .extracting(User::getId).isEqualTo(saved.getId());
        assertThat(userRepository.existsByEmail("ayse@example.com")).isTrue();
        assertThat(userRepository.existsByPhone("+905321234567")).isTrue();
        assertThat(userRepository.findByEmail("nobody@example.com")).isEmpty();
        assertThat(userRepository.existsByPhone("+905009999999")).isFalse();
    }

    @Test
    void findsAllByRoleOrderedByFullName() {
        userRepository.save(user("Zeynep Arslan", null, "+905334445566", Role.OWNER));
        userRepository.save(user("Burak Aydın", "burak@example.com", null, Role.OWNER));
        userRepository.save(user("Dr. Elif Yılmaz", "elif@example.com", null, Role.VET));
        userRepository.flush();

        assertThat(userRepository.findAllByRoleOrderByFullName(Role.OWNER))
                .extracting(User::getFullName)
                .containsExactly("Burak Aydın", "Zeynep Arslan");
    }

    @Test
    void duplicateEmailIsRejected() {
        userRepository.saveAndFlush(user("First", "same@example.com", null, Role.OWNER));

        assertThatThrownBy(() -> userRepository.saveAndFlush(user("Second", "same@example.com", null, Role.OWNER)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicatePhoneIsRejected() {
        userRepository.saveAndFlush(user("First", null, "+905321234567", Role.OWNER));

        assertThatThrownBy(() -> userRepository.saveAndFlush(user("Second", null, "+905321234567", Role.OWNER)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void userWithoutEmailAndPhoneIsRejected() {
        assertThatThrownBy(() -> userRepository.saveAndFlush(user("No Contact", null, null, Role.OWNER)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static User user(String fullName, String email, String phone, Role role) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(role);
        return user;
    }
}
