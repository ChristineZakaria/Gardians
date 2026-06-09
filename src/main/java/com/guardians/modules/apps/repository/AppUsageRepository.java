package com.guardians.modules.apps.repository;

import com.guardians.modules.apps.entity.AppUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppUsageRepository extends JpaRepository<AppUsage, Long> {

    List<AppUsage> findByDeviceId(String deviceId);

    List<AppUsage> findByDeviceIdAndDate(String deviceId, LocalDate date);

    Optional<AppUsage> findByDeviceIdAndPackageName(String deviceId, String packageName);

    Optional<AppUsage> findByDeviceIdAndPackageNameAndDate(String deviceId, String packageName, LocalDate date);

    @Query("SELECT COALESCE(SUM(a.usageMillis), 0) FROM AppUsage a WHERE a.deviceId = :deviceId")
    long sumUsageMillisByDeviceId(@Param("deviceId") String deviceId);

    @Query("SELECT COALESCE(SUM(a.usageMillis), 0) FROM AppUsage a WHERE a.deviceId = :deviceId AND a.date = :date")
    long sumUsageMillisByDeviceIdAndDate(@Param("deviceId") String deviceId, @Param("date") LocalDate date);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM AppUsage a WHERE a.deviceId = :deviceId")
    void deleteByDeviceId(@Param("deviceId") String deviceId);
}
