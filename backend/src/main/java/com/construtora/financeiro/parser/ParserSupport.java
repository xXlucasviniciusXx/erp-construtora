package com.construtora.financeiro.parser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utilidades comuns aos parsers: datas, valores e extração de documento. */
final class ParserSupport {

    private ParserSupport() {}

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyyMMdd"));

    // CPF (11) ou CNPJ (14) com ou sem máscara
    private static final Pattern DOCUMENT = Pattern.compile(
            "(\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}|\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2})");

    static LocalDate parseDate(String raw) {
        String value = raw.trim();
        // OFX costuma trazer YYYYMMDDHHMMSS — usa só os 8 primeiros dígitos
        if (value.matches("\\d{8}.*")) {
            value = value.substring(0, 8);
        }
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, fmt);
            } catch (Exception ignored) {
                // tenta o próximo formato
            }
        }
        throw new IllegalArgumentException("Data inválida: " + raw);
    }

    /** Aceita "1.234,56" (pt-BR) e "1234.56" (en-US), com sinal opcional. */
    static BigDecimal parseAmount(String raw) {
        String v = raw.trim().replace("R$", "").replace(" ", "");
        boolean negative = v.startsWith("-") || v.endsWith("D");
        v = v.replace("D", "").replace("C", "");
        if (v.contains(",") && v.contains(".")) {
            v = v.replace(".", "").replace(",", ".");   // formato pt-BR
        } else if (v.contains(",")) {
            v = v.replace(",", ".");
        }
        v = v.replace("+", "").replace("-", "");
        BigDecimal amount = new BigDecimal(v.isBlank() ? "0" : v);
        return negative ? amount.negate() : amount;
    }

    static String extractDocument(String text) {
        if (text == null) return null;
        Matcher m = DOCUMENT.matcher(text);
        return m.find() ? m.group(1).replaceAll("\\D", "") : null;
    }
}
