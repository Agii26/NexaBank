package com.nexabank.controller;

import com.nexabank.service.StatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/statements")
@RequiredArgsConstructor
@Tag(name = "Statements", description = "PDF account statement download")
@SecurityRequirement(name = "bearerAuth")
public class StatementController {

    private final StatementService statementService;

    @GetMapping(value = "/{accountId}", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Download PDF statement for a date range")
    public ResponseEntity<byte[]> download(
            @PathVariable Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication auth) {

        byte[] pdf = statementService.generate(accountId, auth.getName(), from, to);
        String filename = String.format("statement-%d-%s-to-%s.pdf", accountId, from, to);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
