package com.guardians.modules.location.repository;

import com.guardians.modules.location.entity.LocationEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<LocationEntry, String> {
    List<LocationEntry> findByLinkedParentId(Long parentId);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO locations (device_id, device_name, latitude, longitude, linked_parent_id, updated_at)
        VALUES (:deviceId, :deviceName, :latitude, :longitude, :linkedParentId, :updatedAt)
        ON CONFLICT (device_id) DO UPDATE SET
            device_name      = EXCLUDED.device_name,
            latitude         = EXCLUDED.latitude,
            longitude        = EXCLUDED.longitude,
            linked_parent_id = EXCLUDED.linked_parent_id,
            updated_at       = EXCLUDED.updated_at
        """, nativeQuery = true)
    void upsert(
        @Param("deviceId")       String deviceId,
        @Param("deviceName")     String deviceName,
        @Param("latitude")       double latitude,
        @Param("longitude")      double longitude,
        @Param("linkedParentId") Long linkedParentId,
        @Param("updatedAt")      Instant updatedAt
    );
}
