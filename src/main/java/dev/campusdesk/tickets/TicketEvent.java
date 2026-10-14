package dev.campusdesk.tickets;

import java.time.Instant;
import jakarta.persistence.*;
import dev.campusdesk.users.User;
import org.hibernate.annotations.Immutable;

@Entity @Immutable @Table(name="ticket_events")
public class TicketEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="ticket_id") private Ticket ticket;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="actor_id") private User actor;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private EventType eventType;
    @Column(length=100) private String previousValue;
    @Column(length=100) private String newValue;
    @Column(length=2000) private String reason;
    @Column(nullable=false) private Instant createdAt;
    protected TicketEvent() {}
    public TicketEvent(Ticket ticket,User actor,EventType type,String previousValue,String newValue,String reason) {
        this.ticket=ticket; this.actor=actor; this.eventType=type; this.previousValue=previousValue; this.newValue=newValue;
        this.reason=reason; this.createdAt=Instant.now();
    }
    public Long getId() { return id; }
    public User getActor() { return actor; }
    public EventType getEventType() { return eventType; }
    public String getPreviousValue() { return previousValue; }
    public String getNewValue() { return newValue; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
