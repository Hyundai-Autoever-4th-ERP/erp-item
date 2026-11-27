package erp.item.service;

import erp.item.domain.Item;
import erp.item.dto.request.CreateItemRequest;
import erp.item.dto.request.UpdateItemRequest;
import erp.item.dto.response.ItemPage;
import erp.item.exception.ItemError;
import erp.item.repository.ItemRepository;
import org.springframework.stereotype.Service;

@Service
public class ItemService {
    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public ItemPage list(String q, String category, String active, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw ItemError.invalid();
        }
        if (active != null && !active.equals("true") && !active.equals("false")) {
            throw ItemError.invalid();
        }
        Boolean activeFilter = active == null ? null : Boolean.valueOf(active);
        return itemRepository.list(q, category, activeFilter, page, size);
    }

    public Item get(String sku) {
        return itemRepository.get(sku);
    }

    public Item create(CreateItemRequest input) {
        return itemRepository.create(input);
    }

    public Item update(String sku, UpdateItemRequest input) {
        if (input.empty()) {
            throw ItemError.invalid();
        }
        return itemRepository.update(sku, input);
    }
}
