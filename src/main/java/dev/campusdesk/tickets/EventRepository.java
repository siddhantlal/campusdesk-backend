package dev.campusdesk.tickets;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface EventRepository extends JpaRepository<TicketEvent,Long> {
    @EntityGraph(attributePaths="actor")
    Page<TicketEvent> findByTicketId(Long ticketId,Pageable pageable);
}
