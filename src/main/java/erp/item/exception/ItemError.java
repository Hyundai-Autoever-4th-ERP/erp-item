package erp.item.exception;

import org.springframework.http.HttpStatus;

public class ItemError extends RuntimeException {
    final HttpStatus status;
    final String code;

    private ItemError(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ItemError invalid() {
        return new ItemError(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청값을 확인해 주세요.");
    }

    public static ItemError missing() {
        return new ItemError(HttpStatus.NOT_FOUND, "ITEM_NOT_FOUND", "해당 SKU의 부품이 없습니다.");
    }

    public static ItemError duplicate() {
        return new ItemError(HttpStatus.CONFLICT, "DUPLICATE_SKU", "이미 등록된 SKU입니다.");
    }
}
