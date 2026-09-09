package com.example.demo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "dGVzdFNlY3JldEtleTEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Test
    void generateTokenAndValidate() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");
        
        String token = Jwts.builder()
                .claims(claims)
                .subject("testuser")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
        
        assertNotNull(token);
        
        Claims extractedClaims = Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        assertEquals("testuser", extractedClaims.getSubject());
        assertEquals("USER", extractedClaims.get("role"));
    }

    @Test
    void expiredTokenThrowsException() {
        String token = Jwts.builder()
                .subject("testuser")
                .issuedAt(new Date(System.currentTimeMillis() - 86400000))
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
        
        assertThrows(ExpiredJwtException.class, () -> {
            Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token);
        });
    }
}