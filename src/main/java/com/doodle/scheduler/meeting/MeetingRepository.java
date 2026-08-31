package com.doodle.scheduler.meeting;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    Page<Meeting> findByOrganizerId(UUID organizerId, Pageable pageable);

    @Query("SELECT DISTINCT m FROM Meeting m JOIN m.participantIds p WHERE p = :userId")
    Page<Meeting> findByParticipantId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT DISTINCT m FROM Meeting m LEFT JOIN m.participantIds p WHERE m.organizerId = :userId OR p = :userId")
    Page<Meeting> findByOrganizerIdOrParticipantId(@Param("userId") UUID userId, Pageable pageable);
}
