# sheet-import-api

Google 스프레드시트나 엑셀 파일(.xlsx/.xls)의 목록을 읽어 MySQL에 저장하는 Spring Boot Web API입니다.
지금은 **상품(Product)** 을 샘플 도메인으로 넣어 두었고, 실제 시트 양식에 맞춰 바꿔 쓰는 것을 전제로 합니다.

| 항목 | 버전 |
|---|---|
| Spring Boot | 4.1.1 |
| Java | 25 (Gradle toolchain) |
| 빌드 | Gradle 9.8.2, Groovy DSL (`build.gradle`) |
| DB | MySQL 8.4 + Flyway |
| 엑셀 | Apache POI 5.5.1 |
| Google | google-api-services-sheets v4, google-auth-library (서비스 계정) |

---

## 1. 용어 정리

| 용어 | 뜻 |
|---|---|
| **서비스 계정** | 사람이 아니라 **프로그램용 구글 계정**입니다. `xxx@프로젝트.iam.gserviceaccount.com` 같은 이메일이 있고, JSON 키 파일로 로그인합니다. 서비스 계정도 "다른 사람"이라서, **시트를 이 이메일에 공유해야** 읽을 수 있습니다. |
| **A1 표기법** | 시트 범위를 적는 방식. `상품목록!A1:E` = "상품목록" 시트의 A~E열 전체, `A:Z` = 첫 번째 시트의 A~Z열. |
| **upsert** | update + insert. 같은 키(여기서는 상품코드)가 있으면 수정, 없으면 새로 넣습니다. 같은 시트를 여러 번 가져와도 중복이 생기지 않습니다. |
| **Flyway 마이그레이션** | 테이블 구조를 `V1__init.sql`, `V2__...sql`처럼 버전별 SQL 파일로 관리하는 도구. 앱이 뜰 때 아직 적용 안 된 파일만 순서대로 실행하고, 적용 이력은 `flyway_schema_history` 테이블에 남습니다. |
| **ddl-auto: validate** | JPA가 테이블을 직접 만들지 않고 "엔티티와 테이블이 맞는지"만 검사합니다. 테이블 생성은 Flyway 담당. |
| **ProblemDetail** | 오류 응답 표준(RFC 9457). `{"status":400, "detail":"...", "code":"EMPTY_FILE"}` 형태의 `application/problem+json`. |
| **sealed interface** | 구현 클래스를 정해 둔 인터페이스. `switch`에서 모든 경우를 다뤘는지 컴파일러가 검사해 줍니다. |
| **Testcontainers** | 테스트할 때 Docker로 진짜 MySQL을 잠깐 띄웠다 지우는 라이브러리. |

## 2. 동작 흐름

```
[엑셀 업로드]        [Google Sheets]
 POST .../excel       POST .../google-sheets
        │                    │
        ▼                    ▼
 ExcelSheetReader     GoogleSheetReader        ← ① 읽기: 소스마다 다름 (트랜잭션 밖)
        └────────┬───────────┘
                 ▼
            SheetTable                         ← 소스와 무관한 "헤더 + 행" 표
                 ▼
          ProductRowMapper                     ← ② 검증·변환: 잘못된 행은 이유와 함께 따로 모음
                 ▼
         ProductImportWriter  (@Transactional) ← ③ 저장: 상품 upsert + 가져오기 이력 기록
                 ▼
     201 Created + ImportJobResponse (생성/수정/변경없음/실패 건수, 실패 행 목록)
```

설계에서 신경 쓴 점:

- **읽기와 저장을 분리** — 소스가 늘어나도(CSV 등) `SheetReader` 구현만 추가하면 되고, 검증·저장 로직은 그대로입니다.
- **값은 "화면에 보이는 문자열"로 통일** — 엑셀은 POI `DataFormatter`, Google은 `FORMATTED_VALUE`. 그래서 `1000`이 `1000.0`으로 읽히거나 수식이 `=A2*B2`로 읽히지 않습니다.
- **부분 성공 허용** — 한 행이 틀렸다고 전부 버리지 않습니다. 단, 필수 컬럼이 헤더에 아예 없으면 양식 오류로 전체를 거부합니다.
- **I/O는 트랜잭션 밖** — Google API 응답을 기다리는 동안 DB 커넥션을 붙잡지 않습니다.
- **같은 값이면 UPDATE 안 함** — 재동기화 시 실제로 바뀐 행만 `updatedCount`에 잡힙니다.

## 3. 패키지 구조

```
com.leehv1234.sheetimport
├── global
│   ├── error        ErrorCode, BusinessException, GlobalExceptionHandler(ProblemDetail)
│   ├── web          PageResponse
│   └── config       OpenApiConfig (Swagger)
├── spreadsheet      도메인과 무관한 "표 읽기" 계층
│   ├── SheetSource (sealed) ─ ExcelFileSource / GoogleSheetSource
│   ├── SheetTable, SheetRow, SheetReader, RowError, MappingResult
│   ├── excel        ExcelSheetReader (Apache POI)
│   └── google       GoogleSheetReader, GoogleSheetsConfig, GoogleSheetsProperties
├── importjob        가져오기 이력 (ImportJob + 실패 행 import_job_error)
└── product          샘플 도메인: 엔티티, 시트 컬럼 정의, 검증, upsert, API
```

## 4. 실행

### 4-1. MySQL 8.4 준비

Docker가 있으면:

```bash
docker compose up -d
```

직접 설치한 MySQL을 쓴다면 DB와 계정을 만들어 둡니다.

```sql
CREATE DATABASE sheet_import;
CREATE USER 'sheet'@'%' IDENTIFIED BY 'sheet';
GRANT ALL PRIVILEGES ON sheet_import.* TO 'sheet'@'%';
```

접속 정보는 환경변수로 바꿀 수 있습니다: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` (기본값 `localhost:3306/sheet_import`, `sheet/sheet`).

### 4-2. 앱 실행

```bash
./gradlew bootRun
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- 테이블은 첫 기동 때 Flyway가 만듭니다.
- Docker만 있고 MySQL은 따로 안 띄우고 싶다면 `./gradlew bootTestRun` (Testcontainers로 MySQL 8.4를 띄워서 실행).

JDK 25가 PC에 없어도 됩니다. `build.gradle`의 toolchain 설정과 `settings.gradle`의 foojay 플러그인이 필요한 JDK를 찾아 쓰거나 내려받습니다.

### 4-3. 엑셀로 바로 해보기

`samples/products-sample.xlsx`를 Swagger의 `POST /api/products/imports/excel`에 올려 보세요.
6행 중 5행이 저장되고, 6행(가격 비어 있음)은 `errors`에 이유와 함께 돌아옵니다. 같은 파일을 다시 올리면 전부 `unchangedCount`로 잡힙니다.

## 5. Google Sheets 연동 설정

엑셀 업로드만 쓸 거면 건너뛰어도 됩니다. 설정이 없으면 Google 엔드포인트만 `503 GOOGLE_SHEETS_NOT_CONFIGURED`를 돌려주고 나머지는 정상 동작합니다.

1. [Google Cloud Console](https://console.cloud.google.com/)에서 프로젝트를 만들고 **Google Sheets API**를 "사용 설정"합니다.
2. **IAM 및 관리자 → 서비스 계정**에서 서비스 계정을 만들고, **키 → 새 키 만들기 → JSON**으로 키 파일을 받습니다.
3. 키 파일을 `secrets/service-account.json`에 둡니다. (`secrets/`는 `.gitignore`에 들어 있습니다. **절대 커밋하지 마세요.**)
4. 읽을 스프레드시트를 열고 **공유 → 서비스 계정 이메일 추가(뷰어)**. 이메일은 키 파일의 `client_email`이고, 앱 기동 로그에도 찍힙니다.
5. 키 위치를 지정하고 실행합니다.

```bash
GOOGLE_SHEETS_CREDENTIALS=file:./secrets/service-account.json ./gradlew bootRun
```

자주 보는 오류:

| 응답 code | 원인 |
|---|---|
| `GOOGLE_SHEETS_ACCESS_DENIED` (403) | 시트를 서비스 계정에 공유하지 않았거나, Sheets API를 사용 설정하지 않음 |
| `GOOGLE_SHEETS_NOT_FOUND` (404) | 스프레드시트 ID가 틀림 |
| `GOOGLE_SHEETS_BAD_REQUEST` (400) | 범위 표기가 틀림 (예: 없는 시트 이름) |

## 6. API

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/products/imports/excel` | multipart `file` (+ 선택 `sheetName`) |
| POST | `/api/products/imports/google-sheets` | JSON `{"spreadsheetId": "ID 또는 URL", "range": "상품목록!A1:E"}` |
| GET | `/api/import-jobs` | 가져오기 이력 목록 (최신순, `page`·`size`) |
| GET | `/api/import-jobs/{id}` | 이력 상세 (실패 행 포함) |
| GET | `/api/products` | 상품 목록 (`page`·`size`·`sort`) |

```bash
curl -F "file=@samples/products-sample.xlsx" http://localhost:8080/api/products/imports/excel
```

```bash
curl -H "Content-Type: application/json" -d '{"spreadsheetId":"https://docs.google.com/spreadsheets/d/<ID>/edit","range":"상품목록!A1:E"}' http://localhost:8080/api/products/imports/google-sheets
```

응답 예:

```json
{
  "id": 1,
  "sourceType": "EXCEL",
  "sourceRef": "products-sample.xlsx",
  "status": "PARTIAL_SUCCESS",
  "totalRows": 6,
  "createdCount": 5,
  "updatedCount": 0,
  "unchangedCount": 0,
  "failedCount": 1,
  "createdAt": "2026-10-25T14:00:00",
  "errors": [ { "rowNumber": 6, "message": "[가격] 값이 비어 있습니다" } ]
}
```

## 7. 시트 양식

첫 행이 헤더, 그 아래가 데이터입니다. 컬럼 순서는 상관없고 다른 컬럼이 더 있어도 무시합니다.

| 헤더 | 필수 | 규칙 |
|---|---|---|
| 상품코드 | ✔ | 50자 이하, 시트 안에서 중복 불가, 대소문자 구분 |
| 상품명 | ✔ | 200자 이하 |
| 카테고리 | | 100자 이하 |
| 가격 | ✔ | 0 이상, 소수 둘째 자리까지. `1,000` `₩1,000` `1000원` 모두 허용 |
| 재고 | | 0 이상 정수, 비우면 0 |

## 8. 내 도메인으로 바꾸려면

`spreadsheet`, `importjob`, `global` 패키지는 그대로 두고 `product` 패키지만 바꾸면 됩니다.

1. `db/migration/V2__....sql` — 새 테이블 추가 (이미 적용된 V1 파일은 수정하지 말고 새 버전으로)
2. 엔티티 + Repository
3. `ProductColumn` 같은 헤더 정의 enum
4. `ProductRowMapper` 같은 행 검증·변환
5. `ProductImportWriter` 같은 upsert (자연키로 기존 행 조회)
6. 컨트롤러

## 9. 테스트

```bash
./gradlew test
```

| 테스트 | 검증 내용 | Docker |
|---|---|---|
| `ExcelSheetReaderTest` | 헤더/빈 행/수식/서식/시트 선택/잘못된 파일 | 불필요 |
| `GoogleSheetReaderTest` | MockHttpTransport로 응답 변환, 403/404/400 매핑 | 불필요 |
| `ProductRowMapperTest` | 숫자 파싱, 필수 컬럼, 행 단위 오류 수집, 중복 | 불필요 |
| `ProductImportControllerTest` | 201+Location, ProblemDetail 오류 응답 | 불필요 |
| `ProductImportIntegrationTest` | MySQL 8.4 + Flyway + 재업로드 시 upsert | **필요** (없으면 skip) |

## 10. 알려진 한계 / 다음 단계

- **큰 파일** — POI가 파일 전체를 메모리에 올립니다. 수만 행 이상이면 SAX 스트리밍 방식으로 바꾸는 게 좋습니다. 업로드는 10MB로 제한해 두었습니다.
- **삭제 동기화 없음** — 시트에서 지운 행은 DB에서 지워지지 않습니다. 필요하면 "이번 가져오기에 없던 상품코드"를 비활성화하는 로직을 추가합니다.
- **동기 처리** — 요청 안에서 끝까지 처리합니다. 오래 걸리면 `@Async` + 이력 상태(RUNNING) 조회 방식으로 바꿀 수 있습니다.
- **정기 동기화** — `@Scheduled`로 특정 시트를 주기적으로 가져오게 확장할 수 있습니다.
- **인증 없음** — API에 인증/인가가 없습니다. 외부에 노출하기 전에 Spring Security를 붙이세요.
