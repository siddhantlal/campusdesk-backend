package dev.campusdesk.tickets;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;

public interface TicketRepository extends JpaRepository<Ticket,Long>, JpaSpecificationExecutor<Ticket> {
    @Override @EntityGraph(attributePaths={"category","creator","assignee"})
    Optional<Ticket> findById(Long id);
    @Override @EntityGraph(attributePaths={"category","creator","assignee"})
    Page<Ticket> findAll(Specification<Ticket> spec,Pageable pageable);
    @Query("select t.status as status, count(t) as count from Ticket t group by t.status")
    List<StatusCount> countByStatus();
    long countByAssigneeIsNullAndStatusNot(TicketStatus status);
    interface StatusCount { TicketStatus getStatus(); long getCount(); }
}
