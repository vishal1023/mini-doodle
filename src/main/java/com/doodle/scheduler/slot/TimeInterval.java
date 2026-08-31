package com.doodle.scheduler.slot;

import java.time.Instant;

public record TimeInterval(Instant start, Instant end, SlotStatus status) {
}
