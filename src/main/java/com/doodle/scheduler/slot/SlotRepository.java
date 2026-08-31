package com.doodle.scheduler.slot;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface SlotRepository extends JpaRepository<Slot, UUID>, JpaSpecificationExecutor<Slot> {

    // Pessimistic write lock scoped to the single row being booked - makes
    // "read slot -> check FREE -> flip to BUSY -> insert Meeting" atomic
    // without relying on optimistic-lock retry for the highest-contention
    // write path in the system. Held for milliseconds, not a scale risk.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Slot s WHERE s.id = :id")
    Optional<Slot> findByIdForUpdate(UUID id);
}
