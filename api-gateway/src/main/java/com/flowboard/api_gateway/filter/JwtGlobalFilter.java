package com.flowboard.api_gateway.filter;

import com.flowboard.api_gateway.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Skip validation for OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethod().name())) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getPath().value();

        // Public/internal endpoints — skip JWT validation
        if (path.contains("/auth/login")    || path.contains("/auth/register") ||
            path.contains("/auth/logout")   || path.contains("/auth/sync")     ||
            path.contains("/oauth2/")       || path.contains("/login/oauth2/") ||
            path.contains("/v3/api-docs")   || path.contains("/swagger-ui") ||
            path.contains("/ws/")           || path.endsWith("/ws")            ||
            path.equals("/cards/overdue")   ||  // internal call from user-service
            (path.equals("/notifications") && exchange.getRequest().getMethod().name().equalsIgnoreCase("POST")) ||
            (path.equals("/notifications/bulk") && exchange.getRequest().getMethod().name().equalsIgnoreCase("POST"))) {
            return chain.filter(exchange);
        }


        // 2. Check for Authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("MISSING AUTH HEADER FOR PATH: " + path);
            System.out.println("HEADERS: " + exchange.getRequest().getHeaders());
            return onError(exchange, "Authorization header is missing or invalid", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        // 3. Validate Token
        if (jwtUtil.validateToken(token)) {
            // ... (rest of validation logic)
            String email = jwtUtil.extractEmail(token);
            String role = jwtUtil.extractRole(token);
            Long userId = jwtUtil.extractUserId(token);

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-user-email", email)
                    .header("X-user-role", role != null ? role : "USER")
                    .header("X-user-id", userId != null ? userId.toString() : "0")
                    .build();

            exchange.getResponse().getHeaders().add("X-Debug-ID", userId != null ? userId.toString() : "MISSING");
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        // 6. Return 401 for invalid tokens
        return onError(exchange, "JWT Token is invalid or expired", HttpStatus.UNAUTHORIZED);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        String jsonError = String.format("{\"error\": \"%s\", \"message\": \"%s\"}", status.getReasonPhrase(), err);
        byte[] bytes = jsonError.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        org.springframework.core.io.buffer.DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        exchange.getResponse().getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1; // Run with high priority
    }
}
