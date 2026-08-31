package com.doodle.scheduler.slot;

import com.doodle.scheduler.generated.model.AvailabilityDto;

public sealed interface AvailabilityOutcome {

    record Available(AvailabilityDto availability) implements AvailabilityOutcome {
    }

    record NotFound() implements AvailabilityOutcome {
    }

    record InvalidRange() implements AvailabilityOutcome {
    }
}
