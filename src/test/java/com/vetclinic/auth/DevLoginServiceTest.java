package com.vetclinic.auth;

import com.vetclinic.common.exception.NotFoundException;
import com.vetclinic.user.domain.Role;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevLoginServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DevLoginService service;

    @Test
    void listsAllUsersSortedByRoleAndName() {
        User vet = AppUserPrincipalTest.user(1L, "Dr. Elif Yilmaz", Role.VET, true);
        when(userRepository.findAll(Sort.by("role", "fullName"))).thenReturn(List.of(vet));

        assertThat(service.listUsers()).containsExactly(vet);
    }

    @Test
    void getUserThrowsNotFoundForUnknownId() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUser(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getUserReturnsUser() {
        User owner = AppUserPrincipalTest.user(2L, "Ayse Demir", Role.OWNER, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));

        assertThat(service.getUser(2L)).isSameAs(owner);
    }
}
