package com.construtora.financeiro.parser;

import com.construtora.financeiro.exception.BusinessException;
import com.construtora.financeiro.model.enums.FileFormat;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Seleciona o parser adequado ao formato do arquivo importado. */
@Component
public class StatementParserFactory {

    private final Map<FileFormat, BankStatementParser> parsers;

    public StatementParserFactory(List<BankStatementParser> parserList) {
        this.parsers = parserList.stream()
                .collect(Collectors.toMap(BankStatementParser::supportedFormat, Function.identity()));
    }

    public BankStatementParser forFormat(FileFormat format) {
        BankStatementParser parser = parsers.get(format);
        if (parser == null) {
            throw new BusinessException("Formato não suportado: " + format);
        }
        return parser;
    }
}
