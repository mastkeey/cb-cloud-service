package ru.mastkey.cloudservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mastkey.cloudservice.configuration.properties.JwtProperties;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.exception.ServiceException;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtProperties jwtProperties;

    private JwtService jwtService;

    private final String secretKey = "testSecretKeytestSecretKeytestSecretKeytestSecretKey";
    private final int ttlInMin = 100;

    @BeforeEach
    void setUp() {
        when(jwtProperties.getSecretKey()).thenReturn(secretKey);
        jwtService = new JwtService(jwtProperties);
    }

    @Test
    void extractUsername_ShouldReturnUsername() {
        String token = generateTestToken("testUser");

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("testUser");
    }

    @Test
    void extractClaim_ShouldReturnSpecifiedClaim() {
        String token = generateTestToken("testUser");

        Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void isTokenValid_ShouldReturnTrue_WhenTokenValid() {
        String token = generateTestToken("testUser");

        boolean isValid = jwtService.isTokenValid(token, "testUser");

        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenValid_ShouldReturnFalse_WhenTokenExpired() {
        String username = "testUser";
        String token = generateTestToken(username, new Date(System.currentTimeMillis() - 1000));

        assertThrows(JwtException.class, () -> jwtService.isTokenValid(token, username));
    }

    @Test
    void isTokenValid_ShouldReturnFalse_WhenUsernameMismatch() {
        String token = generateTestToken("testUser");

        boolean isValid = jwtService.isTokenValid(token, "anotherUser");

        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenExpired_ShouldReturnFalse_WhenTokenNotExpired() {
        String token = generateTestToken("testUser");

        boolean isExpired = jwtService.isTokenExpired(token);

        assertThat(isExpired).isFalse();
    }

    @Test
    void generateToken_ShouldReturnValidToken() {
        when(jwtProperties.getTtlInMin()).thenReturn(ttlInMin);

        var id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        user.setUsername("testUser");

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("testUser");
        assertThat(jwtService.extractClaim(token, claims -> claims.get("user_id", String.class)).toString()).isEqualTo(id.toString());
    }

    @Test
    void extractAllClaims_ShouldThrowException_WhenTokenInvalid() {
        String invalidToken = "invalidToken";

        assertThrows(Exception.class, () -> jwtService.extractClaim(invalidToken, Claims::getSubject));
    }

    private String generateTestToken(String username) {
        return generateTestToken(username, new Date(System.currentTimeMillis() + ttlInMin * 60000));
    }

    private String generateTestToken(String username, Date expiration) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }
}