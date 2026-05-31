package com.construtora.financeiro.parser;

import com.construtora.financeiro.model.enums.FileFormat;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser de extrato CSV. Espera um cabeçalho identificando as colunas.
 * Colunas reconhecidas (nomes flexíveis, case-insensitive):
 *   data | date            -> data da transação
 *   descricao | historico  -> descrição
 *   valor | amount         -> valor (negativo = débito)
 *   documento | cpf/cnpj   -> documento (opcional)
 *   id | identificador     -> identificador bancário (opcional)
 * Delimitador detectado automaticamente entre ';' e ','.
 */
@Component
public class CsvStatementParser implements BankStatementParser {

    @Override
    public FileFormat supportedFormat() {
        return FileFormat.CSV;
    }

    @Override
    public List<ParsedTransaction> parse(InputStream inputStream) throws Exception {
        List<ParsedTransaction> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return result;
            }
            char delimiter = headerLine.chars().filter(c -> c == ';').count()
                    >= headerLine.chars().filter(c -> c == ',').count() ? ';' : ',';

            String[] headers = split(headerLine, delimiter);
            int idxDate = indexOf(headers, "data", "date");
            int idxDesc = indexOf(headers, "descricao", "descrição", "historico", "histórico", "description", "memo");
            int idxAmount = indexOf(headers, "valor", "amount", "value");
            int idxDoc = indexOf(headers, "documento", "cpf", "cnpj", "cpf/cnpj", "document");
            int idxId = indexOf(headers, "id", "identificador", "fitid", "identifier");

            if (idxDate < 0 || idxAmount < 0) {
                throw new IllegalArgumentException(
                        "CSV deve conter ao menos as colunas 'data' e 'valor'");
            }

            int lineNumber = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) continue;
                String[] cols = split(line, delimiter);
                try {
                    var date = ParserSupport.parseDate(get(cols, idxDate));
                    String description = get(cols, idxDesc);
                    BigDecimal amount = ParserSupport.parseAmount(get(cols, idxAmount));
                    String document = idxDoc >= 0 ? get(cols, idxDoc) : null;
                    if (document == null || document.isBlank()) {
                        document = ParserSupport.extractDocument(description);
                    } else {
                        document = document.replaceAll("\\D", "");
                    }
                    String identifier = idxId >= 0 ? blankToNull(get(cols, idxId)) : null;
                    result.add(new ParsedTransaction(date, description, amount, document, identifier));
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Erro ao processar linha " + lineNumber + ": " + e.getMessage(), e);
                }
            }
        }
        return result;
    }

    private String[] split(String line, char delimiter) {
        String[] parts = line.split(String.valueOf(delimiter), -1);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim().replaceAll("^\"|\"$", "");
        }
        return parts;
    }

    private int indexOf(String[] headers, String... names) {
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].toLowerCase().trim();
            for (String name : names) {
                if (h.equals(name)) return i;
            }
        }
        return -1;
    }

    private String get(String[] cols, int idx) {
        return (idx >= 0 && idx < cols.length) ? cols[idx] : "";
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
