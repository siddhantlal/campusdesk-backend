package dev.campusdesk.auth;

import dev.campusdesk.users.Role;

public record CurrentUser(long id, Role role) {}
