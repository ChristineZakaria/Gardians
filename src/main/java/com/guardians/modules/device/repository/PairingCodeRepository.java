package com.guardians.modules.device.repository;

import com.guardians.shared.entity.PairingCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PairingCodeRepository extends JpaRepository<PairingCode, Long> {

    Optional<PairingCode> findByCodeAndUsedFalse(String code);

    @Modifying
    @Transactional
    @Query("DELETE FROM PairingCode p WHERE p.expiresAt < :now AND p.used = false")
    void deleteExpiredCodes(Instant now);

    @Query("SELECT COUNT(p) > 0 FROM PairingCode p WHERE p.parent.id = :parentId AND p.used = false AND p.expiresAt > :now")
    boolean hasActivePairingCode(Long parentId, Instant now);
}
