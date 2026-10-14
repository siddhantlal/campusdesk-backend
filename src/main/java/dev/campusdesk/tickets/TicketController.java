package dev.campusdesk.tickets;

import jakarta.validation.Valid;
import dev.campusdesk.auth.CurrentUser;
import dev.campusdesk.common.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import static dev.campusdesk.tickets.TicketDtos.*;

@RestController @RequestMapping("/api/v1/tickets")
public class TicketController {
    private final TicketService tickets;
    private final TicketActivityService activity;
    public TicketController(TicketService tickets,TicketActivityService activity) { this.tickets=tickets; this.activity=activity; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('STUDENT')")
    public TicketResponse create(@Valid @RequestBody CreateTicket request,@AuthenticationPrincipal CurrentUser caller) { return tickets.create(request,caller); }
    @GetMapping
    public PageResponse<TicketResponse> list(@AuthenticationPrincipal CurrentUser caller,
            @RequestParam(required=false) TicketStatus status,@RequestParam(required=false) Priority priority,
            @RequestParam(required=false) Long categoryId,@RequestParam(required=false) Long assigneeId,
            @RequestParam(required=false) String title,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
            @RequestParam(defaultValue="createdAt") String sort,@RequestParam(defaultValue="desc") String direction) {
        return tickets.list(caller,status,priority,categoryId,assigneeId,title,page,size,sort,direction);
    }
    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable long id,@AuthenticationPrincipal CurrentUser caller) { return tickets.get(id,caller); }
    @PatchMapping("/{id}/assignment") @PreAuthorize("hasRole('ADMIN')")
    public TicketResponse assign(@PathVariable long id,@Valid @RequestBody Assignment request,@AuthenticationPrincipal CurrentUser caller) { return tickets.assign(id,request,caller); }
    @PatchMapping("/{id}/status")
    public TicketResponse status(@PathVariable long id,@Valid @RequestBody StatusChange request,@AuthenticationPrincipal CurrentUser caller) { return tickets.changeStatus(id,request,caller); }
    @PostMapping("/{id}/comments") @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse comment(@PathVariable long id,@Valid @RequestBody AddComment request,@AuthenticationPrincipal CurrentUser caller) { return activity.comment(id,request,caller); }
    @GetMapping("/{id}/comments")
    public PageResponse<CommentResponse> comments(@PathVariable long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
            @AuthenticationPrincipal CurrentUser caller) { return activity.comments(id,page,size,caller); }
    @GetMapping("/{id}/events")
    public PageResponse<EventResponse> events(@PathVariable long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
            @AuthenticationPrincipal CurrentUser caller) { return activity.events(id,page,size,caller); }
}
