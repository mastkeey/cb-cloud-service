package ru.mastkey.cloudservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ru.mastkey.cloudservice.exception.ErrorType;
import ru.mastkey.cloudservice.exception.ServiceException;
import ru.mastkey.cloudservice.security.JwtService;
import ru.mastkey.cloudservice.service.HttpContextService;

import java.util.UUID;

import static ru.mastkey.cloudservice.util.Constants.MSG_JWT_ERROR;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpContextServiceImpl implements HttpContextService {

    private final JwtService jwtService;

    @Override
    public UUID getUserIdFromJwtToken() {
        log.info("Extracting user ID from JWT token");
        var token = extractJwtFromSecurityContext();

        if (token == null || token.isEmpty()) {
            log.error("JWT token is missing or empty in security context");
            throw new ServiceException(ErrorType.FORBIDDEN, MSG_JWT_ERROR);
        }

        try {
            var userId = UUID.fromString(jwtService.extractClaim(token, claims -> claims.get("user_id", String.class)));
            log.info("Successfully extracted user ID: {}", userId);
            return userId;
        } catch (Exception e) {
            log.error("Failed to extract user ID from JWT token: {}", e.getMessage(), e);
            throw new ServiceException(ErrorType.FORBIDDEN, MSG_JWT_ERROR, e);
        }
    }

    private String extractJwtFromSecurityContext() {
        log.debug("Attempting to extract JWT token from security context");
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() instanceof String) {
            var token = (String) authentication.getCredentials();
            log.debug("JWT token extracted from security context: {}", token);
            return token;
        }
        log.warn("No authentication or credentials found in security context");
        return null;
    }
}