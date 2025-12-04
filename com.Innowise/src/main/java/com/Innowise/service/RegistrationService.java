package com.Innowise.service;

import com.Innowise.dto.AuthRegisterRequest;
import com.Innowise.dto.RegistrationRequest;
import com.Innowise.dto.UserDto;
import com.Innowise.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final WebClient userServiceWebClient;
    private final WebClient authServiceWebClient;

    public Mono<UserDto> register(RegistrationRequest request) {
        // Готовим DTO для User-service
        UserDto userRequest = new UserDto();
        userRequest.setName(request.getFirstName());
        userRequest.setSurname(request.getLastName());
        userRequest.setEmail(request.getEmail());

        // 1) создаём юзера в user-service
        return userServiceWebClient.post()
                .uri("/api/users")
                .bodyValue(userRequest)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("User-service error: " + body)))
                )
                .bodyToMono(UserDto.class)
                .flatMap(createdUser -> {
                    // 2) создаём учётку в auth-service
                    AuthRegisterRequest authReq = new AuthRegisterRequest();
                    authReq.setUsername(request.getUsername());
                    authReq.setPassword(request.getPassword());

                    return authServiceWebClient.post()
                            .uri("/auth/register")
                            .bodyValue(authReq)
                            .retrieve()
                            .onStatus(HttpStatusCode::isError, resp ->
                                    resp.bodyToMono(String.class)
                                            .flatMap(body -> Mono.error(new RuntimeException("Auth-service error: " + body)))
                            )
                            .bodyToMono(UserResponse.class)
                            // если всё ок, возвращаем createdUser
                            .thenReturn(createdUser)
                            // если auth-service упал — делаем роллбек в user-service
                            .onErrorResume(e ->
                                    userServiceWebClient.delete()
                                            .uri("/api/users/{id}", createdUser.getId())
                                            .retrieve()
                                            .bodyToMono(Void.class)
                                            .onErrorResume(ex -> Mono.empty()) // не получилось удалить – просто логируй
                                            .then(Mono.error(e))
                            );
                });
    }
}
