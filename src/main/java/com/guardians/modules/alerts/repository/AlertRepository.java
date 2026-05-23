package com.guardians.modules.alerts.repository;

import com.guardians.shared.entity.AlertEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<AlertEntity, Long> {

    Page<AlertEntity> findByParentIdOrderByCreatedAtDesc(Long parentId, Pageable pageable);

    Page<AlertEntity> findByParentIdAndReadFalseOrderByCreatedAtDesc(Long parentId, Pageable pageable);

    long countByParentIdAndReadFalse(Long parentId);

    List<AlertEntity> findBySenderDeviceDeviceIdOrderByCreatedAtDesc(String deviceId);

    @Query("SELECT a FROM AlertEntity a WHERE a.senderDevice.deviceId = :deviceId AND a.createdAt >= :startOfDay ORDER BY a.createdAt DESC")
    List<AlertEntity> findTodayByDeviceId(@Param("deviceId") String deviceId, @Param("startOfDay") Instant startOfDay);

    @Modifying
    @Transactional
    @Query("UPDATE AlertEntity a SET a.read = true WHERE a.parent.id = :parentId AND a.read = false")
    int markAllReadForParent(Long parentId);
}
