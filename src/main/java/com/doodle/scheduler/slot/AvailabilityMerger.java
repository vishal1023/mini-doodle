package com.doodle.scheduler.slot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AvailabilityMerger {

    private AvailabilityMerger() {
    }

    public static List<TimeInterval> merge(List<TimeInterval> slots, Instant from, Instant to) {
        List<TimeInterval> clipped = slots.stream()
                .map(slot -> clip(slot, from, to))
                .sorted(Comparator.comparing(TimeInterval::start))
                .toList();

        List<TimeInterval> merged = new ArrayList<>();
        for (TimeInterval interval : clipped) {
            int lastIndex = merged.size() - 1;
            if (lastIndex >= 0) {
                TimeInterval last = merged.get(lastIndex);
                if (last.end().equals(interval.start()) && last.status() == interval.status()) {
                    merged.set(lastIndex, new TimeInterval(last.start(), interval.end(), last.status()));
                    continue;
                }
            }
            merged.add(interval);
        }
        return merged;
    }

    private static TimeInterval clip(TimeInterval slot, Instant from, Instant to) {
        Instant start = slot.start().isBefore(from) ? from : slot.start();
        Instant end = slot.end().isAfter(to) ? to : slot.end();
        return new TimeInterval(start, end, slot.status());
    }
}
