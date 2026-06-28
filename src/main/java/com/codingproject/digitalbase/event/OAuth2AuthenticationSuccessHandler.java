//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.event;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.Generated;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User)authentication.getPrincipal();
        String email = (String)oAuth2User.getAttribute("email");
        String name = (String)oAuth2User.getAttribute("name");
        String accessToken = "mock_access_token_abc123";
        String refreshToken = "mock_refresh_token_xyz789";
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth2/redirect").queryParam("accessToken", new Object[]{accessToken}).queryParam("refreshToken", new Object[]{refreshToken}).build().toUriString();
        this.getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    @Generated
    public OAuth2AuthenticationSuccessHandler() {
    }
}
