package com.ecommerce.dto;

public class MfaChallengeResponse {
    private final String challengeId;
    private final String message;

    public MfaChallengeResponse(String challengeId, String message) {
        this.challengeId = challengeId;
        this.message = message;
    }

    public String getChallengeId() { return challengeId; }
    public String getMessage() { return message; }
}
