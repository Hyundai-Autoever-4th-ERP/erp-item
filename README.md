# 현대파츠 ERP · 부품

SKU를 기준으로 부품 마스터와 현재 단가를 관리한다. 본사 사용자가 부품을 등록하고 가격이나 활성 상태를 변경하면, 지점 사용자는 같은 SKU로 변경된 정보를 조회할 수 있다.

## API · 개발 브랜치

아래 API는 [기능 PR #2](https://github.com/Hyundai-Autoever-4th-ERP/erp-item/pull/2)에서 리뷰 중이다. 로컬 Swagger로 업무 API를 확인하려면 먼저 `git switch feat/1-item-api`로 전환한다. [테스트 토큰 발급 스크립트](https://github.com/Hyundai-Autoever-4th-ERP/erp-item/blob/feat/1-item-api/scripts/local-swagger-auth.mjs)

| 메서드 | 경로 | 기능 | 권한 |
| --- | --- | --- | --- |
| GET | `/api/v1/items` | SKU·이름 검색(`q`), 분류·활성 여부 필터, 목록 페이징 | 로그인 사용자 |
| GET | `/api/v1/items/{sku}` | 비활성 부품을 포함한 상세 정보와 현재 단가 조회 | 로그인 사용자 |
| POST | `/api/v1/items` | 부품 등록. 성공 시 201과 상세 경로(`Location`) 반환 | 본사·관리자 |
| PATCH | `/api/v1/items/{sku}` | 이름·분류·안전재고·단가·활성 여부 부분 수정 | 본사·관리자 |

등록·수정은 JWT의 `role`이 `HQ_STAFF`, `HQ_MANAGER`, `ADMIN`인 경우에 허용한다. 중복 SKU 등록은 409, 존재하지 않는 SKU 조회는 404를 반환한다.

## 로컬 실행과 Swagger

JDK 25, Docker, Node.js 18 이상을 사용한다. 첫 번째 터미널에서 PostgreSQL과 테스트 토큰 발급기를 실행한다. 앱 기동 시 기본 부품 42종 중 누락된 SKU만 추가한다. [마스터 데이터](src/main/resources/master.sql)

```sh
docker compose up -d
node scripts/local-swagger-auth.mjs
```

두 번째 터미널에서 앱을 실행한다.

```sh
AUTH_JWKS_URI=http://127.0.0.1:18081/internal/auth/jwks ./gradlew bootRun
```

[로컬 Swagger](http://localhost:8082/swagger-ui/index.html)의 **Authorize**에 첫 번째 터미널의 토큰 문자열을 입력한다. `HQ_STAFF`로 목록을 조회하면 200, `BRANCH_STAFF`로 부품을 등록하면 403이다. 테스트 토큰은 1시간 동안 유효하며, 재발급할 때는 발급기와 앱을 함께 재시작한다.

로컬 DB는 `localhost:15432/item_db`를 사용한다. DB가 실행 중이면 `./gradlew test`로 검증할 수 있다. 실제 Auth에 연결할 때는 `AUTH_JWKS_URI`를 앱에서 접근할 수 있는 Auth 공개키 주소로 설정한다. [서비스 설정](src/main/resources/application.yaml)
