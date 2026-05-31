package com.construtora.financeiro.service.report;

import java.util.List;

/** Geração simples de CSV (delimitador ';', compatível com Excel pt-BR). */
public final class CsvWriter {

    private CsvWriter() {}

    public static String build(List<String> headers, List<List<Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(";", headers)).append("\r\n");
        for (List<Object> row : rows) {
            sb.append(row.stream().map(CsvWriter::escape).reduce((a, b) -> a + ";" + b).orElse(""));
            sb.append("\r\n");
        }
        return sb.toString();
    }

    private static String escape(Object value) {
        if (value == null) return "";
        String s = value.toString();
        if (s.contains(";") || s.contains("\"") || s.contains("\n")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
