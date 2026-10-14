package dev.campusdesk.tickets;

import dev.campusdesk.auth.CurrentUser;
import dev.campusdesk.common.ApiException;
import dev.campusdesk.users.Role;
import static dev.campusdesk.tickets.TicketStatus.*;

public final class TicketWorkflow {
    private TicketWorkflow() {}
    public static void validate(Ticket ticket,CurrentUser caller,TicketDtos.StatusChange request) {
        TicketStatus from=ticket.getStatus(), to=request.status();
        boolean allowed=(from==OPEN && to==IN_PROGRESS) || (from==IN_PROGRESS && to==RESOLVED)
                || (from==RESOLVED && (to==CLOSED || to==OPEN));
        if (!allowed) throw new ApiException(409,"INVALID_TRANSITION","Cannot move from " + from + " to " + to);
        boolean admin=caller.role()==Role.ADMIN;
        boolean assignedStaff=caller.role()==Role.STAFF && ticket.getAssignee()!=null && ticket.getAssignee().getId()==caller.id();
        boolean owner=caller.role()==Role.STUDENT && ticket.getCreator().getId()==caller.id();
        if ((to==IN_PROGRESS || to==RESOLVED) ? !(admin || assignedStaff) : !(admin || owner))
            throw new ApiException(403,"FORBIDDEN","Your role cannot perform this transition");
        if (to==RESOLVED && (request.resolutionSummary()==null || request.resolutionSummary().isBlank()))
            throw new ApiException(400,"RESOLUTION_REQUIRED","A resolution summary is required");
        if (to==OPEN && (request.reason()==null || request.reason().isBlank()))
            throw new ApiException(400,"REOPEN_REASON_REQUIRED","A reopening reason is required");
        if (to!=RESOLVED && request.resolutionSummary()!=null)
            throw new ApiException(400,"INVALID_REQUEST","Resolution summary is only accepted when resolving");
        if (to!=OPEN && request.reason()!=null)
            throw new ApiException(400,"INVALID_REQUEST","Reason is only accepted when reopening");
    }
}
