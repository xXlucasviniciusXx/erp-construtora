package com.construtora.financeiro.parser;

import com.construtora.financeiro.model.enums.FileFormat;

import java.io.InputStream;
import java.util.List;

/**
 * Contrato para parsers de extrato bancário. Para suportar um novo formato,
 * basta implementar esta interface e anotá-la com {@code @Component} — a
 * {@link StatementParserFactory} a descobre automaticamente.
 */
public interface BankStatementParser {

    FileFormat supportedFormat();

    List<ParsedTransaction> parse(InputStream inputStream) throws Exception;
}
