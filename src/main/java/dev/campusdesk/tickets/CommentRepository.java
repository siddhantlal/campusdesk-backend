package dev.campusdesk.tickets;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface CommentRepository extends JpaRepository<TicketComment,Long> {
    @EntityGraph(attributePaths="author")
    Page<TicketComment> findByTicketId(Long ticketId,Pageable pageable);
}
