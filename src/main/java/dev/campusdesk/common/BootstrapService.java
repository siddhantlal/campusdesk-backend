package dev.campusdesk.common;

import dev.campusdesk.users.*;
import dev.campusdesk.categories.*;
import dev.campusdesk.tickets.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class BootstrapService {
    private final UserRepository users;
    private final CategoryRepository categories;
    private final TicketRepository tickets;
    private final EventRepository events;
    private final PasswordEncoder encoder;
    public BootstrapService(UserRepository users,CategoryRepository categories,TicketRepository tickets,EventRepository events,PasswordEncoder encoder) {
        this.users=users; this.categories=categories; this.tickets=tickets; this.events=events; this.encoder=encoder;
    }
    @Transactional
    public void admin(String name,String email,String password) {
        if (users.existsByRole(Role.ADMIN)) return;
        if (name.isBlank() || name.length()>100 || email.length()>254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new IllegalStateException("Set valid ADMIN_NAME and ADMIN_EMAIL to bootstrap the first administrator");
        UserService.validatePassword(password);
        if (users.existsByEmail(User.normalizeEmail(email))) throw new IllegalStateException("Bootstrap email already belongs to another account");
        users.save(new User(name,email,encoder.encode(password),Role.ADMIN));
    }
    @Transactional
    public void demo(String password) {
        // Synthetic account marker makes repeated local starts idempotent.
        if (users.existsByEmail("student.demo@campusdesk.test")) return;
        UserService.validatePassword(password);
        String hash=encoder.encode(password);
        User student=users.save(new User("Demo Student","student.demo@campusdesk.test",hash,Role.STUDENT));
        users.save(new User("Demo Staff","staff.demo@campusdesk.test",hash,Role.STAFF));
        var category=categories.save(new Category("Demo Facilities","Synthetic campus facility issues"));
        for (int i=1;i<=12;i++) {
            Ticket ticket=tickets.save(new Ticket("Demo facility request " + i,"Synthetic example for learning the API",category,student,Priority.MEDIUM));
            events.save(new TicketEvent(ticket,student,EventType.CREATED,null,"OPEN",null));
        }
    }
}
