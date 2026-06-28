//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.TokenPair;
import com.codingproject.digitalbase.enums.TokenType;
import com.codingproject.digitalbase.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtServiceImpl implements JwtService {
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    @Value("${app.jwt.expiration}")
    private long jwtExpirationMs;
    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpirationMs;

    public JwtServiceImpl() {
    }

    public TokenPair generateTokenPair(Authentication authentication) {
        String accessToken = this.generateAccessToken(authentication);
        String refreshToken = this.generateRefreshToken(authentication);
        return new TokenPair(accessToken, refreshToken);
    }

    public String generateAccessToken(Authentication authentication) {
        Map<String, String> claims = new HashMap();
        claims.put("tokenType", TokenType.ACCESS.name());
        return this.generateToken(authentication, this.jwtExpirationMs, claims);
    }

    public String generateRefreshToken(Authentication authentication) {
        Map<String, String> claims = new HashMap();
        claims.put("tokenType", TokenType.REFRESH.name());
        return this.generateToken(authentication, this.refreshExpirationMs, claims);
    }

    public String generateToken(Authentication authentication, long expirationInMs, Map<String, String> claims) {
        User userPrincipal = (User)authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationInMs);
        claims.put("roles", (String)userPrincipal.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(",")));
        return ((JwtBuilder)((JwtBuilder.BuilderHeader)Jwts.builder().header().add("typ", "JWT")).and()).subject(userPrincipal.getEmail()).claims(claims).issuedAt(now).expiration(expiryDate).signWith(this.getSignInKey()).compact();
    }

    public boolean validateTokenForUser(String token, UserDetails userDetails) {
        String email = this.extractEmailFromToken(token);
        User user = (User)userDetails;
        return email != null && email.equals(user.getEmail());
    }

    public boolean isValidToken(String token) {
        if (token != null && !token.isEmpty()) {
            return this.extractAllClaims(token) != null;
        } else {
            return false;
        }
    }

    public String extractEmailFromToken(String token) {
        Claims claims = this.extractAllClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    public boolean isRefreshToken(String token) {
        Claims claims = this.extractAllClaims(token);
        return claims == null ? false : TokenType.REFRESH.name().equals(String.valueOf(claims.get("tokenType")));
    }

    public boolean isAccessToken(String token) {
        Claims claims = this.extractAllClaims(token);
        return claims == null ? false : TokenType.ACCESS.name().equals(String.valueOf(claims.get("tokenType")));
    }

    private Claims extractAllClaims(String token) {
        Claims claims = null;

        try {
            claims = (Claims)Jwts.parser().verifyWith(this.getSignInKey()).build().parseSignedClaims(token).getPayload();
            return claims;
        } catch (IllegalArgumentException | JwtException e) {
            System.err.println("Invalid JWT token: " + ((RuntimeException)e).getMessage());
            return null;
        }
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = (byte[])Decoders.BASE64.decode(this.jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
