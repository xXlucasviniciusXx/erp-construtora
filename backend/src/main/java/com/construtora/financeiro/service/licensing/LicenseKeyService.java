package com.construtora.financeiro.service.licensing;

import com.construtora.financeiro.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Gera e valida a CHAVE DE LICENCIAMENTO: um token assinado offline (HMAC-SHA256),
 * no formato {@code <payloadBase64Url>.<assinaturaBase64Url>}. Não precisa de
 * servidor central — cada VM valida com o mesmo segredo (app.license.secret).
 * O mesmo algoritmo será usado pelo painel da Fase 5 para emitir as chaves.
 */
@Service
public class LicenseKeyService {

    private static final String HMAC = "HmacSHA256";
    private final ObjectMapper objectMapper;
    private final byte[] secret;

    public LicenseKeyService(ObjectMapper objectMapper,
                             @Value("${app.license.secret}") String secret) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    /** Assina os claims e devolve a chave. */
    public String generate(LicenseClaims claims) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(claims);
            String payloadB64 = base64Url(payload);
            String sig = base64Url(hmac(payloadB64.getBytes(StandardCharsets.UTF_8)));
            return payloadB64 + "." + sig;
        } catch (Exception e) {
            throw new BusinessException("Falha ao gerar a chave de licenciamento");
        }
    }

    /** Valida a assinatura e devolve os claims; lança {@link BusinessException} se inválida. */
    public LicenseClaims parse(String key) {
        if (key == null || key.isBlank()) {
            throw new BusinessException("Chave de licenciamento vazia");
        }
        String[] parts = key.trim().split("\\.");
        if (parts.length != 2) {
            throw new BusinessException("Chave de licenciamento em formato inválido");
        }
        byte[] expected = hmac(parts[0].getBytes(StandardCharsets.UTF_8));
        byte[] provided;
        try {
            provided = Base64.getUrlDecoder().decode(parts[1]);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Chave de licenciamento corrompida");
        }
        if (!MessageDigest.isEqual(expected, provided)) {
            throw new BusinessException("Chave de licenciamento inválida (assinatura não confere)");
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[0]);
            LicenseClaims claims = objectMapper.readValue(payload, LicenseClaims.class);
            if (!PlanModules.isValidPlan(claims.plan())) {
                throw new BusinessException("Plano da chave é desconhecido: " + claims.plan());
            }
            return claims;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Não foi possível ler o conteúdo da chave");
        }
    }

    private byte[] hmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(secret, HMAC));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new BusinessException("Falha ao validar a assinatura da chave");
        }
    }

    private String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}
