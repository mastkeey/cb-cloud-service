package ru.mastkey.cloudservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.mastkey.cloudservice.exception.ErrorType;
import ru.mastkey.cloudservice.exception.ServiceException;
import ru.mastkey.cloudservice.security.JwtService;
import ru.mastkey.cloudservice.service.impl.HttpContextServiceImpl;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpContextServiceImplTest {

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private HttpContextServiceImpl httpContextService;

    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        securityContext = SecurityContextHolder.createEmptyContext();
    }

    @Test
    void getUserIdFromJwtToken_ShouldReturnUserId_WhenTokenIsValid() {
        UUID userId = UUID.randomUUID();
        String jwt = "test.jwt.token";

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("principal", jwt, null));
        SecurityContextHolder.setContext(securityContext);

        when(jwtService.extractClaim(eq(jwt), any())).thenReturn(userId.toString());

        UUID result = httpContextService.getUserIdFromJwtToken();

        assertThat(result).isEqualTo(userId);
        verify(jwtService).extractClaim(eq(jwt), any());
    }

    @Test
    void getUserIdFromJwtToken_ShouldThrowServiceException_WhenTokenIsNull() {
        securityContext.setAuthentication(null);
        SecurityContextHolder.setContext(securityContext);

        ServiceException exception = assertThrows(
                ServiceException.class,
                httpContextService::getUserIdFromJwtToken
        );

        assertThat(exception.getMessage()).isEqualTo("Error while extracting JWT token");
        assertThat(exception.getCode()).isEqualTo(ErrorType.FORBIDDEN.getCode());
    }

    @Test
    void getUserIdFromJwtToken_ShouldThrowServiceException_WhenCredentialsAreNotString() {
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("principal", 12345, null));
        SecurityContextHolder.setContext(securityContext);

        ServiceException exception = assertThrows(
                ServiceException.class,
                httpContextService::getUserIdFromJwtToken
        );

        assertThat(exception.getMessage()).isEqualTo("Error while extracting JWT token");
        assertThat(exception.getCode()).isEqualTo(ErrorType.FORBIDDEN.getCode());
    }

    @Test
    void getUserIdFromJwtToken_ShouldThrowServiceException_WhenTokenIsEmpty() {
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("principal", "", null));
        SecurityContextHolder.setContext(securityContext);

        ServiceException exception = assertThrows(
                ServiceException.class,
                httpContextService::getUserIdFromJwtToken
        );

        assertThat(exception.getMessage()).isEqualTo("Error while extracting JWT token");
        assertThat(exception.getCode()).isEqualTo(ErrorType.FORBIDDEN.getCode());
    }

    @Test
    void getUserIdFromJwtToken_ShouldThrowServiceException_WhenJwtExtractionFails() {
        String jwt = "invalid.jwt.token";
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("principal", jwt, null));
        SecurityContextHolder.setContext(securityContext);

        when(jwtService.extractClaim(eq(jwt), any()))
                .thenThrow(new IllegalArgumentException("Invalid token format"));

        ServiceException exception = assertThrows(
                ServiceException.class,
                httpContextService::getUserIdFromJwtToken
        );

        assertThat(exception.getMessage()).isEqualTo("Error while extracting JWT token");
        assertThat(exception.getCode()).isEqualTo(ErrorType.FORBIDDEN.getCode());
        verify(jwtService).extractClaim(eq(jwt), any());
    }
}