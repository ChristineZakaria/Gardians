package com.guardians.modules.location.repository;

import com.guardians.modules.location.entity.Geofence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GeofenceRepository extends JpaRepository<Geofence, Long> {
    Optional<Geofence> findByDeviceId(String deviceId);
    void deleteByDeviceId(String deviceId);
}
