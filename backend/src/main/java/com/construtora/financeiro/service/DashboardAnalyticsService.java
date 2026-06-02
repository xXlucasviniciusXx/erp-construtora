package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.dashboard.DashboardAnalyticsResponse;
import com.construtora.financeiro.dto.dashboard.Point;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agregações do dashboard (SQL nativo PostgreSQL), com filtros opcionais de
 * período, cliente e imóvel. Os filtros incidem sobre as métricas financeiras e
 * de vendas; os totais de carteira (lotes, contagem de clientes) permanecem
 * como retrato atual.
 */
@Service
@Transactional(readOnly = true)
public class DashboardAnalyticsService {

    private final NamedParameterJdbcTemplate jdbc;

    public DashboardAnalyticsService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public DashboardAnalyticsResponse analytics(LocalDate from, LocalDate to, UUID clientId, UUID propertyId) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("from", from != null ? from : LocalDate.of(1900, 1, 1))
                .addValue("to", to != null ? to : LocalDate.of(2999, 12, 31));

        // Filtros opcionais, montados como fragmentos de SQL (evita params nulos tipados)
        String instF = "";   // para queries de installments com alias i + join s (property_sales)
        String saleF = "";   // para queries de property_sales com alias ps
        if (clientId != null) {
            p.addValue("clientId", clientId);
            instF += " and s.client_id = :clientId";
            saleF += " and ps.client_id = :clientId";
        }
        if (propertyId != null) {
            p.addValue("propertyId", propertyId);
            instF += " and s.lot_id = :propertyId";
            saleF += " and ps.lot_id = :propertyId";
        }

        // ---- Cards ----
        BigDecimal totalSold = big("select coalesce(sum(ps.total_value),0) from property_sales ps "
                + "where ps.sale_date between :from and :to" + saleF, p);
        BigDecimal totalReceived = big("""
                select coalesce(sum(i.amount),0) from installments i
                  join property_sales s on s.id = i.sale_id
                 where i.status='PAID' and i.payment_date between :from and :to""" + instF, p);
        BigDecimal totalOpen = big("""
                select coalesce(sum(i.amount),0) from installments i
                  join property_sales s on s.id = i.sale_id
                 where i.status in ('OPEN','OVERDUE')""" + instF, p);
        BigDecimal totalOverdue = big("""
                select coalesce(sum(i.amount),0) from installments i
                  join property_sales s on s.id = i.sale_id
                 where i.status in ('OPEN','OVERDUE') and i.due_date < current_date""" + instF, p);
        long delinquent = lng("""
                select count(distinct s.client_id) from installments i
                  join property_sales s on s.id = i.sale_id
                 where i.status in ('OPEN','OVERDUE') and i.due_date < current_date""" + instF, p);

        // Retrato de carteira (não filtrado por período)
        long active = lng("select count(*) from clients where status='ACTIVE'", p);
        long inactive = lng("select count(*) from clients where status='INACTIVE'", p);
        long lotsSold = lng("select count(*) from lots where status='SOLD'", p);
        long lotsAvailable = lng("select count(*) from lots where status='AVAILABLE'", p);

        // ---- Séries ----
        List<Point> received = points("""
                select to_char(i.payment_date,'YYYY-MM') as label, sum(i.amount) as value
                from installments i join property_sales s on s.id = i.sale_id
                where i.status='PAID' and i.payment_date between :from and :to""" + instF
                + " group by 1 order by 1", p);
        List<Point> toReceive = points("""
                select to_char(i.due_date,'YYYY-MM') as label, sum(i.amount) as value
                from installments i join property_sales s on s.id = i.sale_id
                where i.status in ('OPEN','OVERDUE') and i.due_date >= date_trunc('month', current_date)
                  and i.due_date < date_trunc('month', current_date) + interval '6 months'""" + instF
                + " group by 1 order by 1", p);
        List<Point> overdueMonth = points("""
                select to_char(i.due_date,'YYYY-MM') as label, sum(i.amount) as value
                from installments i join property_sales s on s.id = i.sale_id
                where i.status in ('OPEN','OVERDUE') and i.due_date < current_date""" + instF
                + " group by 1 order by 1", p);
        List<Point> delinquencyByDev = points("""
                select dv.name as label, sum(i.amount) as value
                from installments i
                  join property_sales s on s.id = i.sale_id
                  join lots lt on lt.id = s.lot_id
                  join blocks bk on bk.id = lt.block_id
                  join developments dv on dv.id = bk.development_id
                where i.status in ('OPEN','OVERDUE') and i.due_date < current_date""" + instF
                + " group by dv.name order by 2 desc", p);
        List<Point> salesByMonth = points("""
                select to_char(ps.sale_date,'YYYY-MM') as label, sum(ps.total_value) as value
                from property_sales ps where ps.sale_date between :from and :to""" + saleF
                + " group by 1 order by 1", p);
        List<Point> salesByPurchaseType = points("""
                select coalesce(ps.purchase_type,'Não informado') as label, sum(ps.total_value) as value
                from property_sales ps where ps.sale_date between :from and :to""" + saleF
                + " group by 1 order by 2 desc", p);
        List<Point> aging = points("""
                select case
                         when current_date - i.due_date between 1 and 30 then '1-30 dias'
                         when current_date - i.due_date between 31 and 60 then '31-60 dias'
                         when current_date - i.due_date between 61 and 90 then '61-90 dias'
                         else 'Acima de 90 dias' end as label,
                       sum(i.amount) as value
                from installments i join property_sales s on s.id = i.sale_id
                where i.status in ('OPEN','OVERDUE') and i.due_date < current_date""" + instF
                + " group by 1 order by min(current_date - i.due_date)", p);

        // Contas a pagar: pagas x em aberto (sem filtro de cliente/imóvel)
        List<Point> payablesPaidVsOpen = List.of(
                new Point("Pagas", big("select coalesce(sum(amount),0) from accounts_payable where status='PAID'", p).doubleValue()),
                new Point("Em aberto", big("select coalesce(sum(amount),0) from accounts_payable where status in ('OPEN','OVERDUE')", p).doubleValue()));

        // Despesas (pagas) por empreendimento — nulo = "Geral / Administrativo"
        List<Point> expensesByDevelopment = points("""
                select coalesce(dv.name, 'Geral / Administrativo') as label, sum(ap.amount) as value
                from accounts_payable ap
                  left join developments dv on dv.id = ap.development_id
                where ap.status='PAID'
                group by 1 order by 2 desc""", p);

        // Despesas (pagas) por categoria e por centro de custo
        List<Point> expensesByCategory = points("""
                select coalesce(cat.grupo || ' / ' || cat.name, 'Sem categoria') as label, sum(ap.amount) as value
                from accounts_payable ap
                  left join categories cat on cat.id = ap.category_id
                where ap.status='PAID'
                group by 1 order by 2 desc""", p);
        List<Point> expensesByCostCenter = points("""
                select coalesce(cc.name, 'Sem centro de custo') as label, sum(ap.amount) as value
                from accounts_payable ap
                  left join cost_centers cc on cc.id = ap.cost_center_id
                where ap.status='PAID'
                group by 1 order by 2 desc""", p);

        // Lucro/prejuízo por empreendimento (caixa): recebido (parcelas pagas) − despesas pagas
        Map<String, Double> receivedByDev = toMap(points("""
                select dv.name as label, sum(i.amount) as value
                from installments i
                  join property_sales s on s.id = i.sale_id
                  join lots lt on lt.id = s.lot_id
                  join blocks bk on bk.id = lt.block_id
                  join developments dv on dv.id = bk.development_id
                where i.status='PAID'
                group by dv.name""", p));
        Map<String, Double> expenseByDev = new LinkedHashMap<>();
        expensesByDevelopment.forEach(pt -> {
            if (!"Geral / Administrativo".equals(pt.label())) expenseByDev.put(pt.label(), pt.value());
        });
        java.util.TreeSet<String> devNames = new java.util.TreeSet<>();
        devNames.addAll(receivedByDev.keySet());
        devNames.addAll(expenseByDev.keySet());
        List<Point> profitByDevelopment = new ArrayList<>();
        for (String name : devNames) {
            profitByDevelopment.add(new Point(name,
                    receivedByDev.getOrDefault(name, 0.0) - expenseByDev.getOrDefault(name, 0.0)));
        }

        // Contas a receber: recebido x a receber (parcelas + receivables avulsas, com filtro de cliente)
        String recvClientF = clientId != null ? " and client_id = :clientId" : "";
        double receivedTotal = big("""
                select coalesce(sum(i.amount),0) from installments i join property_sales s on s.id=i.sale_id
                 where i.status='PAID'""" + instF, p).doubleValue()
                + big("select coalesce(sum(amount),0) from accounts_receivable where status='RECEIVED'" + recvClientF, p).doubleValue();
        double toReceiveTotal = totalOpen.doubleValue()
                + big("select coalesce(sum(amount),0) from accounts_receivable where status in ('OPEN','OVERDUE')" + recvClientF, p).doubleValue();
        List<Point> receivablesReceivedVsOpen = List.of(
                new Point("Recebido", receivedTotal),
                new Point("A receber", toReceiveTotal));

        // Fluxo de caixa previsto = a receber - a pagar (próximos 6 meses)
        Map<String, Double> recv = toMap(toReceive);
        Map<String, Double> pay = toMap(points("""
                select to_char(due_date,'YYYY-MM') as label, sum(amount) as value
                from accounts_payable
                where status in ('OPEN','OVERDUE') and due_date >= date_trunc('month', current_date)
                  and due_date < date_trunc('month', current_date) + interval '6 months'
                group by 1 order by 1""", p));
        List<Point> cashFlow = new ArrayList<>();
        java.util.TreeSet<String> months = new java.util.TreeSet<>();
        months.addAll(recv.keySet());
        months.addAll(pay.keySet());
        for (String m : months) {
            cashFlow.add(new Point(m, recv.getOrDefault(m, 0.0) - pay.getOrDefault(m, 0.0)));
        }

        return new DashboardAnalyticsResponse(
                totalSold, totalReceived, totalOpen, totalOverdue,
                delinquent, active, inactive, lotsSold, lotsAvailable,
                received, toReceive, overdueMonth, delinquencyByDev, salesByMonth,
                salesByPurchaseType, cashFlow, payablesPaidVsOpen, receivablesReceivedVsOpen, aging,
                expensesByDevelopment, profitByDevelopment, expensesByCategory, expensesByCostCenter);
    }

    // ---- helpers ----

    private BigDecimal big(String sql, MapSqlParameterSource p) {
        BigDecimal v = jdbc.queryForObject(sql, p, BigDecimal.class);
        return v != null ? v : BigDecimal.ZERO;
    }

    private long lng(String sql, MapSqlParameterSource p) {
        Long v = jdbc.queryForObject(sql, p, Long.class);
        return v != null ? v : 0L;
    }

    private List<Point> points(String sql, MapSqlParameterSource p) {
        return jdbc.query(sql, p, (rs, i) -> new Point(rs.getString("label"), rs.getDouble("value")));
    }

    private Map<String, Double> toMap(List<Point> points) {
        Map<String, Double> m = new LinkedHashMap<>();
        points.forEach(x -> m.put(x.label(), x.value()));
        return m;
    }
}
