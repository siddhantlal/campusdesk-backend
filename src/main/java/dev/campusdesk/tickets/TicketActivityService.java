package dev.campusdesk.tickets;

import dev.campusdesk.auth.CurrentUser;
import dev.campusdesk.common.*;
import dev.campusdesk.users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static dev.campusdesk.tickets.TicketDtos.*;

@Service
public class TicketActivityService {
    private final TicketService access;
    private final TicketRepository tickets;
    private final CommentRepository comments;
    private final EventRepository events;
    private final UserRepository users;
    public TicketActivityService(TicketService access,TicketRepository tickets,CommentRepository comments,EventRepository events,UserRepository users) {
        this.access=access; this.tickets=tickets; this.comments=comments; this.events=events; this.users=users;
    }
    @Transactional
    public CommentResponse comment(long id,AddComment request,CurrentUser caller) {
        Ticket ticket=access.visible(id,caller); access.mutable(ticket,request.version());
        // Dirty the parent to include comments in optimistic locking against status/assignment changes.
        ticket.touch();
        TicketComment comment=comments.save(new TicketComment(ticket,users.getReferenceById(caller.id()),request.body()));
        access.record(ticket,caller,EventType.COMMENT_CREATED,null,comment.getId().toString(),null);
        tickets.flush();
        return CommentResponse.of(comment);
    }
    @Transactional(readOnly=true)
    public PageResponse<CommentResponse> comments(long id,int page,int size,CurrentUser caller) {
        access.visible(id,caller);
        return PageResponse.of(comments.findByTicketId(id,Paging.request(page,size,"createdAt","asc")).map(CommentResponse::of));
    }
    @Transactional(readOnly=true)
    public PageResponse<EventResponse> events(long id,int page,int size,CurrentUser caller) {
        access.visible(id,caller);
        return PageResponse.of(events.findByTicketId(id,Paging.request(page,size,"createdAt","asc")).map(EventResponse::of));
    }
}
