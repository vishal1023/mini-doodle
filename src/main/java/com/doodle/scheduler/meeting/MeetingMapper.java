package com.doodle.scheduler.meeting;

import com.doodle.scheduler.generated.model.MeetingPageDto;
import com.doodle.scheduler.generated.model.MeetingResponseDto;
import com.doodle.scheduler.generated.model.MeetingStatusDto;
import org.springframework.data.domain.Page;

import java.time.ZoneOffset;
import java.util.List;

public final class MeetingMapper {

    private MeetingMapper() {
    }

    public static MeetingResponseDto toDto(Meeting meeting) {
        return new MeetingResponseDto()
                .id(meeting.getId())
                .slotId(meeting.getSlotId())
                .organizerId(meeting.getOrganizerId())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .status(MeetingStatusDto.valueOf(meeting.getStatus().name()))
                .participantUserIds(List.copyOf(meeting.getParticipantIds()))
                .createdAt(meeting.getCreatedAt().atOffset(ZoneOffset.UTC));
    }

    public static MeetingPageDto toPageDto(Page<Meeting> page) {
        List<MeetingResponseDto> content = page.getContent().stream()
                .map(MeetingMapper::toDto)
                .toList();
        return new MeetingPageDto()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements());
    }
}
