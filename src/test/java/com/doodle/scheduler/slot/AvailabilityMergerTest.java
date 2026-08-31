package com.doodle.scheduler.slot;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityMergerTest {

    private static final Instant WINDOW_START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant WINDOW_END = Instant.parse("2026-01-02T00:00:00Z");

    private static Instant t(int hour) {
        return WINDOW_START.plusSeconds(hour * 3600L);
    }

    @Test
    void emptyInput_returnsEmptyList() {
        List<TimeInterval> result = AvailabilityMerger.merge(List.of(), WINDOW_START, WINDOW_END);

        assertThat(result).isEmpty();
    }

    @Test
    void singleSlotFullyWithinWindow_isUnchanged() {
        TimeInterval slot = new TimeInterval(t(9), t(10), SlotStatus.FREE);

        List<TimeInterval> result = AvailabilityMerger.merge(List.of(slot), WINDOW_START, WINDOW_END);

        assertThat(result).containsExactly(slot);
    }

    @Test
    void slotStartingBeforeWindow_isClippedToWindowStart() {
        TimeInterval slot = new TimeInterval(WINDOW_START.minusSeconds(3600), t(2), SlotStatus.FREE);

        List<TimeInterval> result = AvailabilityMerger.merge(List.of(slot), WINDOW_START, WINDOW_END);

        assertThat(result).containsExactly(new TimeInterval(WINDOW_START, t(2), SlotStatus.FREE));
    }

    @Test
    void slotEndingAfterWindow_isClippedToWindowEnd() {
        TimeInterval slot = new TimeInterval(t(22), WINDOW_END.plusSeconds(3600), SlotStatus.BUSY);

        List<TimeInterval> result = AvailabilityMerger.merge(List.of(slot), WINDOW_START, WINDOW_END);

        assertThat(result).containsExactly(new TimeInterval(t(22), WINDOW_END, SlotStatus.BUSY));
    }

    @Test
    void twoTouchingSlotsWithSameStatus_areMergedIntoOneInterval() {
        TimeInterval first = new TimeInterval(t(9), t(10), SlotStatus.FREE);
        TimeInterval second = new TimeInterval(t(10), t(11), SlotStatus.FREE);

        List<TimeInterval> result = AvailabilityMerger.merge(List.of(first, second), WINDOW_START, WINDOW_END);

        assertThat(result).containsExactly(new TimeInterval(t(9), t(11), SlotStatus.FREE));
    }

    @Test
    void threeConsecutiveTouchingSlotsWithSameStatus_mergeIntoOneInterval() {
        TimeInterval first = new TimeInterval(t(9), t(10), SlotStatus.FREE);
        TimeInterval second = new TimeInterval(t(10), t(11), SlotStatus.FREE);
        TimeInterval third = new TimeInterval(t(11), t(12), SlotStatus.FREE);

        List<TimeInterval> result = AvailabilityMerger.merge(
                List.of(first, second, third), WINDOW_START, WINDOW_END);

        assertThat(result).containsExactly(new TimeInterval(t(9), t(12), SlotStatus.FREE));
    }

    @Test
    void twoTouchingSlotsWithDifferentStatus_areNotMerged() {
        TimeInterval free = new TimeInterval(t(9), t(10), SlotStatus.FREE);
        TimeInterval busy = new TimeInterval(t(10), t(11), SlotStatus.BUSY);

        List<TimeInterval> result = AvailabilityMerger.merge(List.of(free, busy), WINDOW_START, WINDOW_END);

        assertThat(result).containsExactly(free, busy);
    }

    @Test
    void twoNonTouchingSlotsWithSameStatus_areNotMerged() {
        TimeInterval first = new TimeInterval(t(9), t(10), SlotStatus.FREE);
        TimeInterval second = new TimeInterval(t(12), t(13), SlotStatus.FREE);

        List<TimeInterval> result = AvailabilityMerger.merge(List.of(first, second), WINDOW_START, WINDOW_END);

        assertThat(result).containsExactly(first, second);
    }

    @Test
    void unsortedInput_isSortedBeforeMerging() {
        TimeInterval second = new TimeInterval(t(10), t(11), SlotStatus.FREE);
        TimeInterval first = new TimeInterval(t(9), t(10), SlotStatus.FREE);

        List<TimeInterval> result = AvailabilityMerger.merge(List.of(second, first), WINDOW_START, WINDOW_END);

        assertThat(result).containsExactly(new TimeInterval(t(9), t(11), SlotStatus.FREE));
    }
}
