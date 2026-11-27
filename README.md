# ERP Item

## 로컬 실행

JDK 25와 Docker가 필요합니다. 클론한 저장소에서 커밋 훅을 한 번 설정하세요.

```sh
git config --local core.hooksPath .githooks
```

로컬 DB를 켜고 앱을 실행하세요.

```sh
docker compose up -d
./gradlew bootRun
```

IntelliJ에서는 프로젝트 SDK와 Gradle JVM을 25로 설정하고 `ItemApplication`을 실행합니다. Windows 터미널에서는 `.\gradlew.bat bootRun`을 사용합니다.

- 상태 확인 [Actuator Health](http://localhost:8082/actuator/health)
- API 문서 [Swagger UI](http://localhost:8082/swagger-ui/index.html)
- 테스트는 로컬 DB를 실행한 상태에서 `./gradlew test`로 확인합니다.

## 로컬 DB 접속

| 접속 항목 | 값 |
| --- | --- |
| 주소 | `localhost:15432` |
| 데이터베이스 | `item_db` |
| 사용자 | `item_app` |
| 로컬 비밀번호 | `item_local` |

`docker compose down`으로 종료해도 데이터는 볼륨에 남습니다.

처음 DB를 만들면 `items` 테이블에 품목 42종이 들어갑니다. 기존 로컬 DB는 앱을 실행하면 누락된 마스터가 추가됩니다.

## 부품 API 확인

Swagger UI의 **Authorize**에 Auth에서 발급한 액세스 토큰을 입력합니다. 조회는 로그인한 사용자, 등록과 수정은 `ADMIN`, `HQ_MANAGER`, `HQ_STAFF`가 사용할 수 있습니다.

| 메서드 | 경로 | 확인할 내용 |
| --- | --- | --- |
| GET | `/api/v1/items` | `q`로 SKU·이름 검색, `category`와 `active`로 필터. 페이지는 0부터 시작 |
| GET | `/api/v1/items/{sku}` | 비활성 부품을 포함한 상세 정보와 현재 단가 |
| POST | `/api/v1/items` | 등록 시 201, 응답의 Location 헤더로 상세 조회 |
| PATCH | `/api/v1/items/{sku}` | 이름·분류·단가·안전재고·활성 여부 부분 수정 |

로컬 공개키 주소는 `http://localhost:8081/internal/auth/jwks`입니다. Auth를 다른 호스트에서 실행할 때는 Item의 `AUTH_JWKS_URI`를 해당 주소로 설정하세요. Item은 서명된 JWT의 `role` 클레임을 사용합니다.

Auth 구현과 독립적으로 검증하려면 로컬 DB를 켠 뒤 아래 테스트를 실행하세요. 테스트에서 임시 RSA 키와 공개키 서버를 생성하고, 실제 HTTP로 정상·오류 응답을 확인한 뒤 생성한 테스트 부품을 정리합니다.

```sh
./gradlew test --tests '*ItemApiIntegrationTest'
```
