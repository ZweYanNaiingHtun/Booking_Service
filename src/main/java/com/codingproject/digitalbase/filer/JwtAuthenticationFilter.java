package com.codingproject.digitalbase.filer;

import com.codingproject.digitalbase.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String jwt = authHeader.substring(7);
                System.out.println("JWT: " + jwt);
                if (!this.jwtService.isValidToken(jwt)) {
                    System.out.println("Invalid JWT");
                    filterChain.doFilter(request, response);
                    return;
                }

                if (!this.jwtService.isAccessToken(jwt)) {
                    System.out.println("Not access token");
                    filterChain.doFilter(request, response);
                    return;
                }

                String email = this.jwtService.extractEmailFromToken(jwt);
                System.out.println("Email: " + email);
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(email);
                    System.out.println("Authorities: " + String.valueOf(userDetails.getAuthorities()));
                    if (this.jwtService.validateTokenForUser(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, (Object)null, userDetails.getAuthorities());
                        authToken.setDetails((new WebAuthenticationDetailsSource()).buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        System.out.println("Authentication Success");
                    }
                }
            } catch (Exception e) {
                System.out.println("JWT FILTER ERROR: " + e.getMessage());
            }

            filterChain.doFilter(request, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}