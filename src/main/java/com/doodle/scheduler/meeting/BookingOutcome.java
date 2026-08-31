package com.doodle.scheduler.meeting;

import com.doodle.scheduler.generated.model.MeetingResponseDto;

public sealed interface BookingOutcome {

    record Booked(MeetingResponseDto meeting) implements BookingOutcome {
    }

    record NotFound() implements BookingOutcome {
    }

    record Conflict() implements BookingOutcome {
    }

    record InvalidParticipants() implements BookingOutcome {
    }
}
