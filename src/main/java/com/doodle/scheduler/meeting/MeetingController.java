package com.doodle.scheduler.meeting;

import com.doodle.scheduler.generated.api.MeetingsApi;
import com.doodle.scheduler.generated.model.CreateMeetingRequestDto;
import com.doodle.scheduler.generated.model.MeetingResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
public class MeetingController implements MeetingsApi {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @Override
    public ResponseEntity<MeetingResponseDto> bookSlot(
            UUID userId, UUID slotId, CreateMeetingRequestDto createMeetingRequestDto) {
        BookingOutcome outcome = meetingService.bookSlot(userId, slotId, createMeetingRequestDto);
        return switch (outcome) {
            case BookingOutcome.Booked booked -> ResponseEntity
                    .created(URI.create("/api/v1/meetings/" + booked.meeting().getId()))
                    .body(booked.meeting());
            case BookingOutcome.NotFound ignored -> ResponseEntity.notFound().build();
            case BookingOutcome.Conflict ignored -> ResponseEntity.status(HttpStatus.CONFLICT).build();
            case BookingOutcome.InvalidParticipants ignored -> ResponseEntity.badRequest().build();
        };
    }
}
