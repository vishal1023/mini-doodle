package com.doodle.scheduler.meeting;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "meetings")
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "slot_id", nullable = false)
    private UUID slotId;

    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingStatus status = MeetingStatus.SCHEDULED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    // Pure join table, no attributes of its own beyond the two FKs, so an
    // @ElementCollection is a natural fit - no separate entity class needed.
    // Organizer is never included here; organizerId is the sole source of
    // truth for who booked the meeting.
    @ElementCollection
    @CollectionTable(name = "meeting_participants", joinColumns = @JoinColumn(name = "meeting_id"))
    @Column(name = "user_id")
    private Set<UUID> participantIds = new HashSet<>();

    protected Meeting() {
    }

    public Meeting(UUID slotId, UUID organizerId, String title, String description, Set<UUID> participantIds) {
        this.slotId = slotId;
        this.organizerId = organizerId;
        this.title = title;
        this.description = description;
        this.participantIds = participantIds;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getSlotId() {
        return slotId;
    }

    public UUID getOrganizerId() {
        return organizerId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public MeetingStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<UUID> getParticipantIds() {
        return participantIds;
    }

    public boolean isCancelled() {
        return status == MeetingStatus.CANCELLED;
    }

    public void cancel() {
        this.status = MeetingStatus.CANCELLED;
        this.cancelledAt = Instant.now();
    }

    public void updateDetails(String title, String description, Set<UUID> participantIds) {
        this.title = title;
        this.description = description;
        // Mutate in place rather than reassigning the field - Hibernate
        // manages this collection via a wrapper tied to the original
        // instance, so a fresh Set here would break dirty-checking.
        this.participantIds.clear();
        this.participantIds.addAll(participantIds);
    }
}
