package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.dashboard.Point;
import com.construtora.financeiro.dto.dre.DreResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DRE em duas bases:
 * <ul>
 *   <li><b>CAIXA</b> (padrão): receitas recebidas no período × despesas pagas no período.</li>
 *   <li><b>COMPETENCIA</b>: receitas e despesas pelo vencimento no período, independente de
 *       terem sido recebidas/pagas.</li>
 * </ul>
 *
 * <p>Receitas em linhas fixas: "Receita de Vendas" (parcelas) e "Outras Receitas" (recebíveis
 * avulsos). No modo CAIXA, as Outras Receitas são detalhadas por categoria quando a conta
 * a receber tem {@code category_id} preenchido.
 */
@Service
@Transactional(readOnly = true)
public class DreService {

    public enum Basis { CAIXA, COMPETENCIA }

    private final NamedParameterJdbcTemplate jdbc;

    public DreService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public DreResponse dre(LocalDate from, LocalDate to, UUID developmentId, Basis basis) {
        if (basis == null) basis = Basis.CAIXA;
        return basis == Basis.COMPETENCIA
                ? dreAccrual(from, to, developmentId)
                : dreCash(from, to, developmentId);
    }

    // ================================================================
    //  BASE CAIXA — parcelas/recebíveis RECEBIDOS, despesas PAGAS
    // ================================================================

    private DreResponse dreCash(LocalDate from, LocalDate to, UUID developmentId) {
        MapSqlParameterSource p = params(from, to);
        String instDevF = devFilter(developmentId, p, "bk", "payDevF");
        String payDevF = "";
        if (developmentId != null) {
            p.addValue("devId", developmentId);
            payDevF = " and ap.development_id = :devId";
        }

        // Receita de Vendas (parcelas pagas)
        double salesRevenue = num("""
                select coalesce(sum(i.amount),0)
                from installments i
                  join property_sales s on s.id = i.sale_id
                  join lots lt on lt.id = s.lot_id
                  join blocks bk on bk.id = lt.block_id
                where i.status='PAID' and i.payment_date between :from and :to"""
                + instDevF, p);

        // Outras Receitas — agrupadas por categoria quando disponível
        List<Point> otherRevenues = jdbc.query("""
                select coalesce(c.name, 'Sem categoria') as label, sum(ar.amount) as value
                from accounts_receivable ar
                  left join categories c on c.id = ar.category_id
                where ar.status='RECEIVED' and ar.receive_date between :from and :to
                group by 1 order by 2 desc""", p,
                (rs, i) -> new Point(rs.getString("label"), rs.getDouble("value")));
        double otherRevenue = otherRevenues.stream().mapToDouble(Point::value).sum();

        List<Point> revenues = new ArrayList<>();
        revenues.add(new Point("Receita de Vendas (parcelas recebidas)", salesRevenue));
        if (otherRevenues.isEmpty()) {
            revenues.add(new Point("Outras Receitas (recebíveis avulsos)", 0.0));
        } else {
            otherRevenues.forEach(pt -> revenues.add(
                    new Point("Outras Receitas — " + pt.label(), pt.value())));
        }
        double totalRevenue = salesRevenue + otherRevenue;

        // Despesas pagas, por grupo de categoria
        List<Point> expenses = jdbc.query("""
                select coalesce(c.grupo, 'Sem categoria') as label, sum(ap.amount) as value
                from accounts_payable ap
                  left join categories c on c.id = ap.category_id
                where ap.status='PAID' and ap.payment_date between :from and :to"""
                + payDevF + " group by 1 order by 2 desc",
                p, (rs, i) -> new Point(rs.getString("label"), rs.getDouble("value")));
        double totalExpense = expenses.stream().mapToDouble(Point::value).sum();

        return new DreResponse(revenues, totalRevenue, expenses, totalExpense,
                totalRevenue - totalExpense, "CAIXA");
    }

    // ================================================================
    //  BASE COMPETÊNCIA — receitas/despesas pelo VENCIMENTO
    // ================================================================

    private DreResponse dreAccrual(LocalDate from, LocalDate to, UUID developmentId) {
        MapSqlParameterSource p = params(from, to);
        String instDevF = devFilter(developmentId, p, "bk", "accrualInstDev");
        String payDevF = "";
        if (developmentId != null) {
            payDevF = " and ap.development_id = :devId";
        }

        // Receita de Vendas (parcelas com vencimento no período, qualquer status exceto CANCELLED)
        double salesRevenue = num("""
                select coalesce(sum(i.amount),0)
                from installments i
                  join property_sales s on s.id = i.sale_id
                  join lots lt on lt.id = s.lot_id
                  join blocks bk on bk.id = lt.block_id
                where i.status <> 'CANCELLED' and i.due_date between :from and :to"""
                + instDevF, p);

        // Outras Receitas por categoria (vencimento no período)
        List<Point> otherRevenues = jdbc.query("""
                select coalesce(c.name, 'Sem categoria') as label, sum(ar.amount) as value
                from accounts_receivable ar
                  left join categories c on c.id = ar.category_id
                where ar.status <> 'CANCELLED' and ar.due_date between :from and :to
                group by 1 order by 2 desc""", p,
                (rs, i) -> new Point(rs.getString("label"), rs.getDouble("value")));
        double otherRevenue = otherRevenues.stream().mapToDouble(Point::value).sum();

        List<Point> revenues = new ArrayList<>();
        revenues.add(new Point("Receita de Vendas (parcelas a vencer)", salesRevenue));
        if (otherRevenues.isEmpty()) {
            revenues.add(new Point("Outras Receitas (recebíveis avulsos)", 0.0));
        } else {
            otherRevenues.forEach(pt -> revenues.add(
                    new Point("Outras Receitas — " + pt.label(), pt.value())));
        }
        double totalRevenue = salesRevenue + otherRevenue;

        // Despesas com vencimento no período, por grupo de categoria
        List<Point> expenses = jdbc.query("""
                select coalesce(c.grupo, 'Sem categoria') as label, sum(ap.amount) as value
                from accounts_payable ap
                  left join categories c on c.id = ap.category_id
                where ap.status <> 'CANCELLED' and ap.due_date between :from and :to"""
                + payDevF + " group by 1 order by 2 desc",
                p, (rs, i) -> new Point(rs.getString("label"), rs.getDouble("value")));
        double totalExpense = expenses.stream().mapToDouble(Point::value).sum();

        return new DreResponse(revenues, totalRevenue, expenses, totalExpense,
                totalRevenue - totalExpense, "COMPETENCIA");
    }

    // ================================================================
    //  Helpers
    // ================================================================

    private MapSqlParameterSource params(LocalDate from, LocalDate to) {
        return new MapSqlParameterSource()
                .addValue("from", from != null ? from : LocalDate.of(1900, 1, 1))
                .addValue("to",   to   != null ? to   : LocalDate.of(2999, 12, 31));
    }

    /** Adiciona filtro de empreendimento (via lots→blocks) e retorna o SQL snippet. */
    private String devFilter(UUID developmentId, MapSqlParameterSource p,
                              String blocksAlias, String key) {
        if (developmentId == null) return "";
        p.addValue("devId", developmentId);
        return " and " + blocksAlias + ".development_id = :devId";
    }

    private double num(String sql, MapSqlParameterSource p) {
        Double v = jdbc.queryForObject(sql, p, Double.class);
        return v != null ? v : 0.0;
    }
}
