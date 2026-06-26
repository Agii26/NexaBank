package com.nexabank.controller;

import com.nexabank.dto.response.AccountAdminResponse;
import com.nexabank.dto.response.PageResponse;
import com.nexabank.dto.response.UserAdminResponse;
import com.nexabank.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin-only operations")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @Operation(summary = "List all registered users")
    public ResponseEntity<PageResponse<UserAdminResponse>> getUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllUsers(
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/accounts")
    @Operation(summary = "List all accounts across all users")
    public ResponseEntity<PageResponse<AccountAdminResponse>> getAccounts(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllAccounts(
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @PatchMapping("/accounts/{id}/freeze")
    @Operation(summary = "Freeze an account — blocks all transactions")
    public ResponseEntity<AccountAdminResponse> freeze(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(adminService.freezeAccount(id, auth.getName()));
    }

    @PatchMapping("/accounts/{id}/unfreeze")
    @Operation(summary = "Unfreeze an account — restores normal operation")
    public ResponseEntity<AccountAdminResponse> unfreeze(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(adminService.unfreezeAccount(id, auth.getName()));
    }
}
