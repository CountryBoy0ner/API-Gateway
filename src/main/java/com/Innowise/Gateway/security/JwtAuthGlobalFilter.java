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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@Component
public class JwtAuthGlobalFilter implements GlobalFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthGlobalFilter.class);

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

                    log.info("Token validation response: userId={}, username={}, roles={}",
                            resp.userId(),
                            resp.username(),
                            resp.roles());

                    if (!Boolean.TRUE.equals(resp.valid())) {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }

                    return chain.filter(withUserContext(exchange, resp));
                });
    }

    private boolean isPublicPath(String path) {
        return "/auth/login".equals(path) || "/auth/register".equals(path);
    }

    private String extractBearer(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        return authHeader.substring("Bearer ".length()).trim();
    }

    private ServerWebExchange withUserContext(ServerWebExchange exchange, ValidateTokenResponse resp) {

        return exchange.mutate()
                .request(r -> r.headers(h -> {
                    if (resp.userId() != null)
                        h.set("X-User-Id", String.valueOf(resp.userId()));

                    if (resp.username() != null)
                        h.set("X-Username", resp.username());

                    if (resp.roles() != null && !resp.roles().isEmpty())
                        h.set("X-Roles", String.join(",", resp.roles()));
                }))
                .build();
    }
}