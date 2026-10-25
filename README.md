# sheet-import-api

Google 스프레드시트나 엑셀 파일(.xlsx/.xls)의 목록을 읽어 MySQL에 저장하는 Spring Boot Web API입니다.
지금은 시트 읽기 계층, Google 스프레드시트를 읽어 돌려주는 API(그대로 / 한글 헤더를 서책 컬럼으로 바꿔서), 가져오기 이력 API, 서책 테이블(`tb_textbook`, `tb_textbook_map_node`), 그리고 **Apps Script가 보낸 시트 값을 `tb_textbook`에 저장하는 API**가 있습니다. 목차(`tb_textbook_map_node`) 저장은 아직 없습니다.

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
| **A1 표기법** | 시트 범위를 적는 방식. `서책목록!A1:E` = "서책목록" 시트의 A~E열 전체, `A:Z` = 첫 번째 시트의 A~Z열. |
| **upsert** | update + insert. 같은 키가 있으면 수정, 없으면 새로 넣습니다. 같은 시트를 여러 번 가져와도 중복이 생기지 않습니다. |
| **Flyway 마이그레이션** | 테이블 구조를 `V1__init.sql`, `V2__...sql`처럼 버전별 SQL 파일로 관리하는 도구. 앱이 뜰 때 아직 적용 안 된 파일만 순서대로 실행하고, 적용 이력은 `flyway_schema_history` 테이블에 남습니다. |
| **ddl-auto: validate** | JPA가 테이블을 직접 만들지 않고 "엔티티와 테이블이 맞는지"만 검사합니다. 테이블 생성은 Flyway 담당. |
| **ProblemDetail** | 오류 응답 표준(RFC 9457). `{"status":400, "detail":"...", "code":"EMPTY_FILE"}` 형태의 `application/problem+json`. |
| **sealed interface** | 구현 클래스를 정해 둔 인터페이스. `switch`에서 모든 경우를 다뤘는지 컴파일러가 검사해 줍니다. |
| **Testcontainers** | 테스트할 때 Docker로 진짜 MySQL을 잠깐 띄웠다 지우는 라이브러리. |

## 2. 동작 흐름

```
 [엑셀 파일]          [Google Sheets]        [Apps Script]
        │                    │                     │ 시트 값을 JSON으로 POST
        ▼                    ▼                     ▼
 ExcelSheetReader     GoogleSheetReader     TextbookImportRequest   ← 읽기: 소스마다 다름
        └────────────────────┼─────────────────────┘
                             ▼
                        SheetTable                    ← 소스와 무관한 "헤더 + 행" 표
                             ▼
              TextbookSheetMapper.toRows              ← 행 검증 (필수·길이·Y/N·코드 중복)
                             ▼
                  TextbookImportWriter                ← tb_textbook upsert (서책코드 기준)
                             ▼
                         ImportJob                    ← 가져오기 이력 + 실패 행
```

지금 저장까지 이어지는 경로는 Apps Script뿐입니다. 엑셀·서버가 직접 읽는 Google Sheets 경로는 미리보기(읽기)만 있습니다.

설계에서 신경 쓴 점:

- **읽기와 저장을 분리** — 소스가 늘어나도(CSV 등) `SheetReader` 구현만 추가하면 되고, 검증·저장 로직은 그대로입니다.
- **값은 "화면에 보이는 문자열"로 통일** — 엑셀은 POI `DataFormatter`, Google은 `FORMATTED_VALUE`. 그래서 `1000`이 `1000.0`으로 읽히거나 수식이 `=A2*B2`로 읽히지 않습니다.

## 3. 패키지 구조

```
com.leehv1234.sheetimport
├── global
│   ├── error        ErrorCode, BusinessException, GlobalExceptionHandler(ProblemDetail)
│   ├── web          PageResponse
│   └── config       OpenApiConfig (Swagger)
├── spreadsheet      도메인과 무관한 "표 읽기" 계층
│   ├── SheetSource (sealed) ─ ExcelFileSource / GoogleSheetSource
│   ├── SheetTable, SheetRow, SheetReader, RowError, MappingResult, SheetTableResponse
│   ├── excel        ExcelSheetReader (Apache POI)
│   └── google       GoogleSheetController, GoogleSheetReader, GoogleSheetsConfig, GoogleSheetsProperties
├── importjob        가져오기 이력 (ImportJob + 실패 행 import_job_error)
└── textbook         서책: 한글 헤더 정의(TextbookColumn), 시트 → tb_textbook 변환·검증, 읽기·저장 API

apps-script/textbook-import.gs   스프레드시트에 붙여 쓰는 Apps Script ("서책" 메뉴 → 서버에 저장)
```

## 4. 테이블

`src/main/resources/db/migration/V1__init.sql`

| 테이블 | 설명 |
|---|---|
| `tb_textbook` | 서책 리스트 |
| `tb_textbook_map_node` | 서책 목차. `parent_id`로 자기 자신을 참조하는 트리(최상위는 `NULL`), `textbook_id`로 서책을 참조 |
| `import_job` | 가져오기 1회의 기록 |
| `import_job_error` | 가져오기 중 검증에 실패한 행과 이유 |

## 5. 실행

### 5-1. MySQL 8.4 준비

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

접속 정보는 환경변수로 바꿀 수 있습니다: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`.

### 5-2. 앱 실행

```bash
./gradlew bootRun
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- 테이블은 첫 기동 때 Flyway가 만듭니다.
- Docker만 있고 MySQL은 따로 안 띄우고 싶다면 `./gradlew bootTestRun` (Testcontainers로 MySQL 8.4를 띄워서 실행).

JDK 25가 PC에 없어도 됩니다. `build.gradle`의 toolchain 설정과 `settings.gradle`의 foojay 플러그인이 필요한 JDK를 찾아 쓰거나 내려받습니다.

## 6. Google Sheets 연동 설정

엑셀만 쓸 거면 건너뛰어도 됩니다. 설정이 없으면 `POST /api/google-sheets/read`만 `503 GOOGLE_SHEETS_NOT_CONFIGURED`를 돌려주고 나머지는 정상 동작합니다.

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

## 7. API

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/google-sheets/read` | JSON `{"spreadsheetId": "ID 또는 URL", "range": "서책목록!A1:N"}`. 읽은 헤더와 행을 그대로 돌려주고, 저장은 하지 않음 |
| POST | `/api/textbooks/google-sheets/read` | 요청은 위와 같음. 한글 헤더를 `tb_textbook` 컬럼 이름으로 바꿔 돌려주고, 저장은 하지 않음 |
| POST | `/api/textbooks/imports/values` | 시트 값을 JSON으로 받아 `tb_textbook`에 저장 (Apps Script용). 201 + `Location: /api/import-jobs/{id}` |
| GET | `/api/import-jobs` | 가져오기 이력 목록 (최신순, `page`·`size`) |
| GET | `/api/import-jobs/{id}` | 이력 상세 (실패 행 포함) |

```bash
curl -H "Content-Type: application/json" -d '{"spreadsheetId":"https://docs.google.com/spreadsheets/d/<ID>/edit","range":"서책목록!A1:N"}' http://localhost:8080/api/google-sheets/read
```

응답 예 (`cells`는 헤더 순서대로, 빈 셀은 빈 문자열):

```json
{
  "headers": ["code", "name", "publisher"],
  "rows": [
    { "rowNumber": 2, "cells": { "code": "T-001", "name": "국어 3-1", "publisher": "천재교육" } }
  ]
}
```

### 서책 시트 양식 (`/api/textbooks/google-sheets/read`)

첫 행이 헤더, 그 아래가 데이터입니다. 컬럼 순서는 상관없고, 아래에 없는 헤더는 무시한 뒤 응답의 `ignoredHeaders`로 알려줍니다. 헤더는 앞뒤 공백만 무시하고 글자가 정확히 같아야 인식합니다.

| 시트 헤더 | tb_textbook 컬럼 | 필수 |
|---|---|---|
| 서책코드 | code | ✔ |
| 서책명 | name | ✔ |
| 출판사 | publisher | |
| 저자 | author | |
| 교육과정 | curriculum | |
| 학교급 | school_level | |
| 학년 | grade | |
| 과목 | subject | |
| 학기 | term | |
| 사용여부 | use_yn | |
| 표시깊이 | show_depth | |
| 교과서보기여부 | view_textbook_yn | |
| 특별자료보기여부 | view_special_yn | |
| 자료보기여부 | view_resource_yn | |

필수 헤더가 없으면 `400 MISSING_COLUMNS`. 읽기 API는 값의 형식을 검사하지 않고, 저장 API(`/api/textbooks/imports/values`)만 검사합니다.

응답 예 (`values`에는 시트에 있는 컬럼만, tb_textbook 컬럼 순서로):

```json
{
  "rows": [
    { "rowNumber": 2, "values": { "code": "TB-E-KOR-3-1", "name": "국어 3-1", "publisher": "천재교육", "author": "홍길동" } }
  ],
  "ignoredHeaders": ["비고"]
}
```

`samples/textbooks-sample.xlsx`가 이 양식의 테스트용 파일입니다. Google Drive에 올리고 **파일 → Google Sheets로 저장**한 뒤, 새로 생긴 시트의 URL로 호출하세요 (`.xlsx` 그대로는 Sheets API가 읽지 못합니다).

### 서책 저장 (`/api/textbooks/imports/values`)

서버가 시트를 읽지 않고, Apps Script가 읽은 값을 그대로 받습니다. 그래서 **서비스 계정 설정 없이** 동작합니다.

```json
{
  "spreadsheetId": "1AbCdEfGhIjKlMnOpQrStUvWxYz",
  "sheetName": "서책목록",
  "values": [
    ["서책코드", "서책명", "출판사", "사용여부"],
    ["TB-E-KOR-3-1", "국어 3-1", "천재교육", "Y"]
  ]
}
```

- `values`는 A1부터 읽은 값입니다. 첫 행이 헤더이고, `values[i]`가 시트의 `i + 1`행입니다(실패 행 번호에 그대로 쓰임).
- **서책코드가 같은 서책이 있으면 수정, 없으면 생성**합니다(삭제된 서책 `deleted_at`은 대상에서 뺌). 값이 같으면 UPDATE 하지 않고 `unchangedCount`로 셉니다.
- 시트에 **헤더가 없는 컬럼은 건드리지 않습니다.** 헤더는 있는데 셀이 비어 있으면 `NULL`로 저장합니다.
- 시트에서 행을 지워도 DB의 서책은 지우지 않습니다.
- 행 검증: 서책코드·서책명이 비어 있음, 컬럼 길이(`tb_textbook`의 VARCHAR 길이) 초과, `*여부` 컬럼이 Y/N이 아님(소문자는 대문자로 바꿔 저장), 시트 안에서 서책코드 중복. 실패한 행만 빼고 나머지는 저장하며, 실패 행과 이유는 응답의 `errors`와 가져오기 이력에 남습니다.

응답은 가져오기 이력과 같은 형식입니다.

```json
{ "id": 3, "sourceType": "GOOGLE_SHEETS", "sourceRef": "1AbC... [서책목록]", "status": "PARTIAL_SUCCESS",
  "totalRows": 10, "createdCount": 7, "updatedCount": 1, "unchangedCount": 1, "failedCount": 1,
  "errors": [ { "rowNumber": 5, "message": "서책명: 값이 비어 있습니다" } ] }
```

### Apps Script 연동

Apps Script는 Google 서버에서 실행되므로 `localhost`에 접근할 수 없습니다. [cloudflared](https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/) quick tunnel로 로컬 서버를 임시 공개 주소에 연결합니다.

1. 서버를 띄우고(`./gradlew bootRun`), 다른 터미널에서 터널을 엽니다.
   ```bash
   cloudflared tunnel --url http://localhost:8080
   ```
   로그에 나오는 `https://….trycloudflare.com` 주소를 복사합니다. **cloudflared를 다시 켤 때마다 주소가 바뀝니다.**
2. 서책 스프레드시트에서 **확장 프로그램 → Apps Script**를 열고, `Code.gs` 내용을 [`apps-script/textbook-import.gs`](apps-script/textbook-import.gs)로 바꿔 저장합니다.
3. 스프레드시트를 새로고침하면 **서책** 메뉴가 생깁니다.
   - **서책 → 서버 주소 설정**: 1번의 주소를 넣습니다(스크립트 속성에 저장됨).
   - **서책 → 서버에 저장**: 지금 보고 있는 시트를 보내고, 생성·수정·실패 건수와 실패한 행을 창으로 보여 줍니다. 처음 한 번은 권한(외부 서비스 연결) 승인 창이 뜹니다.

> ⚠️ 터널 주소를 아는 사람은 누구나 이 API를 호출할 수 있습니다. 쓰지 않을 때는 cloudflared를 끄세요.

## 8. 도메인을 붙이려면

`spreadsheet`, `importjob`, `global` 패키지는 그대로 두고 도메인 패키지를 새로 만듭니다.

1. 테이블 — 이미 적용된 마이그레이션 파일은 수정하지 말고 `V2__....sql`처럼 새 버전으로 추가
2. 엔티티 + Repository
3. 헤더 정의 — 시트 헤더 이름과 필수 여부
4. 행 검증·변환 — `SheetTable`의 각 행을 도메인 값으로 바꾸고, 잘못된 행은 `RowError`로 모아 `MappingResult`로 반환
5. upsert — 시트에는 DB id가 없으므로 자연키로 기존 행을 조회하고, 같은 트랜잭션에서 `ImportJob`을 남김
6. 서비스 + 컨트롤러 — 시트 읽기(파일·네트워크 I/O)는 트랜잭션 밖에서

## 9. 테스트

```bash
./gradlew test
```

| 테스트 | 검증 내용 | Docker |
|---|---|---|
| `ExcelSheetReaderTest` | 헤더/빈 행/수식/서식/시트 선택/잘못된 파일 | 불필요 |
| `GoogleSheetReaderTest` | MockHttpTransport로 응답 변환, 403/404/400 매핑 | 불필요 |
| `GoogleSheetSourceTest` | URL에서 스프레드시트 ID 추출, 기본 범위 | 불필요 |
| `GoogleSheetControllerTest` | 헤더 순서 응답, 요청 검증, ProblemDetail 오류 응답 | 불필요 |
| `TextbookSheetMapperTest` | 한글 헤더 → tb_textbook 컬럼, 무시된 헤더, 필수 헤더 누락, 저장용 행 검증(빈 값·길이·Y/N·코드 중복) | 불필요 |
| `TextbookSheetControllerTest` | 변환된 응답 형식, 필수 헤더 누락 시 400 | 불필요 |
| `TextbookImportControllerTest` | Apps Script 요청 → 표 변환(행 번호, 빈 행, null 셀), 201 + Location, 빈 요청 400 | 불필요 |
| `TextbookImportIntegrationTest` | 실제 MySQL에 저장 → 재전송 시 수정/변경없음 구분, 헤더 없는 컬럼 보존 | **필요** (없으면 건너뜀) |

## 10. 알려진 한계

- **큰 파일** — POI가 파일 전체를 메모리에 올립니다. 수만 행 이상이면 SAX 스트리밍 방식으로 바꾸는 게 좋습니다. 업로드는 10MB로 제한해 두었습니다.
- **인증 없음** — API에 인증/인가가 없습니다. cloudflared 터널로 열어 두면 주소를 아는 누구나 저장 API를 호출할 수 있으니, 오래 열어 둘 거라면 API 키 헤더 검사나 Spring Security를 붙이세요.
- **서책코드 unique 제약 없음** — `tb_textbook.code`에 unique 인덱스가 없어, 같은 시트를 동시에 두 번 보내면 같은 코드가 두 행 생길 수 있습니다. 이미 중복이 있으면 id가 작은 서책을 갱신합니다.
