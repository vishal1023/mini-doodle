package com.doodle.scheduler.slot;

import com.doodle.scheduler.generated.model.SlotResponseDto;

public sealed interface SlotUpdateOutcome {

    record Updated(SlotResponseDto slot) implements SlotUpdateOutcome {
    }

    record NotFound() implements SlotUpdateOutcome {
    }

    record Conflict() implements SlotUpdateOutcome {
    }
}
