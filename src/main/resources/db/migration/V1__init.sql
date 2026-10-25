-- 서책 리스트
CREATE TABLE tb_textbook
(
    textbook_id      BIGINT       NOT NULL AUTO_INCREMENT,
    code             VARCHAR(100) NULL,
    name             VARCHAR(200) NULL,
    publisher        VARCHAR(200) NULL,
    author           VARCHAR(200) NULL,
    curriculum       VARCHAR(50)  NULL,
    school_level     VARCHAR(2)   NULL,
    grade            VARCHAR(2)   NULL,
    subject          VARCHAR(30)  NULL,
    term             VARCHAR(20)  NULL,
    use_yn           VARCHAR(1)   NULL,
    show_depth       VARCHAR(100) NULL,
    view_textbook_yn VARCHAR(1)   NULL,
    view_special_yn  VARCHAR(1)   NULL,
    view_resource_yn VARCHAR(1)   NULL,
    creator_id       BIGINT       NULL,
    create_date      DATETIME     NULL,
    updater_id       BIGINT       NULL,
    update_date      DATETIME     NULL,
    deleted_at       DATETIME     NULL,
    PRIMARY KEY (textbook_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '서책 리스트';

-- 서책 목차: 서책 하나에 딸린 트리. parent_id가 NULL이면 최상위 노드다.
CREATE TABLE tb_textbook_map_node
(
    tb_textbook_map_node_id BIGINT       NOT NULL AUTO_INCREMENT,
    parent_id               BIGINT       NULL,
    code                    VARCHAR(30)  NULL,
    name                    VARCHAR(100) NULL,
    depth                   INT          NULL,
    sort_order              INT          NULL,
    leaf_yn                 VARCHAR(1)   NULL,
    use_yn                  VARCHAR(1)   NULL,
    creator_id              BIGINT       NULL,
    create_date             DATETIME     NULL,
    updater_id              BIGINT       NULL,
    update_date             DATETIME     NULL,
    deleted_at              DATETIME     NULL,
    textbook_id             BIGINT       NOT NULL,
    PRIMARY KEY (tb_textbook_map_node_id),
    CONSTRAINT fk_textbook_map_node_parent FOREIGN KEY (parent_id) REFERENCES tb_textbook_map_node (tb_textbook_map_node_id),
    CONSTRAINT fk_textbook_map_node_textbook FOREIGN KEY (textbook_id) REFERENCES tb_textbook (textbook_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '서책 목차';

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
