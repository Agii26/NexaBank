package com.nexabank.service;

import com.nexabank.model.AuditLog;
import com.nexabank.model.User;
import com.nexabank.repository.AuditLogRepository;
import com.nexabank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    /**
     * Persists an audit event asynchronously so it never blocks the main transaction.
     * Uses REQUIRES_NEW so the log is always saved even if the caller rolls back.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, String action, String entityType,
                    Long entityId, String ipAddress, String details) {
        try {
            User user = userId != null
                    ? userRepository.findById(userId).orElse(null)
                    : null;

            AuditLog entry = AuditLog.builder()
                    .user(user)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .ipAddress(ipAddress)
                    .details(details)
                    .build();

            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Audit failure must never crash the business operation
            log.error("Failed to persist audit log [{} on {}:{}]: {}",
                    action, entityType, entityId, e.getMessage());
        }
    }
}
