package com.doodle.scheduler.meeting;

import com.doodle.scheduler.generated.model.CreateMeetingRequestDto;
import com.doodle.scheduler.slot.Slot;
import com.doodle.scheduler.slot.SlotRepository;
import com.doodle.scheduler.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class MeetingService {

    private final SlotRepository slotRepository;
    private final UserRepository userRepository;
    private final MeetingRepository meetingRepository;

    public MeetingService(SlotRepository slotRepository, UserRepository userRepository,
                           MeetingRepository meetingRepository) {
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
        this.meetingRepository = meetingRepository;
    }

    @Transactional
    public BookingOutcome bookSlot(UUID userId, UUID slotId, CreateMeetingRequestDto request) {
        if (!userRepository.existsById(userId)) {
            return new BookingOutcome.NotFound();
        }

        Set<UUID> participantIds = new HashSet<>(request.getParticipantUserIds());
        if (userRepository.countByIdIn(participantIds) != participantIds.size()) {
            return new BookingOutcome.InvalidParticipants();
        }

        Optional<Slot> maybeSlot = slotRepository.findByIdForUpdate(slotId)
                .filter(slot -> slot.getUserId().equals(userId));
        if (maybeSlot.isEmpty()) {
            return new BookingOutcome.NotFound();
        }

        Slot slot = maybeSlot.get();
        if (!slot.isFree()) {
            return new BookingOutcome.Conflict();
        }

        slot.markBusy();
        Meeting meeting = meetingRepository.save(
                new Meeting(slot.getId(), userId, request.getTitle(), request.getDescription(), participantIds));

        return new BookingOutcome.Booked(MeetingMapper.toDto(meeting));
    }
}
