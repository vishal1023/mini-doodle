package com.doodle.scheduler.slot;

import com.doodle.scheduler.generated.model.CreateSlotRequestDto;
import com.doodle.scheduler.generated.model.SlotPageDto;
import com.doodle.scheduler.generated.model.SlotResponseDto;
import com.doodle.scheduler.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class SlotService {

    private final SlotRepository slotRepository;
    private final UserRepository userRepository;

    public SlotService(SlotRepository slotRepository, UserRepository userRepository) {
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Optional<SlotResponseDto> createSlot(UUID userId, CreateSlotRequestDto request) {
        if (!userRepository.existsById(userId)) {
            return Optional.empty();
        }
        Instant startTime = request.getStartTime().toInstant();
        Instant endTime = startTime.plus(Duration.ofMinutes(request.getDurationMinutes()));
        Slot slot = slotRepository.save(new Slot(userId, startTime, endTime));
        return Optional.of(SlotMapper.toDto(slot));
    }

    @Transactional(readOnly = true)
    public Optional<SlotPageDto> listSlots(UUID userId, SlotStatus status, Instant from, Instant to, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            return Optional.empty();
        }
        Specification<Slot> spec = Specification.where(SlotSpecifications.hasUserId(userId))
                .and(SlotSpecifications.hasStatus(status))
                .and(SlotSpecifications.endsAfter(from))
                .and(SlotSpecifications.startsBefore(to));
        Page<Slot> page = slotRepository.findAll(spec, pageable);
        return Optional.of(SlotMapper.toPageDto(page));
    }

    @Transactional
    public SlotUpdateOutcome updateSlot(UUID userId, UUID slotId, CreateSlotRequestDto request) {
        Optional<Slot> maybeSlot = findOwnedSlot(userId, slotId);
        if (maybeSlot.isEmpty()) {
            return new SlotUpdateOutcome.NotFound();
        }
        Slot slot = maybeSlot.get();
        if (!slot.isFree()) {
            return new SlotUpdateOutcome.Conflict();
        }
        Instant startTime = request.getStartTime().toInstant();
        Instant endTime = startTime.plus(Duration.ofMinutes(request.getDurationMinutes()));
        slot.updateWindow(startTime, endTime);
        return new SlotUpdateOutcome.Updated(SlotMapper.toDto(slot));
    }

    @Transactional
    public SlotDeleteOutcome deleteSlot(UUID userId, UUID slotId) {
        Optional<Slot> maybeSlot = findOwnedSlot(userId, slotId);
        if (maybeSlot.isEmpty()) {
            return SlotDeleteOutcome.NOT_FOUND;
        }
        Slot slot = maybeSlot.get();
        if (!slot.isFree()) {
            return SlotDeleteOutcome.CONFLICT;
        }
        slotRepository.delete(slot);
        return SlotDeleteOutcome.DELETED;
    }

    private Optional<Slot> findOwnedSlot(UUID userId, UUID slotId) {
        return slotRepository.findById(slotId)
                .filter(slot -> slot.getUserId().equals(userId));
    }
}
