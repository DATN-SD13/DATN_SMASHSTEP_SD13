package com.smashstep.datn.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        // Pad/trim to exactly 32 bytes for HS256
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        byte[] padded   = new byte[32];
        System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
        this.key           = Keys.hmacShaKeyFor(padded);
        this.expirationMs  = expirationMs;
    }

    /** Tạo access token cho cả nhân viên lẫn khách hàng. */
    public String generateToken(Long userId, String username, String role, String userType) {
        return Jwts.builder()
                .subject(username)
                .claim("userId",   userId)
                .claim("role",     role)
                .claim("userType", userType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    public String getUserType(String token) {
        return parseToken(token).get("userType", String.class);
    }

    public Long getUserId(String token) {
        return parseToken(token).get("userId", Long.class);
    }
}
