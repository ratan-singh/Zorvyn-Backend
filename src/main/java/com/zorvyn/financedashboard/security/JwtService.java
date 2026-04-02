package com.zorvyn.financedashboard.security;

import com.zorvyn.financedashboard.model.User;
import com.zorvyn.financedashboard.model.enums.Role;
import io.jsonwebtoken.Claims;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long tokenExpirationMs;

    public JwtService(
            @Value("${jwt.secret-key}") String secretKeyBase64,
            @Value("${jwt.expiration-ms}") long tokenExpirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKeyBase64));
        this.tokenExpirationMs = tokenExpirationMs;
    }

    public String generateToken(User user) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + tokenExpirationMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("role", user.getRole().name())
                .claim("name", user.getName())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractClaim(token, claims -> claims.get("userId", String.class)));
    }

    public Role extractRole(String token) {
        return Role.valueOf(extractClaim(token, claims -> claims.get("role", String.class)));
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String emailFromToken = extractEmail(token);
        return emailFromToken.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public long getTokenExpirationMs() {
        return tokenExpirationMs;
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Throws ExpiredJwtException, MalformedJwtException, or SignatureException
     * on invalid tokens — callers must handle these appropriately.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
