package dev.campusdesk.tickets;

import java.time.Instant;
import jakarta.persistence.*;
import dev.campusdesk.categories.Category;
import dev.campusdesk.users.User;

@Entity @Table(name="tickets")
public class Ticket {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=160) private String title;
    @Column(nullable=false,length=5000) private String description;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="category_id") private Category category;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="creator_id") private User creator;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="assignee_id") private User assignee;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private Priority priority;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private TicketStatus status;
    @Column(length=2000) private String resolutionSummary;
    @Column(nullable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    private Instant resolvedAt;
    private Instant closedAt;
    @Version private long version;
    protected Ticket() {}
    public Ticket(String title,String description,Category category,User creator,Priority priority) {
        this.title=title.trim(); this.description=description.trim(); this.category=category;
        this.creator=creator; this.priority=priority; this.status=TicketStatus.OPEN;
        this.createdAt=Instant.now(); this.updatedAt=createdAt;
    }
    public void assign(User staff) { this.assignee=staff; touch(); }
    public void transition(TicketStatus target,String summary) {
        status=target; touch();
        if (target==TicketStatus.RESOLVED) { resolutionSummary=summary.trim(); resolvedAt=updatedAt; }
        if (target==TicketStatus.OPEN) { resolutionSummary=null; resolvedAt=null; }
        if (target==TicketStatus.CLOSED) closedAt=updatedAt;
    }
    public void touch() { updatedAt=Instant.now(); }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public User getCreator() { return creator; }
    public User getAssignee() { return assignee; }
    public Priority getPriority() { return priority; }
    public TicketStatus getStatus() { return status; }
    public String getResolutionSummary() { return resolutionSummary; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getClosedAt() { return closedAt; }
    public long getVersion() { return version; }
}
