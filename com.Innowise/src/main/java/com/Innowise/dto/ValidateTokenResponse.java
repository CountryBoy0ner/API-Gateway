package com.Innowise.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ValidateTokenResponse {

    private boolean valid;
    private String username;
    private Long userId;
    private List<String> roles;
}