package com.doodle.scheduler.slot;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

public final class SlotSpecifications {

    private SlotSpecifications() {
    }

    public static Specification<Slot> hasUserId(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<Slot> hasStatus(SlotStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Slot> endsAfter(Instant from) {
        return from == null ? null : (root, query, cb) -> cb.greaterThan(root.get("endTime"), from);
    }

    public static Specification<Slot> startsBefore(Instant to) {
        return to == null ? null : (root, query, cb) -> cb.lessThan(root.get("startTime"), to);
    }
}
