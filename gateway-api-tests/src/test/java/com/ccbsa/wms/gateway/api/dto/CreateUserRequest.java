package com.ccbsa.wms.gateway.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
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

