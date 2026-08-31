package com.doodle.scheduler.user;

import com.doodle.scheduler.generated.model.UserResponseDto;

import java.time.ZoneOffset;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponseDto toDto(User user) {
        return new UserResponseDto()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
}
