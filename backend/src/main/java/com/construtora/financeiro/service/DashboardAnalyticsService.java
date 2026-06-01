package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.dashboard.DashboardAnalyticsResponse;
import com.construtora.financeiro.dto.dashboard.Point;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agregações para o dashboard. Usa SQL nativo (PostgreSQL) por serem consultas
 * analíticas com agrupamento por mês/categoria. Concentra a complexidade aqui,
 * mantendo entidades/repos limpos.
 */
@Service
@Transactional(readOnly = true)
public class DashboardAnalyticsService {

    private final JdbcTemplate jdbc;

    public DashboardAnalyticsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public DashboardAnalyticsResponse analytics() {
        BigDecimal totalSold = big("select coalesce(sum(total_value),0) from property_sales");
        BigDecimal totalReceived = big("""
                select coalesce((select sum(amount) from installments where status='PAID'),0)
                     + coalesce((select sum(amount) from accounts_receivable where status='RECEIVED'),0)""");
        BigDecimal totalOpen = big("""
                select coalesce((select sum(amount) from installments where status in ('OPEN','OVERDUE')),0)
                     + coalesce((select sum(amount) from accounts_receivable where status='OPEN'),0)""");
        BigDecimal totalOverdue = big("""
                select coalesce(sum(amount),0) from installments
                where status in ('OPEN','OVERDUE') and due_date < current_date""");

        long delinquent = lng("""
                select count(distinct s.client_id) from installments i
                join property_sales s on s.id = i.sale_id
                where i.status in ('OPEN','OVERDUE') and i.due_date < current_date""");
        long active = lng("select count(*) from clients where status='ACTIVE'");
        long inactive = lng("select count(*) from clients where status='INACTIVE'");
        long lotsSold = lng("select count(*) from properties where status='SOLD'");
        long lotsAvailable = lng("select count(*) from properties where status='AVAILABLE'");

        List<Point> received = points("""
                select to_char(payment_date,'YYYY-MM') as label, sum(amount) as value
                from installments
                where status='PAID' and payment_date >= (date_trunc('month', current_date) - interval '5 months')
                group by 1 order by 1""");
        List<Point> toReceive = points("""
                select to_char(due_date,'YYYY-MM') as label, sum(amount) as value
                from installments
                where status in ('OPEN','OVERDUE')
                  and due_date >= date_trunc('month', current_date)
                  and due_date <  date_trunc('month', current_date) + interval '6 months'
                group by 1 order by 1""");
        List<Point> overdueMonth = points("""
                select to_char(due_date,'YYYY-MM') as label, sum(amount) as value
                from installments
                where status in ('OPEN','OVERDUE') and due_date < current_date
                group by 1 order by 1""");
        List<Point> delinquencyByDev = points("""
                select p.development as label, sum(i.amount) as value
                from installments i
                join property_sales s on s.id = i.sale_id
                join properties p on p.id = s.property_id
                where i.status in ('OPEN','OVERDUE') and i.due_date < current_date
                group by p.development order by 2 desc""");
        List<Point> salesByMonth = points("""
                select to_char(sale_date,'YYYY-MM') as label, sum(total_value) as value
                from property_sales group by 1 order by 1""");
        List<Point> salesByPayment = points("""
                select coalesce(payment_method,'Não informado') as label, sum(total_value) as value
                from property_sales group by 1 order by 2 desc""");
        List<Point> payablesPaidVsOpen = List.of(
                new Point("Pagas", big("select coalesce(sum(amount),0) from accounts_payable where status='PAID'").doubleValue()),
                new Point("Em aberto", big("select coalesce(sum(amount),0) from accounts_payable where status in ('OPEN','OVERDUE')").doubleValue()));
        List<Point> aging = points("""
                select case
                         when current_date - due_date between 1 and 30 then '1-30 dias'
                         when current_date - due_date between 31 and 60 then '31-60 dias'
                         when current_date - due_date between 61 and 90 then '61-90 dias'
                         else 'Acima de 90 dias'
                       end as label,
                       sum(amount) as value
                from installments
                where status in ('OPEN','OVERDUE') and due_date < current_date
                group by 1
                order by min(current_date - due_date)""");

        // Fluxo de caixa previsto (próximos 6 meses) = a receber - a pagar
        Map<String, Double> recv = toMap(toReceive);
        Map<String, Double> pay = toMap(points("""
                select to_char(due_date,'YYYY-MM') as label, sum(amount) as value
                from accounts_payable
                where status in ('OPEN','OVERDUE')
                  and due_date >= date_trunc('month', current_date)
                  and due_date <  date_trunc('month', current_date) + interval '6 months'
                group by 1 order by 1"""));
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
                salesByPayment, cashFlow, payablesPaidVsOpen, aging);
    }

    // ---- helpers ----

    private BigDecimal big(String sql) {
        BigDecimal v = jdbc.queryForObject(sql, BigDecimal.class);
        return v != null ? v : BigDecimal.ZERO;
    }

    private long lng(String sql) {
        Long v = jdbc.queryForObject(sql, Long.class);
        return v != null ? v : 0L;
    }

    private List<Point> points(String sql) {
        return jdbc.query(sql, (rs, i) -> new Point(rs.getString("label"), rs.getDouble("value")));
    }

    private Map<String, Double> toMap(List<Point> points) {
        Map<String, Double> m = new LinkedHashMap<>();
        points.forEach(p -> m.put(p.label(), p.value()));
        return m;
    }
}
