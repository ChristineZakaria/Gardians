package com.guardians.modules.verification.controller;

import com.guardians.modules.verification.dto.VerificationRequest;
import com.guardians.modules.verification.dto.VerificationResponse;
import com.guardians.modules.verification.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
@Tag(name = "Verification", description = "Parent identity verification")
@SecurityRequirement(name = "bearerAuth")
public class VerificationController {

    private final VerificationService verificationService;

    @PostMapping("/submit")
    @Operation(summary = "Submit verification documents (ID card, selfie, birth certificate)")
    public ResponseEntity<VerificationResponse> submit(
            @Valid @RequestBody VerificationRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(verificationService.submit(req, user.getUsername()));
    }

    @GetMapping("/my-status")
    @Operation(summary = "Get current verification status for the logged-in parent")
    public ResponseEntity<VerificationResponse> myStatus(
            @AuthenticationPrincipal UserDetails user) {
        VerificationResponse status = verificationService.getStatus(user.getUsername());
        if (status == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(status);
    }
}
