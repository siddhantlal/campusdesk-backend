package dev.campusdesk.tickets;

import java.util.*;
import dev.campusdesk.auth.CurrentUser;
import dev.campusdesk.categories.CategoryRepository;
import dev.campusdesk.common.*;
import dev.campusdesk.users.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static dev.campusdesk.tickets.TicketDtos.*;

@Service
public class TicketService {
    private final TicketRepository tickets;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final EventRepository events;
    public TicketService(TicketRepository tickets,CategoryRepository categories,UserRepository users,EventRepository events) {
        this.tickets=tickets; this.categories=categories; this.users=users; this.events=events;
    }
    @Transactional
    public TicketResponse create(CreateTicket request,CurrentUser caller) {
        var category=categories.findById(request.categoryId()).filter(c -> c.isActive())
                .orElseThrow(() -> new ApiException(400,"INVALID_CATEGORY","Select an existing active category"));
        User actor=users.getReferenceById(caller.id());
        Ticket ticket=tickets.save(new Ticket(request.title(),request.description(),category,actor,request.priority()));
        record(ticket,caller,EventType.CREATED,null,"OPEN",null);
        tickets.flush();
        return TicketResponse.of(ticket);
    }
    @Transactional(readOnly=true)
    public TicketResponse get(long id,CurrentUser caller) { return TicketResponse.of(visible(id,caller)); }
    @Transactional(readOnly=true)
    public PageResponse<TicketResponse> list(CurrentUser caller,TicketStatus status,Priority priority,Long categoryId,
            Long assigneeId,String title,int page,int size,String sort,String direction) {
        var paging=Paging.request(page,size,sort,direction);
        if (assigneeId!=null && caller.role()!=Role.ADMIN)
            throw new ApiException(403,"FORBIDDEN","Assignee filtering is only available to administrators");
        if (title!=null && title.length()>160) throw new ApiException(400,"INVALID_SEARCH","Search title must be at most 160 characters");
        Specification<Ticket> spec=(root,query,cb) -> {
            var predicates=new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (caller.role()==Role.STUDENT) predicates.add(cb.equal(root.get("creator").get("id"),caller.id()));
            if (caller.role()==Role.STAFF) predicates.add(cb.equal(root.get("assignee").get("id"),caller.id()));
            if (status!=null) predicates.add(cb.equal(root.get("status"),status));
            if (priority!=null) predicates.add(cb.equal(root.get("priority"),priority));
            if (categoryId!=null) predicates.add(cb.equal(root.get("category").get("id"),categoryId));
            if (assigneeId!=null) predicates.add(cb.equal(root.get("assignee").get("id"),assigneeId));
            if (title!=null && !title.isBlank()) {
                // Escape SQL LIKE wildcards so the search behaves as a literal substring.
                String term=title.trim().toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_");
                predicates.add(cb.like(cb.lower(root.get("title")),"%" + term + "%",'\\'));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        // The specification also scopes the count query; no all-campus totals leak to students/staff.
        return PageResponse.of(tickets.findAll(spec,paging).map(TicketResponse::of));
    }
    @Transactional
    public TicketResponse assign(long id,Assignment request,CurrentUser caller) {
        Ticket ticket=visible(id,caller); mutable(ticket,request.version());
        User staff=users.findById(request.assigneeId()).filter(u -> u.isActive() && u.getRole()==Role.STAFF)
                .orElseThrow(() -> new ApiException(400,"INVALID_ASSIGNEE","Assignee must be an active staff account"));
        Long previous=ticket.getAssignee()==null ? null : ticket.getAssignee().getId();
        if (Objects.equals(previous,staff.getId())) throw new ApiException(409,"ALREADY_ASSIGNED","Ticket is already assigned to this staff member");
        ticket.assign(staff);
        record(ticket,caller,previous==null ? EventType.ASSIGNED : EventType.REASSIGNED,
                previous==null ? null : previous.toString(),staff.getId().toString(),null);
        tickets.flush();
        return TicketResponse.of(ticket);
    }
    @Transactional
    public TicketResponse changeStatus(long id,StatusChange request,CurrentUser caller) {
        Ticket ticket=visible(id,caller); mutable(ticket,request.version());
        TicketWorkflow.validate(ticket,caller,request);
        TicketStatus previous=ticket.getStatus();
        ticket.transition(request.status(),request.resolutionSummary());
        boolean reopened=request.status()==TicketStatus.OPEN;
        record(ticket,caller,reopened ? EventType.REOPENED : EventType.STATUS_CHANGED,previous.name(),request.status().name(),
            reopened ? request.reason().trim() : request.status()==TicketStatus.RESOLVED ? request.resolutionSummary().trim() : null);
        tickets.flush();
        return TicketResponse.of(ticket);
    }
    Ticket visible(long id,CurrentUser caller) {
        Ticket ticket=tickets.findById(id).orElseThrow(() -> ApiException.missing("Ticket"));
        boolean allowed=switch(caller.role()) {
            case ADMIN -> true;
            case STUDENT -> ticket.getCreator().getId()==caller.id();
            case STAFF -> ticket.getAssignee()!=null && ticket.getAssignee().getId()==caller.id();
        };
        if (!allowed) throw ApiException.missing("Ticket");
        return ticket;
    }
    void mutable(Ticket ticket,long version) {
        if (ticket.getStatus()==TicketStatus.CLOSED) throw new ApiException(409,"TICKET_CLOSED","Closed tickets cannot be modified");
        if (ticket.getVersion()!=version) throw ApiException.conflict();
    }
    void record(Ticket ticket,CurrentUser actor,EventType type,String previous,String next,String reason) {
        events.save(new TicketEvent(ticket,users.getReferenceById(actor.id()),type,previous,next,reason));
    }
}
