package com.flowboard.user_service.repository;

import com.flowboard.user_service.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByPerformedBy(Long performedBy, Pageable pageable);

    Page<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:performedBy IS NULL OR a.performedBy = :performedBy) AND " +
           "(:start IS NULL OR a.timestamp >= :start) AND " +
           "(:end IS NULL OR a.timestamp <= :end)")
    Page<AuditLog> findFiltered(
        @Param("action") String action,
        @Param("performedBy") Long performedBy,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        Pageable pageable
    );

    // Last 7 days count grouped by date for chart
    @Query("SELECT FUNCTION('DATE', a.timestamp) as day, COUNT(a) FROM AuditLog a " +
           "WHERE a.timestamp >= :since GROUP BY FUNCTION('DATE', a.timestamp) ORDER BY day")
    List<Object[]> countByDay(@Param("since") LocalDateTime since);

    List<AuditLog> findTop100ByOrderByTimestampDesc();
}
