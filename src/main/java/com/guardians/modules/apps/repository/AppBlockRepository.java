package com.guardians.modules.apps.repository;

import com.guardians.modules.apps.entity.AppBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppBlockRepository extends JpaRepository<AppBlock, Long> {

    List<AppBlock> findByDeviceId(String deviceId);

    Optional<AppBlock> findByDeviceIdAndPackageName(String deviceId, String packageName);

    boolean existsByDeviceIdAndPackageName(String deviceId, String packageName);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM AppBlock b WHERE b.deviceId = :deviceId AND b.packageName = :packageName")
    void deleteByDeviceIdAndPackageName(@Param("deviceId") String deviceId,
                                        @Param("packageName") String packageName);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM AppBlock b WHERE b.deviceId = :deviceId")
    void deleteAllByDeviceId(@Param("deviceId") String deviceId);
}
