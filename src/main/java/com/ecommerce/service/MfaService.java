package com.ecommerce.service;

import com.ecommerce.dto.AuthResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MfaService {
    private static final long CODE_VALIDITY_SECONDS = 120;
    private static final long RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 5;

    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, Challenge> challenges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> lastSentByEmail = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public MfaService(PasswordEncoder passwordEncoder, EmailService emailService) {
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public String createChallenge(String email, String userName, AuthResponse authenticatedResponse) {
        String normalizedEmail = email.trim().toLowerCase();
        Instant now = Instant.now();
        Instant lastSent = lastSentByEmail.get(normalizedEmail);
        if (lastSent != null && lastSent.plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
            throw new IllegalStateException("Please wait before requesting another verification code.");
        }

        removeChallengesForEmail(normalizedEmail);
        String code = String.format("%05d", secureRandom.nextInt(100000));
        String challengeId = UUID.randomUUID().toString();
        Challenge challenge = new Challenge(
                normalizedEmail,
                passwordEncoder.encode(code),
                now.plusSeconds(CODE_VALIDITY_SECONDS),
                authenticatedResponse);

        challenges.put(challengeId, challenge);
        try {
            emailService.sendMfaCodeEmail(normalizedEmail, userName, code);
            lastSentByEmail.put(normalizedEmail, now);
        } catch (RuntimeException ex) {
            challenges.remove(challengeId);
            throw ex;
        }
        return challengeId;
    }

    public AuthResponse verify(String challengeId, String code) {
        Challenge challenge = challenges.get(challengeId);
        if (challenge == null) {
            throw new BadCredentialsException("Invalid or expired verification code.");
        }

        synchronized (challenge) {
            if (Instant.now().isAfter(challenge.expiresAt)) {
                // Keep the expired challenge metadata so Re-send Code can create
                // a fresh code for the same authenticated login session.
                throw new BadCredentialsException("Invalid or expired verification code.");
            }
            if (challenge.attempts >= MAX_ATTEMPTS) {
                challenges.remove(challengeId);
                throw new BadCredentialsException("Invalid or expired verification code.");
            }
            challenge.attempts++;
            if (!passwordEncoder.matches(code, challenge.codeHash)) {
                if (challenge.attempts >= MAX_ATTEMPTS) challenges.remove(challengeId);
                throw new BadCredentialsException("Invalid or expired verification code.");
            }
            challenges.remove(challengeId);
            lastSentByEmail.remove(challenge.email);
            return challenge.authResponse;
        }
    }

    public String resend(String existingChallengeId) {
        Challenge existing = challenges.get(existingChallengeId);
        if (existing == null) {
            throw new BadCredentialsException("The verification session has expired. Please return to login and try again.");
        }

        Instant now = Instant.now();
        Instant lastSent = lastSentByEmail.get(existing.email);
        if (lastSent != null && lastSent.plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
            throw new IllegalStateException("Please wait before requesting another verification code.");
        }

        challenges.remove(existingChallengeId);
        String code = String.format("%05d", secureRandom.nextInt(100000));
        String newChallengeId = UUID.randomUUID().toString();
        Challenge replacement = new Challenge(
                existing.email,
                passwordEncoder.encode(code),
                now.plusSeconds(CODE_VALIDITY_SECONDS),
                existing.authResponse);
        challenges.put(newChallengeId, replacement);

        try {
            emailService.sendMfaCodeEmail(existing.email, existing.email, code);
            lastSentByEmail.put(existing.email, now);
            return newChallengeId;
        } catch (RuntimeException ex) {
            challenges.remove(newChallengeId);
            challenges.put(existingChallengeId, existing);
            throw ex;
        }
    }

    private void removeChallengesForEmail(String email) {
        challenges.entrySet().removeIf(entry -> entry.getValue().email.equals(email));
    }

    private static final class Challenge {
        private final String email;
        private final String codeHash;
        private final Instant expiresAt;
        private final AuthResponse authResponse;
        private int attempts;

        private Challenge(String email, String codeHash, Instant expiresAt, AuthResponse authResponse) {
            this.email = email;
            this.codeHash = codeHash;
            this.expiresAt = expiresAt;
            this.authResponse = authResponse;
        }
    }
}
