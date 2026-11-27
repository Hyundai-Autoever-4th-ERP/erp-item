package erp.item.dto.response;

import erp.item.domain.Item;
import java.util.List;

public record ItemPage(List<Item> content, int page, int size, long totalElements, long totalPages) {}
