package erp.item.domain;

import java.math.BigDecimal;

public record Item(long id, String sku, String name, String category, String unit,
                   int safetyStock, BigDecimal unitPrice, boolean active) {}
