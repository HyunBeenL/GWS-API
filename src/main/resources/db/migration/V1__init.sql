-- 상품: 시트의 '상품코드'(sku)를 기준으로 upsert 한다.
CREATE TABLE product
(
    id         BIGINT         NOT NULL AUTO_INCREMENT,
    -- 기본 콜레이션(utf8mb4_0900_ai_ci)은 대소문자를 구분하지 않아 'abc-1'과 'ABC-1'이 같은 값이 된다.
    -- 상품코드는 정확히 일치해야 하므로 바이너리 콜레이션을 쓴다.
    sku        VARCHAR(50) COLLATE utf8mb4_bin NOT NULL,
    name       VARCHAR(200)   NOT NULL,
    category   VARCHAR(100)   NULL,
    price      DECIMAL(15, 2) NOT NULL,
    stock      INT            NOT NULL,
    created_at DATETIME(6)    NOT NULL,
    updated_at DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_product_sku UNIQUE (sku)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- 가져오기 1회 = 1행. 어디서 몇 건을 읽어 몇 건이 반영됐는지 남긴다.
CREATE TABLE import_job
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    source_type     VARCHAR(20)  NOT NULL,
    source_ref      VARCHAR(500) NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    total_rows      INT          NOT NULL,
    created_count   INT          NOT NULL,
    updated_count   INT          NOT NULL,
    unchanged_count INT          NOT NULL,
    failed_count    INT          NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- 검증에 실패한 행과 이유. ROW_NUMBER는 MySQL 8 예약어라 컬럼명을 row_no로 둔다.
CREATE TABLE import_job_error
(
    import_job_id BIGINT       NOT NULL,
    row_no        INT          NOT NULL,
    message       VARCHAR(500) NOT NULL,
    CONSTRAINT fk_import_job_error_job FOREIGN KEY (import_job_id) REFERENCES import_job (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
