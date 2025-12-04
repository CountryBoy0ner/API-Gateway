package com.Innowise.controller;

import com.Innowise.dto.*;
import com.Innowise.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthGatewayController {

    private final WebClient authServiceWebClient;
    private final RegistrationService registrationService;

    @PostMapping("/register")
    public Mono<ResponseEntity<UserDto>> register(
            @Valid @RequestBody RegistrationRequest request) {
        return registrationService.register(request)
                .map(user -> ResponseEntity.status(HttpStatus.CREATED).body(user));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return authServiceWebClient.post()
                .uri("/auth/login")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AuthResponse.class)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authServiceWebClient.post()
                .uri("/auth/refresh")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AuthResponse.class)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/validate")
    public Mono<ResponseEntity<ValidateTokenResponse>> validate(@RequestBody ValidateTokenRequest request) {
        return authServiceWebClient.post()
                .uri("/auth/validate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ValidateTokenResponse.class)
                .map(ResponseEntity::ok);
    }
}
