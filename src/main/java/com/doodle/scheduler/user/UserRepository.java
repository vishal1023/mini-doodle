package com.doodle.scheduler.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    long countByIdIn(Collection<UUID> ids);
}
