package com.Innowise.Gateway.security;


import com.Innowise.Gateway.dto.ValidateTokenResponse;
import com.Innowise.Gateway.security.client.AuthServiceClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthGlobalFilter implements GlobalFilter {

    private final AuthServiceClient authServiceClient;

    public JwtAuthGlobalFilter(AuthServiceClient authServiceClient) {
        this.authServiceClient = authServiceClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String token = extractBearer(exchange);
        if (token == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return authServiceClient.validateToken(token)
                .flatMap(resp -> {
                    if (!Boolean.TRUE.equals(resp.valid())) {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(withUserContext(exchange, resp));
                })
                .onErrorResume(ex -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                });
    }

    private boolean isPublicPath(String path) {
        return "/auth/login".equals(path) || "/auth/register".equals(path);
    }

    private String extractBearer(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;

        String token = authHeader.substring("Bearer ".length()).trim();
        return token.isEmpty() ? null : token;
    }

    private ServerWebExchange withUserContext(ServerWebExchange exchange, ValidateTokenResponse resp) {
        return exchange.mutate()
                .request(r -> r.headers(h -> {
                    if (resp.userId() != null) h.set("X-User-Id", String.valueOf(resp.userId()));
                    if (resp.username() != null) h.set("X-Username", resp.username());
                    if (resp.roles() != null && !resp.roles().isEmpty()) h.set("X-Roles", String.join(",", resp.roles()));
                }))
                .build();
    }
}
