package com.doodle.scheduler.meeting;

import com.doodle.scheduler.generated.api.MeetingsApi;
import com.doodle.scheduler.generated.model.CreateMeetingRequestDto;
import com.doodle.scheduler.generated.model.MeetingPageDto;
import com.doodle.scheduler.generated.model.MeetingResponseDto;
import com.doodle.scheduler.generated.model.MeetingRoleDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    @Override
    public ResponseEntity<Void> cancelMeeting(UUID meetingId) {
        CancellationOutcome outcome = meetingService.cancelMeeting(meetingId);
        return switch (outcome) {
            case CANCELLED -> ResponseEntity.noContent().build();
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).build();
        };
    }

    @Override
    public ResponseEntity<MeetingResponseDto> getMeeting(UUID meetingId) {
        return meetingService.getMeeting(meetingId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<MeetingResponseDto> updateMeeting(UUID meetingId, CreateMeetingRequestDto createMeetingRequestDto) {
        MeetingUpdateOutcome outcome = meetingService.updateMeeting(meetingId, createMeetingRequestDto);
        return switch (outcome) {
            case MeetingUpdateOutcome.Updated updated -> ResponseEntity.ok(updated.meeting());
            case MeetingUpdateOutcome.NotFound ignored -> ResponseEntity.notFound().build();
            case MeetingUpdateOutcome.Conflict ignored -> ResponseEntity.status(HttpStatus.CONFLICT).build();
            case MeetingUpdateOutcome.InvalidParticipants ignored -> ResponseEntity.badRequest().build();
        };
    }

    @Override
    public ResponseEntity<MeetingPageDto> listMeetings(UUID userId, MeetingRoleDto role, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return meetingService.listMeetings(userId, role, pageable)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
