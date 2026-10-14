package dev.campusdesk.tickets;

import java.time.Instant;
import jakarta.validation.constraints.*;
import dev.campusdesk.users.User;

public final class TicketDtos {
    private TicketDtos() {}
    public record CreateTicket(@NotBlank @Size(max=160) String title,@NotBlank @Size(max=5000) String description,
                               @NotNull @Positive Long categoryId,@NotNull Priority priority) {}
    public record Assignment(@NotNull @Positive Long assigneeId,@NotNull @PositiveOrZero Long version) {}
    public record StatusChange(@NotNull TicketStatus status,@Size(max=2000) String resolutionSummary,
                                @Size(max=2000) String reason,@NotNull @PositiveOrZero Long version) {}
    public record AddComment(@NotBlank @Size(max=2000) String body,@NotNull @PositiveOrZero Long version) {}
    public record Person(Long id,String name) {
        static Person of(User user) { return user==null ? null : new Person(user.getId(),user.getName()); }
    }
    public record TicketResponse(Long id,String title,String description,Long categoryId,String categoryName,
                                  Person creator,Person assignee,Priority priority,TicketStatus status,String resolutionSummary,
                                  Instant createdAt,Instant updatedAt,Instant resolvedAt,Instant closedAt,long version) {
        public static TicketResponse of(Ticket t) {
            return new TicketResponse(t.getId(),t.getTitle(),t.getDescription(),t.getCategory().getId(),t.getCategory().getName(),
                Person.of(t.getCreator()),Person.of(t.getAssignee()),t.getPriority(),t.getStatus(),t.getResolutionSummary(),
                t.getCreatedAt(),t.getUpdatedAt(),t.getResolvedAt(),t.getClosedAt(),t.getVersion());
        }
    }
    public record CommentResponse(Long id,Person author,String body,Instant createdAt) {
        static CommentResponse of(TicketComment c) { return new CommentResponse(c.getId(),Person.of(c.getAuthor()),c.getBody(),c.getCreatedAt()); }
    }
    public record EventResponse(Long id,Person actor,EventType eventType,String previousValue,String newValue,String reason,Instant createdAt) {
        static EventResponse of(TicketEvent e) { return new EventResponse(e.getId(),Person.of(e.getActor()),e.getEventType(),e.getPreviousValue(),e.getNewValue(),e.getReason(),e.getCreatedAt()); }
    }
}
