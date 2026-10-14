package dev.campusdesk.users;

import dev.campusdesk.auth.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import static dev.campusdesk.users.UserDtos.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService users;
    public UserController(UserService users) { this.users = users; }
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal CurrentUser caller) { return users.get(caller.id()); }
}
