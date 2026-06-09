package com.construtora.financeiro.model.enums;

/** Resultado financeiro do distrato após o cálculo do saldo final. */
public enum DistratoFinancialOutcome {
    /** Saldo final &gt; 0: empresa devolve ao cliente → Contas a Pagar. */
    PAYABLE,
    /** Saldo final &lt; 0: cliente ainda deve à empresa → Contas a Receber. */
    RECEIVABLE,
    /** Saldo final = 0: sem lançamento financeiro. */
    ZERO
}
