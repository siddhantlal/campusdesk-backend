package dev.campusdesk.users;

import jakarta.validation.Valid;
import dev.campusdesk.auth.CurrentUser;
import dev.campusdesk.common.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import static dev.campusdesk.users.UserDtos.*;

@RestController @RequestMapping("/api/v1/admin") @PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserService users;
    private final AdminService admin;
    public AdminController(UserService users,AdminService admin) { this.users=users; this.admin=admin; }
    @PostMapping("/staff") @ResponseStatus(HttpStatus.CREATED)
    public UserResponse staff(@Valid @RequestBody AccountRequest request) { return users.create(request,Role.STAFF); }
    @GetMapping("/users")
    public PageResponse<UserResponse> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
            @RequestParam(required=false) Role role,@RequestParam(required=false) Boolean active,@RequestParam(required=false) String search) {
        return users.list(page,size,role,active,search);
    }
    @PatchMapping("/users/{id}/status")
    public UserResponse status(@PathVariable long id,@Valid @RequestBody AccountStatus request,@AuthenticationPrincipal CurrentUser caller) { return users.setActive(id,request,caller.id()); }
    @GetMapping("/statistics")
    public AdminService.Statistics statistics() { return admin.statistics(); }
}
