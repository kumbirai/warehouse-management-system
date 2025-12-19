package com.ccbsa.wms.gateway.api.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String tenantId;
    private List<String> roles;
}

