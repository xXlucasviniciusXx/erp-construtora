package com.construtora.financeiro.model.enums;

/** Ciclo de vida de um distrato (rescisão de contrato de lote/terreno). */
public enum DistratoStatus {
    SOLICITADO,
    APROVADO,
    AGUARDANDO_QUITACAO_FINANCEIRA,
    CONCLUIDO,
    CANCELADO
}
