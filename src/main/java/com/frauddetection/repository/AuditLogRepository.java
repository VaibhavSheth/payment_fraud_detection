package com.frauddetection.repository;

import com.frauddetection.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserId(String userId);

    Optional<AuditLog> findByTxnId(String txnId);

    List<AuditLog> findByDecision(String decision);

    List<AuditLog> findByUserIdAndDecision(String userId, String decision);

    List<AuditLog> findByDecidedAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.decidedAt >= :from AND a.decidedAt <= :to AND a.decision = :decision")
    long countByDecisionAndDateRange(String decision, LocalDateTime from, LocalDateTime to);
}
