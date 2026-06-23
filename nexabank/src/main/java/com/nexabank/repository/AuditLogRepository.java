package com.nexabank.repository;

import com.nexabank.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Admin: see all events for a specific user (Phase 5)
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Admin: filter by action type, e.g. "ACCOUNT_FROZEN" (Phase 5)
    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);
}
