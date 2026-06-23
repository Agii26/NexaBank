package com.nexabank.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_logs_user_id",    columnList = "user_id"),
        @Index(name = "idx_audit_logs_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nullable — some audit events are system-generated (e.g., scheduled jobs).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** Short verb-noun code, e.g. "TRANSFER_INITIATED", "ACCOUNT_FROZEN". */
    @Column(nullable = false, length = 100)
    private String action;

    /** The entity type affected, e.g. "Account", "User". */
    @Column(name = "entity_type", length = 50)
    private String entityType;

    /** Primary key of the affected entity — for quick lookup. */
    @Column(name = "entity_id")
    private Long entityId;

    /** IPv4 or IPv6 address of the requester. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** JSON or plain-text detail about the event. */
    @Column(columnDefinition = "TEXT")
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
