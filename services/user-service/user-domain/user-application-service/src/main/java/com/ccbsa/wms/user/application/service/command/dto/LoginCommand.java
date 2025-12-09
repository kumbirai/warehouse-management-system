package com.ccbsa.wms.user.application.service.command.dto;

import com.ccbsa.common.application.command.Command;

/**
 * Command: LoginCommand
 * <p>
 * Represents the intent to authenticate a user.
 */
public final class LoginCommand implements Command {
    private final String username;
    private final String password;

    public LoginCommand(String username,
                        String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank");
        }
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return String.format("LoginCommand{username='%s'}",
                username);
    }
}

