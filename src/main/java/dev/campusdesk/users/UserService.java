package dev.campusdesk.users;

import java.nio.charset.StandardCharsets;
import dev.campusdesk.common.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static dev.campusdesk.users.UserDtos.*;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    public UserService(UserRepository users, PasswordEncoder encoder) { this.users = users; this.encoder = encoder; }
    @Transactional
    public UserResponse create(AccountRequest request, Role role) {
        String email = User.normalizeEmail(request.email());
        if (users.existsByEmail(email)) throw new ApiException(409, "EMAIL_EXISTS", "Email is already registered");
        validatePassword(request.password());
        return UserResponse.of(users.saveAndFlush(new User(request.name(), email, encoder.encode(request.password()), role)));
    }
    public static void validatePassword(String password) {
        // BCrypt truncates after 72 bytes; reject instead of silently accepting a truncated secret.
        if (password == null || password.length() < 12 || password.length() > 64 || password.isBlank()
                || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ApiException(400,"INVALID_PASSWORD","Password must be 12..64 characters and at most 72 UTF-8 bytes");
    }
    @Transactional(readOnly=true)
    public UserResponse get(long id) { return UserResponse.of(users.findById(id).orElseThrow(() -> ApiException.missing("User"))); }
    @Transactional(readOnly=true)
    public PageResponse<UserResponse> list(int page, int size, Role role, Boolean active, String search) {
        Paging.request(page,size,"createdAt","desc");
        if (search != null && search.length() > 100) throw new ApiException(400, "INVALID_SEARCH", "User search must be at most 100 characters");
        org.springframework.data.jpa.domain.Specification<User> filter = (root, query, cb) -> {
            var conditions = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (role != null) conditions.add(cb.equal(root.get("role"), role));
            if (active != null) conditions.add(cb.equal(root.get("active"), active));
            if (search != null && !search.isBlank()) {
                String term = search.trim().toLowerCase(java.util.Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                conditions.add(cb.or(cb.like(cb.lower(root.get("name")), "%" + term + "%", '\\'),
                    cb.like(cb.lower(root.get("email")), "%" + term + "%", '\\')));
            }
            return cb.and(conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return PageResponse.of(users.findAll(filter, PageRequest.of(page,size,Sort.by("id"))).map(UserResponse::of));
    }
    @Transactional
    public UserResponse setActive(long id, AccountStatus request, long callerId) {
        User user = users.findById(id).orElseThrow(() -> ApiException.missing("User"));
        if (user.getVersion() != request.version()) throw ApiException.conflict();
        if (id == callerId && !request.active())
            throw new ApiException(409, "SELF_DEACTIVATION", "An administrator cannot deactivate their own account");
        user.setActive(request.active());
        users.flush();
        return UserResponse.of(user);
    }
}
