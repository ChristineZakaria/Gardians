package com.guardians.modules.apps.repository;

import com.guardians.modules.apps.entity.InstalledApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstalledAppRepository extends JpaRepository<InstalledApp, Long> {

    List<InstalledApp> findByDeviceId(String deviceId);

    Optional<InstalledApp> findByDeviceIdAndPackageName(String deviceId, String packageName);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM InstalledApp a WHERE a.deviceId = :deviceId")
    void deleteByDeviceId(@Param("deviceId") String deviceId);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO installed_apps (device_id, package_name, app_name, updated_at)
            VALUES (:deviceId, :packageName, :appName, NOW())
            ON CONFLICT (device_id, package_name)
            DO UPDATE SET app_name = EXCLUDED.app_name, updated_at = EXCLUDED.updated_at
            """, nativeQuery = true)
    void upsert(@Param("deviceId") String deviceId,
                @Param("packageName") String packageName,
                @Param("appName") String appName);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM InstalledApp a WHERE a.deviceId = :deviceId AND a.packageName NOT IN :packages")
    void deleteByDeviceIdAndPackageNameNotIn(@Param("deviceId") String deviceId,
                                             @Param("packages") List<String> packages);
}
