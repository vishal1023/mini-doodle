package com.doodle.scheduler.slot;

import com.doodle.scheduler.generated.model.SlotPageDto;
import com.doodle.scheduler.generated.model.SlotResponseDto;
import com.doodle.scheduler.generated.model.SlotStatusDto;
import org.springframework.data.domain.Page;

import java.time.ZoneOffset;
import java.util.List;

public final class SlotMapper {

    private SlotMapper() {
    }

    public static SlotResponseDto toDto(Slot slot) {
        return new SlotResponseDto()
                .id(slot.getId())
                .userId(slot.getUserId())
                .startTime(slot.getStartTime().atOffset(ZoneOffset.UTC))
                .endTime(slot.getEndTime().atOffset(ZoneOffset.UTC))
                .status(SlotStatusDto.valueOf(slot.getStatus().name()));
    }

    public static SlotPageDto toPageDto(Page<Slot> page) {
        List<SlotResponseDto> content = page.getContent().stream()
                .map(SlotMapper::toDto)
                .toList();
        return new SlotPageDto()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements());
    }
}
