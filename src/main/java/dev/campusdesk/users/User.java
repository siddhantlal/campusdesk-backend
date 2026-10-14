package dev.campusdesk.users;

import java.time.Instant;
import java.util.Locale;
import jakarta.persistence.*;

@Entity
@Table(name = "app_users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, unique = true, length = 254) private String email;
    @Column(nullable = false, length = 100) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Role role;
    @Column(nullable = false) private boolean active;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @Version private long version;
    protected User() {}
    public User(String name, String email, String passwordHash, Role role) {
        this.name = name.trim(); this.email = normalizeEmail(email); this.passwordHash = passwordHash;
        this.role = role; this.active = true; this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }
    public static String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    public void setActive(boolean active) { this.active = active; this.updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
}
