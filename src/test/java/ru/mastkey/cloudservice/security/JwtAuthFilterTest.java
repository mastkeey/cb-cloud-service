package ru.mastkey.cloudservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.io.PrintWriter;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldProceed_WhenNoAuthHeader() throws ServletException, IOException {
        when(request.getHeader(JwtAuthFilter.HEADER_NAME)).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldProceed_WhenInvalidAuthHeader() throws ServletException, IOException {
        when(request.getHeader(JwtAuthFilter.HEADER_NAME)).thenReturn("InvalidHeader");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldSetAuthentication_WhenValidJwt() throws ServletException, IOException {
        String validJwt = "valid.jwt.token";
        String username = "testUser";

        when(request.getHeader(JwtAuthFilter.HEADER_NAME)).thenReturn("Bearer " + validJwt);
        when(jwtService.extractUsername(validJwt)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtService.isTokenValid(validJwt, username)).thenReturn(true);
        when(userDetails.getAuthorities()).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        SecurityContext context = SecurityContextHolder.getContext();
        assertThat(context.getAuthentication()).isNotNull();
        assertThat(context.getAuthentication().getPrincipal()).isEqualTo(userDetails);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldProceed_WhenJwtInvalid() throws ServletException, IOException {
        String invalidJwt = "invalid.jwt.token";
        String username = "testUser";

        when(request.getHeader(JwtAuthFilter.HEADER_NAME)).thenReturn("Bearer " + invalidJwt);
        when(jwtService.extractUsername(invalidJwt)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtService.isTokenValid(invalidJwt, username)).thenReturn(false);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldProceed_WhenUsernameNotExtracted() throws ServletException, IOException {
        String jwt = "some.jwt.token";

        when(request.getHeader(JwtAuthFilter.HEADER_NAME)).thenReturn("Bearer " + jwt);
        when(jwtService.extractUsername(jwt)).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldProceed_WhenAuthenticationAlreadySet() throws ServletException, IOException {
        String jwt = "test.jwt.token";
        String username = "testUser";

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("existingUser", null, null));
        SecurityContextHolder.setContext(securityContext);

        when(request.getHeader(JwtAuthFilter.HEADER_NAME)).thenReturn("Bearer " + jwt);
        when(jwtService.extractUsername(jwt)).thenReturn(username);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("existingUser");

        verify(filterChain).doFilter(request, response);

        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtService, never()).isTokenValid(anyString(), anyString());
    }

    @Test
    void shouldReturnUnauthorized_WhenJwtExceptionThrown() throws ServletException, IOException {
        String jwt = "invalid.jwt.token";

        PrintWriter mockWriter = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(mockWriter);
        when(request.getHeader(JwtAuthFilter.HEADER_NAME)).thenReturn("Bearer " + jwt);
        doThrow(new io.jsonwebtoken.JwtException("Invalid JWT")).when(jwtService).extractUsername(jwt);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response,times(2)).getWriter();
        verify(filterChain, never()).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}