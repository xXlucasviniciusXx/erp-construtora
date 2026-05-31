package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.sale.SaleRequest;
import com.construtora.financeiro.dto.sale.SaleResponse;
import com.construtora.financeiro.exception.BusinessException;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.mapper.SaleMapper;
import com.construtora.financeiro.model.Client;
import com.construtora.financeiro.model.Installment;
import com.construtora.financeiro.model.Property;
import com.construtora.financeiro.model.PropertySale;
import com.construtora.financeiro.model.enums.PropertyStatus;
import com.construtora.financeiro.repository.PropertyRepository;
import com.construtora.financeiro.repository.PropertySaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SaleService {

    private final PropertySaleRepository saleRepository;
    private final PropertyRepository propertyRepository;
    private final ClientService clientService;
    private final PropertyService propertyService;
    private final NotificationService notificationService;
    private final SaleMapper mapper;

    public SaleService(PropertySaleRepository saleRepository, PropertyRepository propertyRepository,
                       ClientService clientService, PropertyService propertyService,
                       NotificationService notificationService, SaleMapper mapper) {
        this.saleRepository = saleRepository;
        this.propertyRepository = propertyRepository;
        this.clientService = clientService;
        this.propertyService = propertyService;
        this.notificationService = notificationService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> findAll() {
        return saleRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SaleResponse findById(UUID id) {
        return mapper.toResponse(getEntity(id));
    }

    public SaleResponse create(SaleRequest request) {
        Client client = clientService.getEntity(request.clientId());
        Property property = propertyService.getEntity(request.propertyId());

        if (property.getStatus() == PropertyStatus.SOLD) {
            throw new BusinessException("Imóvel já está vendido");
        }
        BigDecimal down = request.downPayment() != null ? request.downPayment() : BigDecimal.ZERO;
        if (down.compareTo(request.totalValue()) > 0) {
            throw new BusinessException("Entrada não pode ser maior que o valor total");
        }

        PropertySale sale = new PropertySale();
        sale.setClient(client);
        sale.setProperty(property);
        sale.setTotalValue(request.totalValue());
        sale.setDownPayment(down);
        sale.setInstallmentsCount(request.installmentsCount());
        sale.setFirstDueDate(request.firstDueDate());
        sale.setPaymentMethod(request.paymentMethod());
        sale.setCorrectionIndex(request.correctionIndex());
        sale.setInterestRate(orZero(request.interestRate()));
        sale.setPenaltyRate(orZero(request.penaltyRate()));
        sale.setNotes(request.notes());

        generateInstallments(sale);

        property.setStatus(PropertyStatus.SOLD);
        propertyRepository.save(property);

        PropertySale saved = saleRepository.save(sale);
        notificationService.notifySaleCreated(saved);
        return mapper.toResponse(saved);
    }

    /**
     * Gera as parcelas dividindo (valor total - entrada) pela quantidade.
     * O resíduo de arredondamento é somado à última parcela para fechar o total.
     */
    private void generateInstallments(PropertySale sale) {
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

    public void delete(UUID id) {
        PropertySale sale = getEntity(id);
        Property property = sale.getProperty();
        property.setStatus(PropertyStatus.AVAILABLE);
        propertyRepository.save(property);
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
