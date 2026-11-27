package erp.item.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CreateItemRequest(
        @NotBlank String sku,
        @NotBlank String name,
        @NotBlank String category,
        @NotBlank String unit,
        @NotNull @PositiveOrZero Integer safetyStock,
        @NotNull @PositiveOrZero BigDecimal unitPrice,
        Boolean active
) {}
