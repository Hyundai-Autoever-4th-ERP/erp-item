package erp.item.controller;

import erp.item.controller.docs.ItemApi;
import erp.item.domain.Item;
import erp.item.dto.request.CreateItemRequest;
import erp.item.dto.request.UpdateItemRequest;
import erp.item.dto.response.ItemPage;
import erp.item.service.ItemService;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
class ItemController implements ItemApi {
    private final ItemService itemService;

    ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @Override
    public ItemPage list(String q, String category, String active, int page, int size) {
        return itemService.list(q, category, active, page, size);
    }

    @Override
    public Item detail(String sku) {
        return itemService.get(sku);
    }

    @Override
    public ResponseEntity<Item> create(CreateItemRequest input) {
        Item item = itemService.create(input);
        URI location = UriComponentsBuilder.fromPath("/api/v1/items/{sku}")
                .encode()
                .buildAndExpand(item.sku())
                .toUri();
        return ResponseEntity.created(location).body(item);
    }

    @Override
    public Item update(String sku, UpdateItemRequest input) {
        return itemService.update(sku, input);
    }
}
