package dev.campusdesk.tickets;

import java.time.Instant;
import jakarta.persistence.*;
import dev.campusdesk.users.User;

@Entity @Table(name="comments")
public class TicketComment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="ticket_id") private Ticket ticket;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="author_id") private User author;
    @Column(nullable=false,length=2000) private String body;
    @Column(nullable=false) private Instant createdAt;
    protected TicketComment() {}
    public TicketComment(Ticket ticket,User author,String body) { this.ticket=ticket; this.author=author; this.body=body.trim(); this.createdAt=Instant.now(); }
    public Long getId() { return id; }
    public User getAuthor() { return author; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}
