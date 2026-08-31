package com.doodle.scheduler.meeting;

import com.doodle.scheduler.generated.model.MeetingResponseDto;

public sealed interface MeetingUpdateOutcome {

    record Updated(MeetingResponseDto meeting) implements MeetingUpdateOutcome {
    }

    record NotFound() implements MeetingUpdateOutcome {
    }

    record Conflict() implements MeetingUpdateOutcome {
    }

    record InvalidParticipants() implements MeetingUpdateOutcome {
    }
}
