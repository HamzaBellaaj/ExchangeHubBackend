package com.exchangeHub.Backend.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignedUrlResponse {
    private UUID documentId;
    private String signedUrl;
    private Integer expiresIn;
}
