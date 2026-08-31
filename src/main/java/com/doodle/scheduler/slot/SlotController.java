package com.doodle.scheduler.slot;

import com.doodle.scheduler.generated.api.SlotsApi;
import com.doodle.scheduler.generated.model.CreateSlotRequestDto;
import com.doodle.scheduler.generated.model.SlotPageDto;
import com.doodle.scheduler.generated.model.SlotResponseDto;
import com.doodle.scheduler.generated.model.SlotStatusDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
public class SlotController implements SlotsApi {

    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
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
}
