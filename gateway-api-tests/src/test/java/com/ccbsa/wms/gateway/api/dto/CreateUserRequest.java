package com.ccbsa.wms.gateway.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    private String tenantId;
    private String username;
    @JsonProperty("emailAddress")
    private String emailAddress;
    private String password;
    private String firstName;
    private String lastName;
    private List<String> roles;
}

