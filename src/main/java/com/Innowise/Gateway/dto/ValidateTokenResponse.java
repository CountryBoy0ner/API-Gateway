package com.Innowise.Gateway.dto;

import java.util.List;

public record ValidateTokenResponse(
        Boolean valid,
        String username,
        Long userId,
        List<String> roles
) {
}

