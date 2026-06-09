package com.construtora.financeiro.parser;

import com.construtora.financeiro.model.enums.FileFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser de extrato em planilha (XLSX/XLS) — ex.: exportação do Asaas.
 *
 * <p>Detecta a linha de cabeçalho (que pode não ser a primeira) procurando
 * colunas de <b>data</b> e <b>valor</b>, e mapeia de forma flexível:
 * <pre>
 *   Data / Date                         -> data
 *   Valor / Amount / Value              -> valor (com sinal: negativo = débito)
 *   Descrição / Histórico / Movimentações -> descrição (traz o nome do pagador)
 *   Transação / ID / Identificador      -> identificador (idempotência)
 * </pre>
 * Linhas sem data/valor válidos (cabeçalho, "Saldo Inicial", vazias) são ignoradas.
 */
@Component
public class XlsxStatementParser implements BankStatementParser {

    @Override
    public FileFormat supportedFormat() {
        return FileFormat.XLSX;
    }

    @Override
    public List<ParsedTransaction> parse(InputStream inputStream) throws Exception {
        List<ParsedTransaction> result = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(inputStream)) {
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) return result;

            int[] cols = findHeader(sheet);   // [headerRow, idxDate, idxAmount, idxDesc, idxId]
            if (cols == null) {
                throw new IllegalArgumentException(
                        "Planilha deve conter um cabeçalho com colunas de 'data' e 'valor'");
            }
            int headerRow = cols[0], idxDate = cols[1], idxAmount = cols[2], idxDesc = cols[3], idxId = cols[4];

            for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String rawDate = cellString(row.getCell(idxDate));
                String rawAmount = cellString(row.getCell(idxAmount));
                if (rawDate.isBlank() || rawAmount.isBlank()) continue; // pula "Saldo Inicial" / vazias

                LocalDate date;
                BigDecimal amount;
                try {
                    date = cellDate(row.getCell(idxDate));
                    amount = cellAmount(row.getCell(idxAmount));
                } catch (Exception ignore) {
                    continue; // linha sem data/valor parseável — ignora
                }

                String description = idxDesc >= 0 ? cellString(row.getCell(idxDesc)) : "";
                String document = ParserSupport.extractDocument(description); // Asaas não traz CPF; null ok
                String identifier = idxId >= 0 ? blankToNull(cellString(row.getCell(idxId))) : null;

                result.add(new ParsedTransaction(date, description, amount, document, identifier));
            }
        }
        return result;
    }

    /** Procura a linha de cabeçalho nas primeiras 15 linhas. */
    private int[] findHeader(Sheet sheet) {
        int scanTo = Math.min(sheet.getLastRowNum(), 15);
        for (int r = sheet.getFirstRowNum(); r <= scanTo; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            Map<String, Integer> byName = new HashMap<>();
            for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
                String h = cellString(row.getCell(c)).toLowerCase().trim();
                if (!h.isBlank()) byName.putIfAbsent(h, c);
            }
            int idxDate = firstOf(byName, "data", "date");
            int idxAmount = firstOf(byName, "valor", "amount", "value");
            if (idxDate >= 0 && idxAmount >= 0) {
                int idxDesc = containsOf(byName, "descri", "histor", "movimenta", "memo", "description");
                int idxId = firstOf(byName, "transação", "transacao", "id", "identificador", "fitid", "identifier");
                return new int[]{r, idxDate, idxAmount, idxDesc, idxId};
            }
        }
        return null;
    }

    private int firstOf(Map<String, Integer> byName, String... names) {
        for (String n : names) {
            Integer i = byName.get(n);
            if (i != null) return i;
        }
        return -1;
    }

    /** Match por "contém" (ex.: "descrição" casa "descri"). */
    private int containsOf(Map<String, Integer> byName, String... fragments) {
        for (var e : byName.entrySet()) {
            for (String f : fragments) {
                if (e.getKey().contains(f)) return e.getValue();
            }
        }
        return -1;
    }

    private String cellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double d = cell.getNumericCellValue();
                // inteiros (ids/transação) sem notação científica; demais via BigDecimal
                yield (d == Math.rint(d) && !Double.isInfinite(d))
                        ? String.valueOf((long) d)
                        : BigDecimal.valueOf(d).toPlainString();
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue().trim(); }
                catch (Exception e) { yield BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString(); }
            }
            default -> "";
        };
    }

    private LocalDate cellDate(Cell cell) {
        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        return ParserSupport.parseDate(cellString(cell));
    }

    private BigDecimal cellAmount(Cell cell) {
        if (cell != null && cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        return ParserSupport.parseAmount(cellString(cell));
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
