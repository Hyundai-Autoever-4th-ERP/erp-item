package erp.item.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
class ItemAdvice {
    @ExceptionHandler(ItemError.class)
    ResponseEntity<ApiError> domain(ItemError error) {
        return ResponseEntity.status(error.status).body(new ApiError(error.code, error.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> invalid() {
        return domain(ItemError.invalid());
    }
}
