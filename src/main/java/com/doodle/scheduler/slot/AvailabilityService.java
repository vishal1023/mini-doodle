package com.doodle.scheduler.slot;

import com.doodle.scheduler.user.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AvailabilityService {

    private static final Duration MAX_WINDOW = Duration.ofDays(31);

    private final SlotRepository slotRepository;
    private final UserRepository userRepository;

    public AvailabilityService(SlotRepository slotRepository, UserRepository userRepository) {
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AvailabilityOutcome getAvailability(UUID userId, Instant from, Instant to) {
        if (!to.isAfter(from) || Duration.between(from, to).compareTo(MAX_WINDOW) > 0) {
            return new AvailabilityOutcome.InvalidRange();
        }
        if (!userRepository.existsById(userId)) {
            return new AvailabilityOutcome.NotFound();
        }

        Specification<Slot> spec = Specification.where(SlotSpecifications.hasUserId(userId))
                .and(SlotSpecifications.endsAfter(from))
                .and(SlotSpecifications.startsBefore(to));
        List<Slot> slots = slotRepository.findAll(spec, Sort.by("startTime"));

        List<TimeInterval> intervals = slots.stream()
                .map(slot -> new TimeInterval(slot.getStartTime(), slot.getEndTime(), slot.getStatus()))
                .toList();
        List<TimeInterval> merged = AvailabilityMerger.merge(intervals, from, to);

        return new AvailabilityOutcome.Available(AvailabilityMapper.toDto(userId, from, to, merged));
    }
}
