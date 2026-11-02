package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class MfaVerifyRequest {
    @NotBlank
    private String challengeId;

    @NotBlank
    @Pattern(regexp = "^\\d{5}$", message = "MFA code must contain exactly 5 digits")
    private String code;

    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
