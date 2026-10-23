package com.minicloud.security;

import com.minicloud.auth.entity.Role;
import com.minicloud.config.MiniCloudProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final MiniCloudProperties properties;

    public JwtService(MiniCloudProperties properties) {
        this.properties = properties;
    }

    public String createToken(String username, Role role) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(properties.getSecurity().getJwtExpirationMs());
        return Jwts.builder()
                .subject(username)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long expirationMs() {
        return properties.getSecurity().getJwtExpirationMs();
    }

    private SecretKey signingKey() {
        byte[] bytes = properties.getSecurity().getJwtSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(padSecret(bytes));
    }

    private byte[] padSecret(byte[] bytes) {
        if (bytes.length >= 32) {
            return bytes;
        }
        byte[] padded = new byte[32];
        System.arraycopy(bytes, 0, padded, 0, bytes.length);
        return padded;
    }
}
