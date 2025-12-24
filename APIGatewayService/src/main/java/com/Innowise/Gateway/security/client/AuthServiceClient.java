package com.Innowise.Gateway.security.client;


import com.Innowise.Gateway.dto.ValidateTokenRequest;
import com.Innowise.Gateway.dto.ValidateTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class AuthServiceClient {

    private final WebClient client;

    public AuthServiceClient(WebClient.Builder builder,
                             @Value("${services.auth-url}") String authBaseUrl) {
        this.client = builder.baseUrl(authBaseUrl).build();
    }

    public Mono<ValidateTokenResponse> validateToken(String token) {
        return client.post()
                .uri("/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ValidateTokenRequest(token))
                .retrieve()
                .bodyToMono(ValidateTokenResponse.class);
    }
}
