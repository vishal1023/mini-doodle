package com.doodle.scheduler.user;

import com.doodle.scheduler.generated.api.UsersApi;
import com.doodle.scheduler.generated.model.CreateUserRequestDto;
import com.doodle.scheduler.generated.model.UserResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
public class UserController implements UsersApi {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public ResponseEntity<UserResponseDto> createUser(CreateUserRequestDto createUserRequestDto) {
        UserResponseDto user = userService.createUser(createUserRequestDto);
        return ResponseEntity.created(URI.create("/api/v1/users/" + user.getId())).body(user);
    }

    @Override
    public ResponseEntity<UserResponseDto> getUser(UUID userId) {
        return userService.getUser(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
