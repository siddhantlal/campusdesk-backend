package dev.campusdesk.tickets;

import dev.campusdesk.auth.CurrentUser;
import dev.campusdesk.categories.Category;
import dev.campusdesk.common.ApiException;
import dev.campusdesk.users.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class TicketWorkflowTest {
    @Test void lifecycleRequiresResolutionAndReopeningReason() {
        var ticket=new Ticket("Wi-Fi","Library connectivity",new Category("IT","Technology"),new User("Student","s@example.test","hash",Role.STUDENT),Priority.HIGH);
        var admin=new CurrentUser(1,Role.ADMIN);
        var progress=new TicketDtos.StatusChange(TicketStatus.IN_PROGRESS,null,null,0L);
        TicketWorkflow.validate(ticket,admin,progress); ticket.transition(progress.status(),null);
        assertThatThrownBy(() -> TicketWorkflow.validate(ticket,admin,new TicketDtos.StatusChange(TicketStatus.RESOLVED," ",null,0L)))
            .isInstanceOf(ApiException.class).hasMessageContaining("summary");
        var resolved=new TicketDtos.StatusChange(TicketStatus.RESOLVED,"Access point repaired",null,0L);
        TicketWorkflow.validate(ticket,admin,resolved); ticket.transition(resolved.status(),resolved.resolutionSummary());
        assertThat(ticket.getResolvedAt()).isNotNull();
        assertThatThrownBy(() -> TicketWorkflow.validate(ticket,admin,new TicketDtos.StatusChange(TicketStatus.OPEN,null,null,0L)))
            .isInstanceOf(ApiException.class).hasMessageContaining("reason");
        var reopen=new TicketDtos.StatusChange(TicketStatus.OPEN,null,"Still failing",0L);
        TicketWorkflow.validate(ticket,admin,reopen); ticket.transition(reopen.status(),null);
        assertThat(ticket.getResolutionSummary()).isNull(); assertThat(ticket.getResolvedAt()).isNull();
    }
    @Test void evenAdministratorsCannotSkipLifecycle() {
        var ticket=new Ticket("Fan","Fan broken",new Category("Facilities","Repairs"),new User("S","s@example.test","hash",Role.STUDENT),Priority.LOW);
        assertThatThrownBy(() -> TicketWorkflow.validate(ticket,new CurrentUser(1,Role.ADMIN),new TicketDtos.StatusChange(TicketStatus.CLOSED,null,null,0L)))
            .isInstanceOf(ApiException.class).hasMessageContaining("Cannot move from OPEN to CLOSED");
    }
}
