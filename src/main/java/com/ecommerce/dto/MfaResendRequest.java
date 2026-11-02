package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;

public class MfaResendRequest {
    @NotBlank
    private String challengeId;

    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
}
