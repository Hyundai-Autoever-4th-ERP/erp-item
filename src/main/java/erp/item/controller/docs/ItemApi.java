package erp.item.controller.docs;

import erp.item.domain.Item;
import erp.item.dto.request.CreateItemRequest;
import erp.item.dto.request.UpdateItemRequest;
import erp.item.dto.response.ItemPage;
import erp.item.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping(value = "/api/v1/items", produces = "application/json")
@Tag(name = "부품")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "UNAUTHORIZED · 토큰 없음 또는 만료",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
public interface ItemApi {
    @GetMapping
    @Operation(summary = "검색·필터 목록", description = "인증된 모든 역할. SKU 오름차순으로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "부품 목록", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "INVALID_REQUEST",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ItemPage list(
            @Parameter(description = "SKU·이름 부분 검색") @RequestParam(defaultValue = "") String q,
            @Parameter(description = "분류명 정확히 일치") @RequestParam(required = false) String category,
            @Parameter(description = "활성 여부. 생략 시 전체", schema = @Schema(type = "boolean"))
            @RequestParam(required = false) String active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @GetMapping("/{sku}")
    @Operation(summary = "부품 상세·현재 단가", description = "비활성 SKU도 조회할 수 있습니다.")
    @ApiResponse(responseCode = "200", description = "부품 상세", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", description = "ITEM_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    Item detail(@PathVariable String sku);

    @PostMapping
    @Operation(summary = "부품 등록", description = "ADMIN, HQ_MANAGER, HQ_STAFF. active 생략 시 true.")
    @ApiResponse(responseCode = "201", description = "신규 부품. Location 헤더에 상세 경로 반환", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "INVALID_REQUEST",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "DUPLICATE_SKU",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ResponseEntity<Item> create(@Valid @RequestBody CreateItemRequest input);

    @PatchMapping("/{sku}")
    @Operation(summary = "부품 정보 수정·비활성화", description = "ADMIN, HQ_MANAGER, HQ_STAFF. active=false로 거래 이력을 보존합니다.")
    @ApiResponse(responseCode = "200", description = "수정된 부품", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "INVALID_REQUEST",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "ITEM_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    Item update(@PathVariable String sku, @Valid @RequestBody UpdateItemRequest input);
}
