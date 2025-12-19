package com.ccbsa.wms.gateway.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String userId;
    private String username;
    @JsonProperty("emailAddress")
    private String emailAddress;
    private String firstName;
    private String lastName;
    private String tenantId;
    private String status;
    private List<String> roles;
    private String createdAt;
    private String updatedAt;
}

