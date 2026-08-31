package com.doodle.scheduler.meeting;

import com.doodle.scheduler.generated.model.CreateMeetingRequestDto;
import com.doodle.scheduler.generated.model.MeetingPageDto;
import com.doodle.scheduler.generated.model.MeetingResponseDto;
import com.doodle.scheduler.generated.model.MeetingRoleDto;
import com.doodle.scheduler.slot.Slot;
import com.doodle.scheduler.slot.SlotRepository;
import com.doodle.scheduler.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Transactional
    public CancellationOutcome cancelMeeting(UUID meetingId) {
        Optional<Meeting> maybeMeeting = meetingRepository.findById(meetingId);
        if (maybeMeeting.isEmpty()) {
            return CancellationOutcome.NOT_FOUND;
        }

        Meeting meeting = maybeMeeting.get();
        if (meeting.isCancelled()) {
            return CancellationOutcome.CONFLICT;
        }

        meeting.cancel();
        // Reuses the same pessimistic lock the booking path relies on, since
        // this is the same slot-state transition in reverse.
        slotRepository.findByIdForUpdate(meeting.getSlotId())
                .ifPresent(Slot::markFree);

        return CancellationOutcome.CANCELLED;
    }

    @Transactional(readOnly = true)
    public Optional<MeetingResponseDto> getMeeting(UUID meetingId) {
        return meetingRepository.findById(meetingId).map(MeetingMapper::toDto);
    }

    @Transactional
    public MeetingUpdateOutcome updateMeeting(UUID meetingId, CreateMeetingRequestDto request) {
        Optional<Meeting> maybeMeeting = meetingRepository.findById(meetingId);
        if (maybeMeeting.isEmpty()) {
            return new MeetingUpdateOutcome.NotFound();
        }

        Meeting meeting = maybeMeeting.get();
        if (meeting.isCancelled()) {
            return new MeetingUpdateOutcome.Conflict();
        }

        Set<UUID> participantIds = new HashSet<>(request.getParticipantUserIds());
        if (userRepository.countByIdIn(participantIds) != participantIds.size()) {
            return new MeetingUpdateOutcome.InvalidParticipants();
        }

        meeting.updateDetails(request.getTitle(), request.getDescription(), participantIds);
        return new MeetingUpdateOutcome.Updated(MeetingMapper.toDto(meeting));
    }

    @Transactional(readOnly = true)
    public Optional<MeetingPageDto> listMeetings(UUID userId, MeetingRoleDto role, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            return Optional.empty();
        }
        Page<Meeting> page = switch (role) {
            case ORGANIZER -> meetingRepository.findByOrganizerId(userId, pageable);
            case PARTICIPANT -> meetingRepository.findByParticipantId(userId, pageable);
            case ANY -> meetingRepository.findByOrganizerIdOrParticipantId(userId, pageable);
        };
        return Optional.of(MeetingMapper.toPageDto(page));
    }
}
