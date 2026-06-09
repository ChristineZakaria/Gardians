package com.guardians.modules.device.repository;

import com.guardians.shared.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByDeviceId(String deviceId);

    boolean existsByDeviceId(String deviceId);

    @Query("SELECT d FROM Device d WHERE d.owner.id = :ownerId")
    List<Device> findByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT d FROM Device d WHERE d.linkedParent.id = :parentId")
    List<Device> findByLinkedParentId(@Param("parentId") Long parentId);

    @Query("SELECT d FROM Device d WHERE d.linkedParent.id = :parentId AND d.active = true")
    List<Device> findActiveChildDevicesByParentId(@Param("parentId") Long parentId);

    @Query("SELECT d FROM Device d WHERE d.linkedParent IS NOT NULL ORDER BY d.lastSeen DESC")
    List<Device> findMostRecentlyActiveChildDevices(org.springframework.data.domain.Pageable pageable);
}
