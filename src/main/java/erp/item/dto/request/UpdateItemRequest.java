package erp.item.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

@Schema(description = "한 개 이상의 변경 필드 필요. null 입력 불가.")
public class UpdateItemRequest {
    @JsonSetter(nulls = Nulls.FAIL)
    @Pattern(regexp = "(?s).*\\S.*")
    public String name;

    @JsonSetter(nulls = Nulls.FAIL)
    @Pattern(regexp = "(?s).*\\S.*")
    public String category;

    @JsonSetter(nulls = Nulls.FAIL)
    @PositiveOrZero
    public Integer safetyStock;

    @JsonSetter(nulls = Nulls.FAIL)
    @PositiveOrZero
    public BigDecimal unitPrice;

    @JsonSetter(nulls = Nulls.FAIL)
    public Boolean active;

    public boolean empty() {
        return name == null && category == null && safetyStock == null && unitPrice == null && active == null;
    }
}
