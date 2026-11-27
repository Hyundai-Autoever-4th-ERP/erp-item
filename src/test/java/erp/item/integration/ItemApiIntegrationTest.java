package erp.item.integration;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ItemApiIntegrationTest {
    private static final String ITEMS = "/api/v1/items";
    private static RSAKey key;
    private static HttpServer jwks;
    private final HttpClient client = HttpClient.newHttpClient();
    private final String sku = "TEST-" + UUID.randomUUID();

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void configureKeys(DynamicPropertyRegistry properties) throws Exception {
        key = new RSAKeyGenerator(2048).keyID("local-test").generate();
        byte[] keys = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        jwks = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        jwks.createContext("/internal/auth/jwks", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, keys.length);
            try (var body = exchange.getResponseBody()) {
                body.write(keys);
            }
        });
        jwks.start();
        properties.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://127.0.0.1:" + jwks.getAddress().getPort() + "/internal/auth/jwks");
    }

    @AfterAll
    static void stopKeys() {
        jwks.stop(0);
    }

    @AfterEach
    void removeFixture() {
        jdbc.update("DELETE FROM items WHERE sku = ?", sku);
    }

    @Test
    void createSearchChangePriceAndDeactivate() throws Exception {
        String writer = token("HQ_STAFF", 300);
        String reader = token("BRANCH_STAFF", 300);
        var created = call("POST", ITEMS, writer, createBody());
        assertThat(created.statusCode()).isEqualTo(201);
        String detail = created.headers().firstValue("Location").orElseThrow();
        assertThat(detail).isEqualTo(ITEMS + "/" + sku);
        JsonNode item = mapper.readTree(created.body());
        assertThat(item.get("id").asLong()).isPositive();
        assertThat(item.get("active").asBoolean()).isTrue();
        assertThat(json("GET", detail, reader, null, 200)).isEqualTo(item);
        assertError("POST", ITEMS, writer, createBody(), 409, "DUPLICATE_SKU");

        JsonNode page = json("GET", ITEMS + "?q=" + sku.toLowerCase() + "&active=true&size=1&category=QA", reader, null, 200);
        assertThat(page.get("content").get(0)).isEqualTo(item);
        assertThat(page.get("totalElements").asInt()).isOne();
        assertThat(page.get("totalPages").asInt()).isOne();

        var changed = json("PATCH", detail, token("HQ_MANAGER", 300),
                "{\"active\":false,\"unitPrice\":12.75,\"safetyStock\":0}", 200);
        assertThat(changed.get("active").asBoolean()).isFalse();
        assertThat(changed.get("unitPrice").asDouble()).isEqualTo(12.75);
        assertThat(changed.get("name")).isEqualTo(item.get("name"));
        assertThat(json("GET", detail, reader, null, 200)).isEqualTo(changed);
        assertThat(json("GET", ITEMS + "?q=" + sku + "&active=true", reader, null, 200)
                .get("content").size()).isZero();
    }

    @Test
    void rejectsMalformedRequestsWithoutChangingTheItem() throws Exception {
        String admin = token("ADMIN", 300);
        JsonNode original = json("POST", ITEMS, admin, createBody(), 201);
        for (String body : new String[]{"{}", "{\"sku\":\"other\"}", "{\"unit\":\"SET\"}", "{\"name\":null}",
                "{\"category\":\"  \"}", "{\"safetyStock\":1.5}", "{\"unitPrice\":-1}"}) {
            assertError("PATCH", ITEMS + "/" + sku, admin, body, 400, "INVALID_REQUEST");
        }
        assertThat(json("GET", ITEMS + "/" + sku, admin, null, 200)).isEqualTo(original);
        assertError("POST", ITEMS, admin, "{}", 400, "INVALID_REQUEST");
        for (String filter : new String[]{"size=0", "size=101", "page=-1", "page=no", "active=yes"}) {
            assertError("GET", ITEMS + "?" + filter, admin, null, 400, "INVALID_REQUEST");
        }
        String missing = ITEMS + "/missing-" + sku;
        assertError("PATCH", missing, admin, "{\"active\":false}", 404, "ITEM_NOT_FOUND");
        assertError("GET", missing, admin, null, 404, "ITEM_NOT_FOUND");
    }

    @Test
    void enforcesBearerTokenAndWriteRole() throws Exception {
        for (String bearer : new String[]{null, "invalid", token("ADMIN", -300)}) {
            assertError("GET", ITEMS, bearer, null, 401, "UNAUTHORIZED");
        }
        String reader = token("BRANCH_STAFF", 300);
        assertError("POST", ITEMS, reader, createBody(), 403, "FORBIDDEN");
        assertError("PATCH", ITEMS + "/" + sku, reader, "{\"active\":false}", 403, "FORBIDDEN");
    }

    @Test
    void publishesSwaggerUiAndAllFourOperations() throws Exception {
        assertThat(call("GET", "/swagger-ui/index.html", null, null).statusCode()).isEqualTo(200);
        JsonNode spec = json("GET", "/v3/api-docs", null, null, 200);
        JsonNode paths = spec.get("paths");
        assertThat(paths.get(ITEMS).get("get").get("responses").has("200")).isTrue();
        assertThat(paths.get(ITEMS).get("post").get("responses").has("409")).isTrue();
        assertThat(paths.get(ITEMS + "/{sku}").get("get").get("responses").has("404")).isTrue();
        assertThat(paths.get(ITEMS + "/{sku}").get("patch").get("responses").has("403")).isTrue();
        assertThat(spec.at("/components/securitySchemes/bearerAuth/scheme").asString()).isEqualTo("bearer");
    }

    private String createBody() {
        return """
                {"sku":"%s","name":"검증 부품","category":"QA","unit":"EA","safetyStock":2,"unitPrice":10}
                """.formatted(sku);
    }

    private static String token(String role, int seconds) throws Exception {
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder().subject("test-user").claim("role", role)
                .expirationTime(Date.from(now.plusSeconds(seconds))).build();
        var signed = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        signed.sign(new RSASSASigner(key));
        return signed.serialize();
    }

    private void assertError(String method, String path, String bearer, String body, int status, String code) throws Exception {
        JsonNode error = json(method, path, bearer, body, status);
        assertThat(error.get("code").asString()).isEqualTo(code);
        assertThat(error.get("message").asString()).isNotBlank();
    }

    private JsonNode json(String method, String path, String bearer, String body, int status) throws Exception {
        var response = call(method, path, bearer, body);
        assertThat(response.statusCode()).as("%s %s: %s", method, path, response.body()).isEqualTo(status);
        return mapper.readTree(response.body());
    }

    private HttpResponse<String> call(String method, String path, String bearer, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
