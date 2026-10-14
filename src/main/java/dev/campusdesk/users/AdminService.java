package dev.campusdesk.users;

import java.util.*;
import dev.campusdesk.tickets.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final TicketRepository tickets;
    private final UserRepository users;
    public AdminService(TicketRepository tickets,UserRepository users) { this.tickets=tickets; this.users=users; }
    public record Statistics(long totalTickets,Map<TicketStatus,Long> byStatus,long unassignedTickets,long activeStaff) {}
    @Transactional(readOnly=true)
    public Statistics statistics() {
        Map<TicketStatus,Long> counts=new EnumMap<>(TicketStatus.class);
        for (TicketStatus status:TicketStatus.values()) counts.put(status,0L);
        tickets.countByStatus().forEach(row -> counts.put(row.getStatus(),row.getCount()));
        return new Statistics(counts.values().stream().mapToLong(Long::longValue).sum(),counts,
            tickets.countByAssigneeIsNullAndStatusNot(TicketStatus.CLOSED),users.countByRoleAndActiveTrue(Role.STAFF));
    }
}
