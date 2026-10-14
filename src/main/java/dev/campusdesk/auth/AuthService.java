package dev.campusdesk.auth;

import dev.campusdesk.common.ApiException;
import dev.campusdesk.users.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static dev.campusdesk.users.UserDtos.*;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService tokens;
    private final String dummyHash;
    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService tokens) {
        this.users = users; this.encoder = encoder; this.tokens = tokens;
        dummyHash = encoder.encode(java.util.UUID.randomUUID().toString());
    }
    @Transactional(readOnly=true)
    public AuthController.LoginResponse login(String email, String password) {
        User user = users.findByEmail(User.normalizeEmail(email)).orElse(null);
        boolean matches = encoder.matches(password,user == null ? dummyHash : user.getPasswordHash());
        if (user == null || !matches || !user.isActive())
            throw new ApiException(401,"INVALID_CREDENTIALS","Email or password is incorrect");
        return new AuthController.LoginResponse(tokens.issue(user),"Bearer",900,UserResponse.of(user));
    }
}
