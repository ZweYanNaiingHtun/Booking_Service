//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.TokenPair;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {
    TokenPair generateTokenPair(Authentication authentication);

    String generateAccessToken(Authentication authentication);

    String generateRefreshToken(Authentication authentication);

    String generateToken(Authentication authentication, long expirationInMs, Map<String, String> claims);

    boolean validateTokenForUser(String token, UserDetails userDetails);

    boolean isValidToken(String token);

    String extractEmailFromToken(String token);

    boolean isRefreshToken(String token);

    boolean isAccessToken(String token);
}
