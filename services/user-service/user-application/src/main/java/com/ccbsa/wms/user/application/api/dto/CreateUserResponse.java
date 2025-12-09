package com.ccbsa.wms.user.application.api.dto;

/**
 * Response DTO for user creation.
 */
public class CreateUserResponse {
    private String userId;
    private boolean success;
    private String message;

    public CreateUserResponse() {
    }

    public CreateUserResponse(String userId, boolean success, String message) {
        this.userId = userId;
        this.success = success;
        this.message = message;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

