package com.ccbsa.wms.user.application.api.mapper;

import org.springframework.stereotype.Component;

import com.ccbsa.wms.user.application.api.dto.LoginRequest;
import com.ccbsa.wms.user.application.api.dto.LoginResponse;
import com.ccbsa.wms.user.application.api.dto.RefreshTokenRequest;
import com.ccbsa.wms.user.application.api.dto.UserContextResponse;
import com.ccbsa.wms.user.application.service.command.dto.AuthenticationResult;
import com.ccbsa.wms.user.application.service.command.dto.LoginCommand;
import com.ccbsa.wms.user.application.service.command.dto.RefreshTokenCommand;
import com.ccbsa.wms.user.application.service.query.dto.UserContextQuery;
import com.ccbsa.wms.user.application.service.query.dto.UserContextView;

/**
 * Mapper: AuthMapper
 * <p>
 * Maps between API DTOs and application service DTOs. Anti-corruption layer between API and application service layers.
 */
@Component
public class AuthMapper {
    public LoginCommand toLoginCommand(LoginRequest request) {
        return new LoginCommand(request.getUsername(), request.getPassword());
    }

    public RefreshTokenCommand toRefreshTokenCommand(RefreshTokenRequest request) {
        return new RefreshTokenCommand(request.getRefreshToken());
    }

    public LoginResponse toLoginResponse(AuthenticationResult result) {
        AuthenticationResult.UserContext context = result.getUserContext();
        // Convert value objects to String for API DTO (anti-corruption layer)
        // TenantId may be null for SYSTEM_ADMIN users
        LoginResponse.UserContext apiContext = new LoginResponse.UserContext(context.getUserId()
                .getValue(), context.getUsername(), context.getTenantId() != null ? context.getTenantId()
                .getValue() : null, context.getRoles(), context.getEmail(),
                context.getFirstName(), context.getLastName());

        return new LoginResponse(result.getAccessToken(), result.getRefreshToken(), result.getTokenType(), result.getExpiresIn(), apiContext);
    }

    public UserContextResponse toUserContextResponse(UserContextView view) {
        // Convert value objects to String for API DTO (anti-corruption layer)
        // TenantId may be null for SYSTEM_ADMIN users
        return new UserContextResponse(view.getUserId()
                .getValue(), view.getUsername(), view.getTenantId() != null ? view.getTenantId()
                .getValue() : null, view.getRoles(), view.getEmail(), view.getFirstName(), view.getLastName());
    }

    public UserContextQuery toUserContextQuery(String accessToken) {
        return new UserContextQuery(accessToken);
    }
}

