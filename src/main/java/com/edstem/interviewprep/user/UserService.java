package com.edstem.interviewprep.user;

import com.edstem.interviewprep.common.error.NotFoundException;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("User '" + email + "' not found"));
    }

    public List<UserResponse> listAll() {
        return userRepository.findAll(Sort.by("id")).stream().map(UserResponse::from).toList();
    }
}
