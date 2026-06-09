package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.dashboard.DashboardResponse;
import com.construtora.financeiro.security.DevelopmentScopeService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Indicadores consolidados do topo do dashboard. SQL nativo com escopo por
 * empreendimento (Fase E): usuários restritos só somam/contam dados dos
 * empreendimentos vinculados.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final NamedParameterJdbcTemplate jdbc;
    private final DevelopmentScopeService scope;

    public DashboardService(NamedParameterJdbcTemplate jdbc, DevelopmentScopeService scope) {
        this.jdbc = jdbc;
        this.scope = scope;
    }

    public DashboardResponse summary() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("ms", monthStart).addValue("me", monthEnd).addValue("today", today);

        var qs = scope.queryScope();
        boolean restricted = !qs.unrestricted();
        String lotSub = "(select lt.id from lots lt join blocks bk on bk.id = lt.block_id where bk.development_id in (:devIds))";
        String instF = "", recvDevF = "", payDevF = "";
        if (restricted) {
            p.addValue("devIds", qs.devIds());
            instF = " and s.lot_id in " + lotSub;
            recvDevF = " and sale_id in (select ps.id from property_sales ps where ps.lot_id in " + lotSub + ")";
            payDevF = " and development_id in (:devIds)";
        }

        BigDecimal receivableInst = big("select coalesce(sum(i.amount),0) from installments i"
                + " join property_sales s on s.id = i.sale_id"
                + " where i.status='OPEN' and i.due_date between :ms and :me" + instF, p);
        BigDecimal receivableAcc = big("select coalesce(sum(amount),0) from accounts_receivable"
                + " where status='OPEN' and due_date between :ms and :me" + recvDevF, p);
        BigDecimal totalReceivable = receivableInst.add(receivableAcc);

        BigDecimal totalPayable = big("select coalesce(sum(amount),0) from accounts_payable"
                + " where status='OPEN' and due_date between :ms and :me" + payDevF, p);

        long overdue = lng("select count(*) from installments i join property_sales s on s.id = i.sale_id"
                + " where i.status='OVERDUE'" + instF, p);
        long upcoming = lng("select count(*) from installments i join property_sales s on s.id = i.sale_id"
                + " where i.status='OPEN' and i.due_date between :today and :me" + instF, p);

        long pendingTxn = restricted
                ? lng("select count(*) from bank_transactions bt join bank_accounts ba on ba.id = bt.bank_account_id"
                        + " where bt.status='PENDING' and ba.development_id in (:devIds)", p)
                : lng("select count(*) from bank_transactions where status='PENDING'", p);

        String lotsFrom = restricted
                ? "from lots lt join blocks bk on bk.id = lt.block_id where bk.development_id in (:devIds) and lt.status="
                : "from lots where status=";
        long available = lng("select count(*) " + lotsFrom + "'AVAILABLE'", p);
        long sold = lng("select count(*) " + lotsFrom + "'SOLD'", p);

        return new DashboardResponse(
                totalReceivable, totalPayable, totalReceivable.subtract(totalPayable),
                overdue, upcoming, pendingTxn, available, sold);
    }

    private BigDecimal big(String sql, MapSqlParameterSource p) {
        BigDecimal v = jdbc.queryForObject(sql, p, BigDecimal.class);
        return v != null ? v : BigDecimal.ZERO;
    }

    private long lng(String sql, MapSqlParameterSource p) {
        Long v = jdbc.queryForObject(sql, p, Long.class);
        return v != null ? v : 0L;
    }
}
