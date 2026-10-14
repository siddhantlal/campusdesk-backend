package dev.campusdesk.users;

import java.time.Instant;
import jakarta.validation.constraints.*;

public final class UserDtos {
    private UserDtos() {}
    public record AccountRequest(@NotBlank @Size(max=100) String name,
                                 @NotBlank @Email @Size(max=254) String email,
                                 @NotBlank @Size(min=12, max=64) String password) {
        @Override public String toString() { return "AccountRequest[redacted]"; }
    }
    public record AccountStatus(@NotNull Boolean active, @NotNull @PositiveOrZero Long version) {}
    public record UserResponse(Long id, String name, String email, Role role, boolean active, Instant createdAt, long version) {
        public static UserResponse of(User u) {
            return new UserResponse(u.getId(),u.getName(),u.getEmail(),u.getRole(),u.isActive(),u.getCreatedAt(),u.getVersion());
        }
    }
}
