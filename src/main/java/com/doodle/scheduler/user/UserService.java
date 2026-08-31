package com.doodle.scheduler.user;

import com.doodle.scheduler.generated.model.CreateUserRequestDto;
import com.doodle.scheduler.generated.model.UserResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponseDto createUser(CreateUserRequestDto request) {
        User user = userRepository.save(new User(request.getName(), request.getEmail()));
        return UserMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public Optional<UserResponseDto> getUser(UUID id) {
        return userRepository.findById(id).map(UserMapper::toDto);
    }
}
