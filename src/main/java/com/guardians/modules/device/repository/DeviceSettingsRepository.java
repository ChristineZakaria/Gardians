package com.guardians.modules.device.repository;

import com.guardians.modules.device.entity.DeviceSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceSettingsRepository extends JpaRepository<DeviceSettings, String> {
}
