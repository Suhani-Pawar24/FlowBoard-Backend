package com.flowboard.api_gateway.config;

import com.flowboard.api_gateway.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * OAuth2SuccessHandler — handles post-login for Google.
 *
 * Attribute differences:
 *   Google:  email="email"  name="name"     avatar="picture"
 *
 * Flow:
 *  1. Detect provider ("google") from OAuth2AuthenticationToken
 *  2. Extract email, fullName, avatarUrl using provider-aware attribute names
 *  3. POST /auth/sync to user-service → register or fetch user, get real userId
 *  4. Generate JWT with { sub=email, role, userId }
 *  5. Redirect frontend: /login?token=<jwt>&provider=<google>
 */
@Component
public class OAuth2SuccessHandler implements ServerAuthenticationSuccessHandler {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${user.service.url:http://localhost:8081}")
    private String userServiceUrl;

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

    private final WebClient webClient = WebClient.builder().build();

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange,
                                               Authentication authentication) {
        try {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oAuth2User = oauthToken.getPrincipal();

            // Detect which provider triggered this callback
            String provider = oauthToken.getAuthorizedClientRegistrationId(); // "google" or "github"

            // ── Attribute extraction (provider-aware) ──────────────────────────
            String email     = getAttr(oAuth2User, "email");
            String fullName  = getAttr(oAuth2User, "name");
            String avatarUrl;

            // Google uses "picture"
            avatarUrl = getAttr(oAuth2User, "picture");

            if (email == null || email.isBlank()) {
                email = oAuth2User.getName();
            }

            System.out.printf("[OAuth2][%s] Login success — email=%s name=%s%n",
                    provider.toUpperCase(), email, fullName);

            final String finalEmail     = email;
            final String finalFullName  = fullName  != null ? fullName  : "";
            final String finalAvatarUrl = avatarUrl != null ? avatarUrl : "";
            final String finalProvider  = provider;

            // ── Sync user with user-service (use UriBuilder to avoid double-encoding) ──
            return webClient.post()
                    .uri(userServiceUrl + "/auth/sync", uriBuilder -> {
                        uriBuilder.queryParam("email", finalEmail);
                        uriBuilder.queryParam("fullName", finalFullName);
                        if (!finalAvatarUrl.isBlank()) {
                            uriBuilder.queryParam("avatarUrl", finalAvatarUrl);
                        }
                        uriBuilder.queryParam("provider", finalProvider.toUpperCase());
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .bodyToMono(java.util.Map.class)
                    .flatMap(userMap -> {
                        Long   userId = extractLong(userMap, "userId");
                        String role   = (String) userMap.getOrDefault("role", "USER");
                        String token  = jwtUtil.generateToken(finalEmail, role, userId != null ? userId : 0L);
                        System.out.printf("[OAuth2][%s] Token issued — userId=%s role=%s%n",
                                finalProvider.toUpperCase(), userId, role);
                        return redirect(webFilterExchange,
                                frontendUrl + "/login?token=" + token + "&provider=" + finalProvider);
                    })
                    .onErrorResume(err -> {
                        System.err.printf("[OAuth2][%s] sync failed: %s — using provisional token%n",
                                finalProvider.toUpperCase(), err.getMessage());
                        String token = jwtUtil.generateToken(finalEmail, "USER", 0L);
                        return redirect(webFilterExchange,
                                frontendUrl + "/login?token=" + token + "&provider=" + finalProvider + "&sync=pending");
                    });

        } catch (Exception e) {
            System.err.println("[OAuth2] Handler error: " + e.getMessage());
            return redirect(webFilterExchange, frontendUrl + "/login?error=oauth_failed");
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String getAttr(OAuth2User user, String key) {
        Object val = user.getAttribute(key);
        return val != null ? val.toString() : null;
    }

    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    private Long extractLong(java.util.Map map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        if (val instanceof String) {
            try { return Long.parseLong((String) val); } catch (Exception ignored) {}
        }
        return null;
    }

    private Mono<Void> redirect(WebFilterExchange wfe, String url) {
        wfe.getExchange().getResponse().setStatusCode(org.springframework.http.HttpStatus.FOUND);
        wfe.getExchange().getResponse().getHeaders().setLocation(URI.create(url));
        return wfe.getExchange().getResponse().setComplete();
    }
}
