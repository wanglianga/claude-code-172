package com.example.robotdelivery.auth;

public record CurrentUser(Long id, String username, String displayName, Role role) {
}
