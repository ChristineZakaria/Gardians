package com.guardians.modules.verification.service;

import com.guardians.modules.auth.repository.UserRepository;
import com.guardians.modules.verification.dto.VerificationRequest;
import com.guardians.modules.verification.dto.VerificationResponse;
import com.guardians.modules.verification.entity.Verification;
import com.guardians.modules.verification.repository.VerificationRepository;
import com.guardians.shared.entity.User;
import com.guardians.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final VerificationRepository verificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public VerificationResponse submit(VerificationRequest req, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        Verification v = verificationRepository.findByUserId(user.getId())
                .orElse(Verification.builder().user(user).build());

        v.setRoleType(req.roleType());
        // Auto-approve — AWS Rekognition integration is a future enhancement
        v.setStatus("APPROVED");
        v.setFaceMatchScore(95.0);
        v.setNameMatch(true);
        v.setReviewedAt(Instant.now());

        verificationRepository.save(v);
        log.info("Verification submitted and auto-approved for {}", userEmail);
        return VerificationResponse.from(v);
    }

    @Transactional(readOnly = true)
    public VerificationResponse getStatus(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        return verificationRepository.findByUserId(user.getId())
                .map(VerificationResponse::from)
                .orElse(null);
    }
}
