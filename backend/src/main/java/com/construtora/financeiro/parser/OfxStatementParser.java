package com.construtora.financeiro.parser;

import com.construtora.financeiro.model.enums.FileFormat;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser de extrato OFX (SGML 1.x e XML 2.x). Faz a leitura dos blocos
 * &lt;STMTTRN&gt; sem depender de biblioteca externa, tolerando tags não
 * fechadas (estilo SGML). Cobre o caso comum de extratos de bancos brasileiros.
 *
 * TODO: validar versão/charset do cabeçalho OFX e suportar múltiplas contas
 *       (&lt;BANKACCTFROM&gt;) no mesmo arquivo.
 */
@Component
public class OfxStatementParser implements BankStatementParser {

    private static final Pattern TRN_BLOCK =
            Pattern.compile("<STMTTRN>(.*?)</STMTTRN>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    @Override
    public FileFormat supportedFormat() {
        return FileFormat.OFX;
    }

    @Override
    public List<ParsedTransaction> parse(InputStream inputStream) throws Exception {
        String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        // Normaliza SGML (tags sem fechamento) inserindo </TAG> antes da próxima tag/quebra
        String normalized = content.replaceAll("(<STMTTRN>)", "\n$1");

        List<ParsedTransaction> result = new ArrayList<>();
        Matcher blocks = TRN_BLOCK.matcher(ensureClosedBlocks(normalized));
        while (blocks.find()) {
            String block = blocks.group(1);
            String dtRaw = tag(block, "DTPOSTED");
            String amtRaw = tag(block, "TRNAMT");
            if (dtRaw == null || amtRaw == null) continue;

            String memo = firstNonNull(tag(block, "MEMO"), tag(block, "NAME"));
            String fitId = tag(block, "FITID");
            String document = firstNonNull(tag(block, "CHECKNUM"), ParserSupport.extractDocument(memo));

            var date = ParserSupport.parseDate(dtRaw);
            BigDecimal amount = ParserSupport.parseAmount(amtRaw);
            result.add(new ParsedTransaction(date, memo, amount,
                    document == null ? null : document.replaceAll("\\D", ""), fitId));
        }
        return result;
    }

    /** Lê o valor de uma tag OFX, parando na próxima tag ou quebra de linha (estilo SGML). */
    private String tag(String block, String name) {
        Matcher m = Pattern.compile("<" + name + ">([^<\\r\\n]*)", Pattern.CASE_INSENSITIVE).matcher(block);
        return m.find() ? m.group(1).trim() : null;
    }

    /** Garante que cada &lt;STMTTRN&gt; tenha um fechamento para o regex de bloco funcionar. */
    private String ensureClosedBlocks(String content) {
        if (content.toUpperCase().contains("</STMTTRN>")) {
            return content;
        }
        // SGML sem fechamento: fecha cada bloco no início do próximo (ou no fim da lista)
        return content
                .replaceAll("(?i)(<STMTTRN>)", "</STMTTRN>$1")
                .replaceFirst("(?i)</STMTTRN>", "")            // remove o primeiro fechamento órfão
                .replaceAll("(?i)(</BANKTRANLIST>)", "</STMTTRN>$1");
    }

    private String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
}
