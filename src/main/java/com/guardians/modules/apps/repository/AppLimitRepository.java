package com.guardians.modules.apps.repository;

import com.guardians.modules.apps.entity.AppLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppLimitRepository extends JpaRepository<AppLimit, Long> {
    List<AppLimit> findByDeviceId(String deviceId);
    Optional<AppLimit> findByDeviceIdAndPackageName(String deviceId, String packageName);
    void deleteByDeviceIdAndPackageName(String deviceId, String packageName);
}
