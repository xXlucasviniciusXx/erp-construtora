package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.sale.SaleRequest;
import com.construtora.financeiro.dto.sale.SaleResponse;
import com.construtora.financeiro.exception.BusinessException;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.mapper.SaleMapper;
import com.construtora.financeiro.model.Client;
import com.construtora.financeiro.model.Installment;
import com.construtora.financeiro.model.Lot;
import com.construtora.financeiro.model.PropertySale;
import com.construtora.financeiro.model.enums.InstallmentStatus;
import com.construtora.financeiro.model.enums.PropertyStatus;
import com.construtora.financeiro.repository.LotRepository;
import com.construtora.financeiro.repository.PropertySaleRepository;
import com.construtora.financeiro.annotation.Auditable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SaleService {

    /** Forma de compra que habilita o campo de entrada. */
    public static final String PURCHASE_WITH_DOWNPAYMENT = "Entrada + parcelas";

    private final PropertySaleRepository saleRepository;
    private final LotRepository lotRepository;
    private final ClientService clientService;
    private final LotService lotService;
    private final NotificationService notificationService;
    private final SaleMapper mapper;

    public SaleService(PropertySaleRepository saleRepository, LotRepository lotRepository,
                       ClientService clientService, LotService lotService,
                       NotificationService notificationService, SaleMapper mapper) {
        this.saleRepository = saleRepository;
        this.lotRepository = lotRepository;
        this.clientService = clientService;
        this.lotService = lotService;
        this.notificationService = notificationService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<SaleResponse> search(
            String q, com.construtora.financeiro.model.enums.SaleStatus status, UUID clientId,
            org.springframework.data.domain.Pageable pageable) {
        String query = (q != null && !q.isBlank()) ? q.trim() : "";
        return saleRepository.search(query, status, clientId, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public SaleResponse findById(UUID id) {
        return mapper.toResponse(getEntity(id));
    }

    @Auditable(action = "SALE_CREATE", entity = "property_sales")
    public SaleResponse create(SaleRequest request) {
        Client client = clientService.getEntity(request.clientId());
        Lot lot = lotService.getEntity(request.lotId());

        if (lot.getStatus() == PropertyStatus.SOLD) {
            throw new BusinessException("Lote já está vendido");
        }
        BigDecimal down = resolveDownPayment(request);
        if (down.compareTo(request.totalValue()) > 0) {
            throw new BusinessException("Entrada não pode ser maior que o valor vendido");
        }

        PropertySale sale = new PropertySale();
        sale.setClient(client);
        sale.setLot(lot);
        sale.setTotalValue(request.totalValue());     // valor que foi vendido
        sale.setDownPayment(down);
        sale.setInstallmentsCount(request.installmentsCount());
        sale.setFirstDueDate(request.firstDueDate());
        sale.setPurchaseType(request.purchaseType());
        sale.setPaymentMethod(request.paymentMethod());
        sale.setCorrectionIndex(request.correctionIndex());
        sale.setInterestRate(orZero(request.interestRate()));
        sale.setPenaltyRate(orZero(request.penaltyRate()));
        sale.setNotes(request.notes());

        generateInstallments(sale);

        // Integração: grava o valor vendido no lote e marca como vendido.
        lot.setSaleValue(request.totalValue());
        lot.setStatus(PropertyStatus.SOLD);
        lotRepository.save(lot);

        PropertySale saved = saleRepository.save(sale);
        notificationService.notifySaleCreated(saved);
        return mapper.toResponse(saved);
    }

    /**
     * Edita uma venda. Se o valor vendido ou a quantidade de parcelas mudar, as
     * parcelas são regeradas — porém SOMENTE se nenhuma parcela tiver sido paga.
     * Havendo parcela paga, bloqueia a alteração de valor/quantidade (os demais
     * campos seguem editáveis). O valor vendido reflete no lote/empreendimento.
     */
    @Auditable(action = "SALE_UPDATE", entity = "property_sales")
    public SaleResponse update(UUID id, SaleRequest request) {
        PropertySale sale = getEntity(id);
        Lot lot = sale.getLot();

        BigDecimal down = resolveDownPayment(request);
        if (down.compareTo(request.totalValue()) > 0) {
            throw new BusinessException("Entrada não pode ser maior que o valor vendido");
        }

        boolean valueOrCountChanged =
                sale.getTotalValue().compareTo(request.totalValue()) != 0
                || sale.getDownPayment().compareTo(down) != 0
                || !sale.getInstallmentsCount().equals(request.installmentsCount())
                || !sale.getFirstDueDate().equals(request.firstDueDate());

        boolean anyPaid = sale.getInstallments().stream()
                .anyMatch(i -> i.getStatus() == InstallmentStatus.PAID);

        if (valueOrCountChanged && anyPaid) {
            throw new BusinessException(
                    "Já existem parcelas pagas: não é possível alterar valor/entrada/quantidade. "
                    + "Ajuste manualmente as parcelas em aberto.");
        }

        // Campos sempre editáveis
        sale.setPurchaseType(request.purchaseType());
        sale.setPaymentMethod(request.paymentMethod());
        sale.setCorrectionIndex(request.correctionIndex());
        sale.setInterestRate(orZero(request.interestRate()));
        sale.setPenaltyRate(orZero(request.penaltyRate()));
        sale.setNotes(request.notes());

        if (valueOrCountChanged) {
            sale.setTotalValue(request.totalValue());
            sale.setDownPayment(down);
            sale.setInstallmentsCount(request.installmentsCount());
            sale.setFirstDueDate(request.firstDueDate());
            sale.getInstallments().clear();   // orphanRemoval apaga as antigas
            generateInstallments(sale);
            lot.setSaleValue(request.totalValue());
            lotRepository.save(lot);
        }

        return mapper.toResponse(saleRepository.save(sale));
    }

    // package-private static: sem estado de instância, para permitir teste unitário direto.
    static BigDecimal resolveDownPayment(SaleRequest request) {
        // Entrada só vale para "Entrada + parcelas"; nas demais formas é zerada.
        boolean allowsDown = PURCHASE_WITH_DOWNPAYMENT.equalsIgnoreCase(request.purchaseType());
        if (!allowsDown) return BigDecimal.ZERO;
        return request.downPayment() != null ? request.downPayment() : BigDecimal.ZERO;
    }

    /**
     * Gera as parcelas dividindo (valor vendido - entrada) pela quantidade.
     * O resíduo de arredondamento é somado à última parcela para fechar o total.
     */
    static void generateInstallments(PropertySale sale) {
        int count = sale.getInstallmentsCount();
        if (count <= 0) {
            return;
        }
        BigDecimal financed = sale.getTotalValue().subtract(sale.getDownPayment());
        BigDecimal base = financed.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        BigDecimal accumulated = base.multiply(BigDecimal.valueOf(count));
        BigDecimal remainder = financed.subtract(accumulated);

        for (int i = 1; i <= count; i++) {
            Installment inst = new Installment();
            inst.setSale(sale);
            inst.setNumber(i);
            BigDecimal amount = (i == count) ? base.add(remainder) : base;
            inst.setAmount(amount);
            inst.setDueDate(sale.getFirstDueDate().plusMonths(i - 1L));
            inst.setPaymentMethod(sale.getPaymentMethod());
            sale.getInstallments().add(inst);
        }
    }

    @Auditable(action = "SALE_DELETE", entity = "property_sales")
    public void delete(UUID id) {
        PropertySale sale = getEntity(id);
        Lot lot = sale.getLot();
        lot.setStatus(PropertyStatus.AVAILABLE);
        lot.setSaleValue(null);
        lotRepository.save(lot);
        saleRepository.delete(sale);
    }

    public PropertySale getEntity(UUID id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Venda", id));
    }

    private BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
