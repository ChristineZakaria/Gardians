package com.guardians.modules.apps.repository;

import com.guardians.modules.apps.entity.AppUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUsageRepository extends JpaRepository<AppUsage, Long> {

    List<AppUsage> findByDeviceId(String deviceId);

    Optional<AppUsage> findByDeviceIdAndPackageName(String deviceId, String packageName);

    @Query("SELECT COALESCE(SUM(a.usageMillis), 0) FROM AppUsage a WHERE a.deviceId = :deviceId")
    long sumUsageMillisByDeviceId(@Param("deviceId") String deviceId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM AppUsage a WHERE a.deviceId = :deviceId")
    void deleteByDeviceId(@Param("deviceId") String deviceId);
}
