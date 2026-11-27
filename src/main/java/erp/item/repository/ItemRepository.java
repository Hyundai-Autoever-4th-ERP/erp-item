package erp.item.repository;

import erp.item.domain.Item;
import erp.item.dto.request.CreateItemRequest;
import erp.item.dto.request.UpdateItemRequest;
import erp.item.dto.response.ItemPage;
import erp.item.exception.ItemError;
import java.sql.Types;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ItemRepository {
    private static final String COLUMNS = "id, sku, name, category, unit, safety_stock, unit_price, active";
    private static final String FILTER = """
            FROM items WHERE (strpos(lower(sku), lower(:q)) > 0 OR strpos(lower(name), lower(:q)) > 0)
              AND (CAST(:category AS text) IS NULL OR category = :category)
              AND (CAST(:active AS boolean) IS NULL OR active = :active)
            """;

    private final JdbcClient jdbc;

    ItemRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ItemPage list(String q, String category, Boolean active, int page, int size) {
        long count = applyFilters("SELECT count(*) " + FILTER, q, category, active)
                .query(Long.class)
                .single();
        String sql = "SELECT " + COLUMNS + " " + FILTER + " ORDER BY sku LIMIT :size OFFSET :offset";
        List<Item> items = applyFilters(sql, q, category, active)
                .param("size", size)
                .param("offset", (long) page * size)
                .query(Item.class)
                .list();
        return new ItemPage(items, page, size, count, Math.ceilDiv(count, size));
    }

    private JdbcClient.StatementSpec applyFilters(String sql, String q, String category, Boolean active) {
        return jdbc.sql(sql)
                .param("q", q)
                .param("category", category, Types.VARCHAR)
                .param("active", active, Types.BOOLEAN);
    }

    public Item get(String sku) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM items WHERE sku = :sku")
                .param("sku", sku)
                .query(Item.class)
                .optional()
                .orElseThrow(ItemError::missing);
    }

    public Item create(CreateItemRequest input) {
        return jdbc.sql("""
                INSERT INTO items (sku, name, category, unit, safety_stock, unit_price, active)
                VALUES (:sku, :name, :category, :unit, :safetyStock, :unitPrice, :active)
                ON CONFLICT (sku) DO NOTHING RETURNING
                """ + COLUMNS)
                .param("sku", input.sku())
                .param("name", input.name())
                .param("category", input.category())
                .param("unit", input.unit())
                .param("safetyStock", input.safetyStock())
                .param("unitPrice", input.unitPrice())
                .param("active", !Boolean.FALSE.equals(input.active()))
                .query(Item.class)
                .optional()
                .orElseThrow(ItemError::duplicate);
    }

    public Item update(String sku, UpdateItemRequest input) {
        return jdbc.sql("""
                UPDATE items SET name = coalesce(:name, name), category = coalesce(:category, category),
                    unit_price = coalesce(:unitPrice, unit_price), safety_stock = coalesce(:safetyStock, safety_stock),
                    active = coalesce(:active, active)
                WHERE sku = :sku RETURNING
                """ + COLUMNS)
                .param("sku", sku)
                .param("name", input.name, Types.VARCHAR)
                .param("category", input.category, Types.VARCHAR)
                .param("active", input.active, Types.BOOLEAN)
                .param("unitPrice", input.unitPrice, Types.NUMERIC)
                .param("safetyStock", input.safetyStock, Types.INTEGER)
                .query(Item.class)
                .optional()
                .orElseThrow(ItemError::missing);
    }
}
