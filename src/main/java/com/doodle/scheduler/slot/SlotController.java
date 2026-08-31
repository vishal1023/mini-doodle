package com.doodle.scheduler.slot;

import com.doodle.scheduler.generated.api.SlotsApi;
import com.doodle.scheduler.generated.model.AvailabilityDto;
import com.doodle.scheduler.generated.model.CreateSlotRequestDto;
import com.doodle.scheduler.generated.model.SlotPageDto;
import com.doodle.scheduler.generated.model.SlotResponseDto;
import com.doodle.scheduler.generated.model.SlotStatusDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
public class SlotController implements SlotsApi {

    private final SlotService slotService;
    private final AvailabilityService availabilityService;

    public SlotController(SlotService slotService, AvailabilityService availabilityService) {
        this.slotService = slotService;
        this.availabilityService = availabilityService;
    }

    @Override
    public ResponseEntity<SlotResponseDto> createSlot(UUID userId, CreateSlotRequestDto createSlotRequestDto) {
        return slotService.createSlot(userId, createSlotRequestDto)
                .map(slot -> ResponseEntity
                        .created(URI.create("/api/v1/users/" + userId + "/slots/" + slot.getId()))
                        .body(slot))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<SlotPageDto> listSlots(
            UUID userId, OffsetDateTime from, OffsetDateTime to, SlotStatusDto status, Integer page, Integer size) {
        Instant fromInstant = from != null ? from.toInstant() : null;
        Instant toInstant = to != null ? to.toInstant() : null;
        SlotStatus slotStatus = status != null ? SlotStatus.valueOf(status.name()) : null;
        Pageable pageable = PageRequest.of(page, size);

        return slotService.listSlots(userId, slotStatus, fromInstant, toInstant, pageable)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<SlotResponseDto> updateSlot(UUID userId, UUID slotId, CreateSlotRequestDto createSlotRequestDto) {
        SlotUpdateOutcome outcome = slotService.updateSlot(userId, slotId, createSlotRequestDto);
        return switch (outcome) {
            case SlotUpdateOutcome.Updated updated -> ResponseEntity.ok(updated.slot());
            case SlotUpdateOutcome.NotFound ignored -> ResponseEntity.notFound().build();
            case SlotUpdateOutcome.Conflict ignored -> ResponseEntity.status(HttpStatus.CONFLICT).build();
        };
    }

    @Override
    public ResponseEntity<Void> deleteSlot(UUID userId, UUID slotId) {
        SlotDeleteOutcome outcome = slotService.deleteSlot(userId, slotId);
        return switch (outcome) {
            case DELETED -> ResponseEntity.noContent().build();
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).build();
        };
    }

    @Override
    public ResponseEntity<AvailabilityDto> getAvailability(UUID userId, OffsetDateTime from, OffsetDateTime to) {
        AvailabilityOutcome outcome = availabilityService.getAvailability(userId, from.toInstant(), to.toInstant());
        return switch (outcome) {
            case AvailabilityOutcome.Available available -> ResponseEntity.ok(available.availability());
            case AvailabilityOutcome.NotFound ignored -> ResponseEntity.notFound().build();
            case AvailabilityOutcome.InvalidRange ignored -> ResponseEntity.badRequest().build();
        };
    }
}
