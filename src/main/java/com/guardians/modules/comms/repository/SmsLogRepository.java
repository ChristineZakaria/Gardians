package com.guardians.modules.comms.repository;

import com.guardians.modules.comms.entity.SmsLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SmsLogRepository extends JpaRepository<SmsLogEntry, Long> {

    List<SmsLogEntry> findByDeviceIdOrderByTimestampDesc(String deviceId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM SmsLogEntry s WHERE s.deviceId = :deviceId")
    void deleteByDeviceId(@Param("deviceId") String deviceId);
}
