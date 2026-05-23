package com.guardians.modules.comms.repository;

import com.guardians.modules.comms.entity.CallLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CallLogRepository extends JpaRepository<CallLogEntry, Long> {

    List<CallLogEntry> findByDeviceIdOrderByTimestampDesc(String deviceId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM CallLogEntry c WHERE c.deviceId = :deviceId")
    void deleteByDeviceId(@Param("deviceId") String deviceId);
}
