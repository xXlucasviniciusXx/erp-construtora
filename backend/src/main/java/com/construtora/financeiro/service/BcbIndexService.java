package com.construtora.financeiro.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Consulta os valores oficiais dos índices de correção na API pública do
 * Banco Central (SGS — Sistema Gerenciador de Séries Temporais).
 *
 * <p>Endpoint: {@code api.bcb.gov.br/dados/serie/bcdata.sgs.{codigo}/dados/ultimos/12}
 * que retorna as 12 variações mensais (%) mais recentes. O acumulado em 12 meses
 * é o produtório composto: {@code (∏(1 + v/100) - 1) * 100}.
 *
 * <p>O resultado é cacheado por código SGS (TTL longo — os índices são mensais),
 * e qualquer falha de rede retorna {@code null} sem quebrar o fluxo do chamador.
 */
@Service
public class BcbIndexService {

    private static final Logger log = LoggerFactory.getLogger(BcbIndexService.class);
    private static final String URL =
            "https://api.bcb.gov.br/dados/serie/bcdata.sgs.%d/dados/ultimos/12?formato=json";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();
    private final ObjectMapper om = new ObjectMapper();

    /** Resultado bruto da consulta ao BCB (antes de associar a um índice). */
    public record Quote(double accumulated12m, double lastValue, String lastRef) {}

    /**
     * Busca o acumulado 12m de uma série SGS. Retorna {@code null} se o BCB
     * estiver indisponível ou a série não tiver dados. Cacheado por sgsCode.
     */
    @Cacheable(value = "bcbIndex", key = "#sgsCode", unless = "#result == null")
    public Quote accumulated12m(int sgsCode) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(URL, sgsCode)))
                    .timeout(Duration.ofSeconds(8))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("BCB SGS {} retornou HTTP {}", sgsCode, resp.statusCode());
                return null;
            }
            JsonNode arr = om.readTree(resp.body());
            if (!arr.isArray() || arr.isEmpty()) return null;

            double factor = 1.0;
            double lastValue = 0.0;
            String lastRef = null;
            for (JsonNode node : arr) {
                double v = Double.parseDouble(node.get("valor").asText().replace(",", "."));
                factor *= (1 + v / 100.0);
                lastValue = v;                       // último item = mês mais recente
                lastRef = node.get("data").asText();  // "DD/MM/AAAA"
            }
            double accumulated = (factor - 1) * 100.0;
            // Normaliza a referência para MM/AAAA
            String ref = lastRef != null && lastRef.length() == 10
                    ? lastRef.substring(3) : lastRef;
            return new Quote(round(accumulated), round(lastValue), ref);
        } catch (Exception e) {
            log.warn("Falha ao consultar BCB SGS {}: {}", sgsCode, e.getMessage());
            return null;
        }
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
