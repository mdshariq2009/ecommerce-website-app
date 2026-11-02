package com.ecommerce.controller;

import com.ecommerce.dto.AuthResponse;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.MfaChallengeResponse;
import com.ecommerce.dto.MfaVerifyRequest;
import com.ecommerce.dto.MfaResendRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.service.UserService;
import com.ecommerce.service.MfaService;
import com.ecommerce.service.LoginAttemptService;
import com.ecommerce.exception.TooManyLoginAttemptsException;
import org.springframework.security.core.AuthenticationException;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    private final UserService userService;
    private final MfaService mfaService;
    private final LoginAttemptService loginAttemptService;
    
    @Autowired
    public AuthController(UserService userService, MfaService mfaService, LoginAttemptService loginAttemptService) {
        this.userService = userService;
        this.mfaService = mfaService;
        this.loginAttemptService = loginAttemptService;
    }
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/mfa/request")
    public ResponseEntity<MfaChallengeResponse> requestMfa(@Valid @RequestBody LoginRequest request) {
        long remaining = loginAttemptService.getRemainingLockSeconds(request.getEmail());
        if (remaining > 0) throw new TooManyLoginAttemptsException(remaining);

        try {
            AuthResponse authenticatedResponse = userService.login(request);
            loginAttemptService.recordSuccess(request.getEmail());
            String challengeId = mfaService.createChallenge(
                    request.getEmail(),
                    request.getEmail(),
                    authenticatedResponse);
            return ResponseEntity.ok(new MfaChallengeResponse(
                    challengeId,
                    "A 5-digit verification code was sent to your registered email."));
        } catch (AuthenticationException ex) {
            long lockSeconds = loginAttemptService.recordFailure(request.getEmail());
            if (lockSeconds > 0) throw new TooManyLoginAttemptsException(lockSeconds);
            throw ex;
        }
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<?> verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        try {
            return ResponseEntity.ok(mfaService.verify(request.getChallengeId(), request.getCode()));
        } catch (org.springframework.security.authentication.BadCredentialsException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "The MFA code is incorrect or expired. Please check the code and try again."));
        }
    }

    @PostMapping("/mfa/resend")
    public ResponseEntity<?> resendMfa(@Valid @RequestBody MfaResendRequest request) {
        try {
            String newChallengeId = mfaService.resend(request.getChallengeId());
            return ResponseEntity.ok(new MfaChallengeResponse(
                    newChallengeId,
                    "A new 5-digit verification code was sent to your registered email."));
        } catch (org.springframework.security.authentication.BadCredentialsException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", ex.getMessage()));
        }
    }
    

    @ExceptionHandler(TooManyLoginAttemptsException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyAttempts(TooManyLoginAttemptsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(Map.of(
                        "message", ex.getMessage(),
                        "retryAfterSeconds", ex.getRetryAfterSeconds()));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is running");
    }
}
