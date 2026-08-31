package com.doodle.scheduler.slot;

import com.doodle.scheduler.generated.model.AvailabilityDto;
import com.doodle.scheduler.generated.model.SlotIntervalDto;
import com.doodle.scheduler.generated.model.SlotStatusDto;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

public final class AvailabilityMapper {

    private AvailabilityMapper() {
    }

    public static AvailabilityDto toDto(UUID userId, Instant from, Instant to, List<TimeInterval> intervals) {
        List<SlotIntervalDto> intervalDtos = intervals.stream()
                .map(AvailabilityMapper::toIntervalDto)
                .toList();
        return new AvailabilityDto()
                .userId(userId)
                .from(from.atOffset(ZoneOffset.UTC))
                .to(to.atOffset(ZoneOffset.UTC))
                .intervals(intervalDtos);
    }

    private static SlotIntervalDto toIntervalDto(TimeInterval interval) {
        return new SlotIntervalDto()
                .start(interval.start().atOffset(ZoneOffset.UTC))
                .end(interval.end().atOffset(ZoneOffset.UTC))
                .status(SlotStatusDto.valueOf(interval.status().name()));
    }
}
