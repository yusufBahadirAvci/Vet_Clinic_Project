package com.vetclinic.auth;

import com.vetclinic.common.exception.NotFoundException;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("dev")
public class DevLoginService {

    private final UserRepository userRepository;

    public DevLoginService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> listUsers() {
        return userRepository.findAll(Sort.by("role", "fullName"));
    }

    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }
}
