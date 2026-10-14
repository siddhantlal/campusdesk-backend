package dev.campusdesk.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import dev.campusdesk.users.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import static dev.campusdesk.users.UserDtos.*;

@io.swagger.v3.oas.annotations.security.SecurityRequirements
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserService users;
    private final AuthService auth;
    public AuthController(UserService users, AuthService auth) { this.users = users; this.auth = auth; }
    public record LoginRequest(@NotBlank @Email @Size(max=254) String email, @NotBlank @Size(max=64) String password) {
        @Override public String toString() { return "LoginRequest[redacted]"; }
    }
    public record LoginResponse(String accessToken, String tokenType, int expiresIn, UserResponse user) {
        @Override public String toString() { return "LoginResponse[redacted]"; }
    }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody AccountRequest request) { return users.create(request,Role.STUDENT); }
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) { return auth.login(request.email(),request.password()); }
}
