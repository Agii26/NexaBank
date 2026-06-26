package com.nexabank.repository;

import com.nexabank.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // All transactions for one account, newest first — used for statement + history
    Page<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

    // Date-range filter for PDF statement (Phase 5)
    @Query("""
        SELECT t FROM Transaction t
        WHERE t.account.id = :accountId
          AND t.createdAt BETWEEN :from AND :to
        ORDER BY t.createdAt DESC
    """)
    Page<Transaction> findByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("from")      LocalDateTime from,
            @Param("to")        LocalDateTime to,
            Pageable pageable);

    // Used by StatementService — returns all transactions in range without pagination
    @Query("""
        SELECT t FROM Transaction t
        WHERE t.account.id = :accountId
          AND t.createdAt BETWEEN :from AND :to
        ORDER BY t.createdAt DESC
    """)
    List<Transaction> findForStatement(
            @Param("accountId") Long accountId,
            @Param("from")      LocalDateTime from,
            @Param("to")        LocalDateTime to);
}