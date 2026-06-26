package com.nexabank.service;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.nexabank.exception.ApiException;
import com.nexabank.exception.ResourceNotFoundException;
import com.nexabank.model.Account;
import com.nexabank.model.Transaction;
import com.nexabank.repository.AccountRepository;
import com.nexabank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatementService {

    private final AccountRepository     accountRepository;
    private final TransactionRepository transactionRepository;

    private static final DeviceRgb BRAND_BLUE  = new DeviceRgb(29,  78,  216);
    private static final DeviceRgb HEADER_TEXT = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb ROW_ALT     = new DeviceRgb(241, 245, 249);
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final DateTimeFormatter D_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @Transactional(readOnly = true)
    public byte[] generate(Long accountId, String email, LocalDate from, LocalDate to) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));

        if (!account.getUser().getEmail().equals(email)) {
            throw new ApiException("Access denied to this account", HttpStatus.FORBIDDEN);
        }

        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt   = to.atTime(23, 59, 59);

        List<Transaction> txs = transactionRepository
                .findForStatement(accountId, fromDt, toDt);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            Document    doc = new Document(pdf);

            buildPdf(doc, account, txs, from, to);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF generation failed for account {}: {}", accountId, e.getMessage());
            throw new ApiException("Could not generate statement PDF", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ── PDF layout ─────────────────────────────────────────────────────────────

    private void buildPdf(Document doc, Account account,
                          List<Transaction> txs, LocalDate from, LocalDate to) {

        // ── Bank header ──
        doc.add(new Paragraph("NexaBank")
                .setFontSize(26).setBold()
                .setFontColor(BRAND_BLUE));
        doc.add(new Paragraph("Account Statement")
                .setFontSize(14).setFontColor(new DeviceRgb(100, 116, 139)));
        doc.add(new Paragraph(" "));

        // ── Account info table ──
        Table info = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                .setWidth(UnitValue.createPercentValue(60));

        addInfoRow(info, "Account Holder", account.getUser().getFullName());
        addInfoRow(info, "Account Number", account.getAccountNumber());
        addInfoRow(info, "Account Type",   account.getAccountType().name());
        addInfoRow(info, "Period",         from.format(D_FMT) + " – " + to.format(D_FMT));
        addInfoRow(info, "Current Balance",
                "PHP " + account.getBalance().toPlainString());
        addInfoRow(info, "Total Transactions", String.valueOf(txs.size()));
        doc.add(info);
        doc.add(new Paragraph(" "));

        // ── Transaction table ──
        if (txs.isEmpty()) {
            doc.add(new Paragraph("No transactions found for the selected period.")
                    .setFontColor(new DeviceRgb(100, 116, 139)));
        } else {
            doc.add(new Paragraph("Transactions")
                    .setFontSize(12).setBold()
                    .setFontColor(BRAND_BLUE));

            Table table = new Table(UnitValue.createPercentArray(
                    new float[]{2.5f, 3f, 2f, 2f, 2f}))
                    .setWidth(UnitValue.createPercentValue(100));

            // Header row
            for (String col : new String[]{"Date", "Description", "Type", "Amount", "Balance After"}) {
                table.addHeaderCell(new Cell()
                        .add(new Paragraph(col).setBold().setFontColor(HEADER_TEXT))
                        .setBackgroundColor(BRAND_BLUE)
                        .setPadding(6));
            }

            // Data rows
            boolean shade = false;
            for (Transaction tx : txs) {
                DeviceRgb bg = shade ? ROW_ALT : new DeviceRgb(255, 255, 255);
                String sign  = tx.getType().name().startsWith("TRANSFER_OUT") ||
                               tx.getType().name().equals("WITHDRAWAL") ? "−" : "+";
                String desc  = tx.getDescription() != null
                        ? tx.getDescription()
                        : tx.getType().name().replace("_", " ");

                addDataCell(table, tx.getCreatedAt().format(DT_FMT),               bg, false);
                addDataCell(table, desc,                                             bg, false);
                addDataCell(table, tx.getType().name().replace("_", " "),           bg, false);
                addDataCell(table, sign + "PHP " + tx.getAmount().toPlainString(),  bg, true);
                addDataCell(table, "PHP "  + tx.getBalanceAfter().toPlainString(),  bg, true);
                shade = !shade;
            }

            // Summary row
            BigDecimal totalIn  = txs.stream()
                    .filter(t -> t.getType().name().equals("DEPOSIT") ||
                                 t.getType().name().equals("TRANSFER_IN"))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalOut = txs.stream()
                    .filter(t -> t.getType().name().equals("WITHDRAWAL") ||
                                 t.getType().name().equals("TRANSFER_OUT"))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            table.addCell(new Cell(1, 3)
                    .add(new Paragraph("Period Summary").setBold())
                    .setBackgroundColor(ROW_ALT).setPadding(6));
            addDataCell(table, "+PHP " + totalIn.toPlainString(),  ROW_ALT, true);
            addDataCell(table, "−PHP " + totalOut.toPlainString(), ROW_ALT, true);

            doc.add(table);
        }

        // ── Footer ──
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("Generated on " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")))
                .setFontSize(9)
                .setFontColor(new DeviceRgb(148, 163, 184)));
        doc.add(new Paragraph("This is a system-generated statement. NexaBank © 2026")
                .setFontSize(9)
                .setFontColor(new DeviceRgb(148, 163, 184)));
    }

    private void addInfoRow(Table table, String label, String value) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setBold().setFontSize(9))
                .setBorder(null).setPadding(3));
        table.addCell(new Cell()
                .add(new Paragraph(value).setFontSize(9))
                .setBorder(null).setPadding(3));
    }

    private void addDataCell(Table table, String text, DeviceRgb bg, boolean rightAlign) {
        Cell cell = new Cell()
                .add(new Paragraph(text).setFontSize(9))
                .setBackgroundColor(bg)
                .setPadding(5);
        if (rightAlign) cell.setTextAlignment(TextAlignment.RIGHT);
        table.addCell(cell);
    }
}
