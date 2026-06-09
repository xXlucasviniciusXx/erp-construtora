package com.construtora.financeiro.parser;

import com.construtora.financeiro.model.enums.FileFormat;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser de extrato em PDF (texto). Ajustado ao layout do Asaas (tabela
 * Data | Movimentações | Valor). Como a descrição pode quebrar em várias linhas
 * e o valor cair noutra linha, agrupa cada lançamento da linha de <b>data</b>
 * até a próxima data e extrai o <b>R$ valor</b> de qualquer ponto do bloco.
 * Linhas de saldo e de cabeçalho de página (que se repetem no PDF de várias
 * páginas) são descartadas.
 *
 * <p>O XLSX é o caminho recomendado para o Asaas (colunas estruturadas + id de
 * transação); o PDF é alternativa para o extrato impresso. Para idempotência,
 * gera um identificador sintético por lançamento.
 */
@Component
public class PdfStatementParser implements BankStatementParser {

    private static final Pattern LEADING_DATE = Pattern.compile("^(\\d{2}/\\d{2}/\\d{4})\\b");
    private static final Pattern VALUE = Pattern.compile("R\\$\\s*-?\\s*[\\d.][\\d.,]*");
    // Ruído que se repete nas quebras de página do PDF (cabeçalho/saldo).
    private static final Pattern NOISE = Pattern.compile(
            "(?i)\\b(saldo|cnpj|ag[êe]ncia|conta:|per[íi]odo|extrato gerado|movimenta[çc][õo]es|empreendimento imobiliario)\\b");

    @Override
    public FileFormat supportedFormat() {
        return FileFormat.PDF;
    }

    @Override
    public List<ParsedTransaction> parse(InputStream inputStream) throws Exception {
        String text;
        try (PDDocument doc = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);   // preserva a ordem visual
            text = stripper.getText(doc);
        }

        List<ParsedTransaction> result = new ArrayList<>();
        List<String> block = new ArrayList<>();
        for (String raw : text.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty() || NOISE.matcher(line).find()) continue; // pula saldo/cabeçalho

            if (LEADING_DATE.matcher(line).find()) {
                emit(block, result);   // fecha o lançamento anterior
                block = new ArrayList<>();
            }
            block.add(line);
        }
        emit(block, result);
        return result;
    }

    /** Monta um lançamento a partir do bloco (data .. próxima data). */
    private void emit(List<String> block, List<ParsedTransaction> out) {
        if (block.isEmpty()) return;
        String joined = String.join(" ", block).replaceAll("\\s+", " ").trim();

        Matcher dm = LEADING_DATE.matcher(joined);
        if (!dm.find()) return;
        Matcher vm = VALUE.matcher(joined);
        if (!vm.find()) return;                // sem "R$ valor" → não é lançamento

        LocalDate date;
        BigDecimal amount;
        try {
            date = ParserSupport.parseDate(dm.group(1));
            amount = ParserSupport.parseAmount(vm.group());
        } catch (Exception ignore) {
            return;
        }
        // descrição = bloco sem a data inicial e sem o "R$ valor"
        String description = joined.substring(dm.end()).replace(vm.group(), " ").replaceAll("\\s+", " ").trim();
        if (description.isEmpty() || description.length() > 200) return;

        String document = ParserSupport.extractDocument(description);
        String identifier = "pdf:" + date + ":" + amount.toPlainString()
                + ":" + Integer.toHexString(description.hashCode());
        out.add(new ParsedTransaction(date, description, amount, document, identifier));
    }
}
