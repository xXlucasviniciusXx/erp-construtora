package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.bank.ImportResultResponse;
import com.construtora.financeiro.exception.BusinessException;
import com.construtora.financeiro.model.BankAccount;
import com.construtora.financeiro.model.BankStatementImport;
import com.construtora.financeiro.model.BankTransaction;
import com.construtora.financeiro.model.enums.FileFormat;
import com.construtora.financeiro.model.enums.ImportStatus;
import com.construtora.financeiro.model.enums.TransactionType;
import com.construtora.financeiro.parser.ParsedTransaction;
import com.construtora.financeiro.parser.StatementParserFactory;
import com.construtora.financeiro.repository.BankStatementImportRepository;
import com.construtora.financeiro.repository.BankTransactionRepository;
import com.construtora.financeiro.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Recebe o arquivo de extrato, valida, delega ao parser correto e persiste
 * as transações. Registra metadados e logs da importação. Transações com
 * identificador bancário já existente são ignoradas (idempotência).
 */
@Service
public class StatementImportService {

    private static final Logger log = LoggerFactory.getLogger(StatementImportService.class);

    private final BankAccountService bankAccountService;
    private final BankStatementImportRepository importRepository;
    private final BankTransactionRepository transactionRepository;
    private final StatementParserFactory parserFactory;

    public StatementImportService(BankAccountService bankAccountService,
                                  BankStatementImportRepository importRepository,
                                  BankTransactionRepository transactionRepository,
                                  StatementParserFactory parserFactory) {
        this.bankAccountService = bankAccountService;
        this.importRepository = importRepository;
        this.transactionRepository = transactionRepository;
        this.parserFactory = parserFactory;
    }

    @Transactional(readOnly = true)
    public List<ImportResultResponse> history(UUID bankAccountId) {
        return importRepository.findByBankAccountIdOrderByCreatedAtDesc(bankAccountId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ImportResultResponse importFile(UUID bankAccountId, MultipartFile file) {
        BankAccount account = bankAccountService.getEntity(bankAccountId);
        FileFormat format = detectFormat(file);

        BankStatementImport record = new BankStatementImport();
        record.setBankAccount(account);
        record.setFileName(file.getOriginalFilename());
        record.setFileFormat(format);
        record.setFileSize(file.getSize());
        record.setStatus(ImportStatus.PROCESSING);
        SecurityUtils.currentUserId().ifPresent(record::setImportedBy);
        record = importRepository.save(record);

        try {
            List<ParsedTransaction> parsed = parserFactory.forFormat(format).parse(file.getInputStream());
            record.setTotalRows(parsed.size());

            int imported = 0;
            for (ParsedTransaction p : parsed) {
                if (p.bankIdentifier() != null
                        && transactionRepository.existsByBankAccountIdAndBankIdentifier(
                                account.getId(), p.bankIdentifier())) {
                    continue; // já importada anteriormente
                }
                BankTransaction txn = new BankTransaction();
                txn.setStatementImport(record);
                txn.setBankAccount(account);
                txn.setTransactionDate(p.date());
                txn.setDescription(p.description());
                txn.setAmount(p.amount());
                txn.setType(p.amount().signum() >= 0 ? TransactionType.CREDIT : TransactionType.DEBIT);
                txn.setDocumentNumber(p.documentNumber());
                txn.setBankIdentifier(p.bankIdentifier());
                transactionRepository.save(txn);
                imported++;
            }
            record.setImportedRows(imported);
            record.setStatus(ImportStatus.COMPLETED);
            log.info("Importação {} concluída: {}/{} transações", record.getId(), imported, parsed.size());
        } catch (Exception e) {
            log.error("Falha ao importar extrato {}", file.getOriginalFilename(), e);
            record.setStatus(ImportStatus.FAILED);
            record.setErrorMessage(e.getMessage());
        }
        return toResponse(importRepository.save(record));
    }

    private FileFormat detectFormat(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null) {
            throw new BusinessException("Nome de arquivo ausente");
        }
        String lower = name.toLowerCase();
        if (lower.endsWith(".csv")) return FileFormat.CSV;
        if (lower.endsWith(".ofx")) return FileFormat.OFX;
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) return FileFormat.XLSX;
        if (lower.endsWith(".pdf")) return FileFormat.PDF;
        throw new BusinessException("Extensão não suportada. Use .csv, .ofx, .xlsx ou .pdf");
    }

    private ImportResultResponse toResponse(BankStatementImport i) {
        return new ImportResultResponse(
                i.getId(), i.getBankAccount().getId(), i.getFileName(), i.getFileFormat(),
                i.getStatus(),
                i.getTotalRows() != null ? i.getTotalRows() : 0,
                i.getImportedRows() != null ? i.getImportedRows() : 0,
                i.getErrorMessage(), i.getCreatedAt());
    }
}
