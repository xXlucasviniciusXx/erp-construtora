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
 * DRE (Demonstração do Resultado) em base caixa: receitas recebidas no período
 * menos despesas pagas no período, opcionalmente filtrado por empreendimento.
 *
 * <p>Receitas em linhas fixas: "Receita de Vendas" (parcelas recebidas) e
 * "Outras Receitas" (recebíveis avulsos). Despesas agrupadas por grupo da
 * categoria (plano de contas).
 */
@Service
@Transactional(readOnly = true)
public class DreService {

    private final NamedParameterJdbcTemplate jdbc;

    public DreService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public DreResponse dre(LocalDate from, LocalDate to, UUID developmentId) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("from", from != null ? from : LocalDate.of(1900, 1, 1))
                .addValue("to", to != null ? to : LocalDate.of(2999, 12, 31));

        String instDevF = "";   // filtro de empreendimento em parcelas
        String recvDevF = "";   // filtro em recebíveis avulsos
        String payDevF = "";    // filtro em contas a pagar
        if (developmentId != null) {
            p.addValue("devId", developmentId);
            instDevF = " and bk.development_id = :devId";
            recvDevF = " and exists (select 1 from property_sales s2 "
                    + "join lots lt2 on lt2.id = s2.lot_id "
                    + "join blocks bk2 on bk2.id = lt2.block_id "
                    + "where s2.id = ar.sale_id and bk2.development_id = :devId)";
            payDevF = " and ap.development_id = :devId";
        }

        // ---- Receitas (recebidas no período) ----
        double salesRevenue = num("""
                select coalesce(sum(i.amount),0)
                from installments i
                  join property_sales s on s.id = i.sale_id
                  join lots lt on lt.id = s.lot_id
                  join blocks bk on bk.id = lt.block_id
                where i.status='PAID' and i.payment_date between :from and :to""" + instDevF, p);
        double otherRevenue = num("""
                select coalesce(sum(ar.amount),0)
                from accounts_receivable ar
                where ar.status='RECEIVED' and ar.receive_date between :from and :to""" + recvDevF, p);

        List<Point> revenues = new ArrayList<>();
        revenues.add(new Point("Receita de Vendas (parcelas recebidas)", salesRevenue));
        revenues.add(new Point("Outras Receitas (recebíveis avulsos)", otherRevenue));
        double totalRevenue = salesRevenue + otherRevenue;

        // ---- Despesas (pagas no período), por grupo de categoria ----
        List<Point> expenses = jdbc.query("""
                select coalesce(c.grupo, 'Sem categoria') as label, sum(ap.amount) as value
                from accounts_payable ap
                  left join categories c on c.id = ap.category_id
                where ap.status='PAID' and ap.payment_date between :from and :to""" + payDevF
                + " group by 1 order by 2 desc",
                p, (rs, i) -> new Point(rs.getString("label"), rs.getDouble("value")));
        double totalExpense = expenses.stream().mapToDouble(Point::value).sum();

        return new DreResponse(revenues, totalRevenue, expenses, totalExpense, totalRevenue - totalExpense);
    }

    private double num(String sql, MapSqlParameterSource p) {
        Double v = jdbc.queryForObject(sql, p, Double.class);
        return v != null ? v : 0.0;
    }
}
