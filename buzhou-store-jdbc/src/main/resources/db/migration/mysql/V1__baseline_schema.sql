-- V1 基线（MySQL）：全量 schema（原 db/schema-mysql.sql 迁入版本化轨道）。
-- 索引幂等化：MySQL 8 不支持 CREATE INDEX IF NOT EXISTS，故每条索引 DDL 先查
-- information_schema.STATISTICS 再经 PREPARE/EXECUTE 按需执行——脚本重跑安全
-- （版本机制保证只跑一次 + 基线判定兜底，此处再叠加自身幂等以覆盖「脚本中途失败后重试」）。
CREATE TABLE IF NOT EXISTS buzhou_message (
    id                VARCHAR(64)  PRIMARY KEY,
    session_id        VARCHAR(128) NOT NULL,
    turn_seq          INT          NOT NULL,
    seq_in_turn       INT          NOT NULL,
    role              VARCHAR(16)  NOT NULL,
    content           LONGTEXT,
    tool_calls        LONGTEXT,
    tool_call_id      VARCHAR(64),
    reasoning_content LONGTEXT,
    reasoning_signature VARCHAR(512),
    metadata          LONGTEXT,
    created_at        TIMESTAMP    NOT NULL
);
SET @buzhou_index_count = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'buzhou_message' AND INDEX_NAME = 'idx_msg_session_order');
SET @buzhou_index_ddl = IF(@buzhou_index_count = 0,
    'CREATE UNIQUE INDEX idx_msg_session_order ON buzhou_message (session_id, turn_seq, seq_in_turn)',
    'SELECT 1');
PREPARE buzhou_index_stmt FROM @buzhou_index_ddl;
EXECUTE buzhou_index_stmt;
DEALLOCATE PREPARE buzhou_index_stmt;

CREATE TABLE IF NOT EXISTS buzhou_summary (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id        VARCHAR(128) NOT NULL,
    version           BIGINT       NOT NULL,
    sections          LONGTEXT     NOT NULL,
    token_estimate    INT          NOT NULL,
    created_at        TIMESTAMP    NOT NULL
);
SET @buzhou_index_count = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'buzhou_summary' AND INDEX_NAME = 'idx_summary_session_version');
SET @buzhou_index_ddl = IF(@buzhou_index_count = 0,
    'CREATE UNIQUE INDEX idx_summary_session_version ON buzhou_summary (session_id, version)',
    'SELECT 1');
PREPARE buzhou_index_stmt FROM @buzhou_index_ddl;
EXECUTE buzhou_index_stmt;
DEALLOCATE PREPARE buzhou_index_stmt;

CREATE TABLE IF NOT EXISTS buzhou_session_state (
    session_id        VARCHAR(128) NOT NULL,
    state_key         VARCHAR(256) NOT NULL,
    state_value       LONGTEXT,
    producer          VARCHAR(128) NOT NULL,
    created_turn      INT          NOT NULL,
    ttl_turns         INT,
    updated_at        TIMESTAMP    NOT NULL,
    PRIMARY KEY (session_id, state_key)
);

CREATE TABLE IF NOT EXISTS buzhou_session_lease (
    session_id        VARCHAR(128) PRIMARY KEY,
    owner_id          VARCHAR(128) NOT NULL,
    fencing_token     BIGINT       NOT NULL,
    acquired_at       TIMESTAMP    NOT NULL,
    expires_at        TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS buzhou_span (
    span_id           VARCHAR(64)  PRIMARY KEY,
    session_id        VARCHAR(128) NOT NULL,
    turn_seq          INT,
    parent_id         VARCHAR(64),
    kind              VARCHAR(32)  NOT NULL,
    name              VARCHAR(256) NOT NULL,
    started_at        TIMESTAMP    NOT NULL,
    ended_at          TIMESTAMP,
    status            VARCHAR(16)  NOT NULL,
    attributes        LONGTEXT
);
SET @buzhou_index_count = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'buzhou_span' AND INDEX_NAME = 'idx_span_session');
SET @buzhou_index_ddl = IF(@buzhou_index_count = 0,
    'CREATE INDEX idx_span_session ON buzhou_span (session_id, turn_seq)',
    'SELECT 1');
PREPARE buzhou_index_stmt FROM @buzhou_index_ddl;
EXECUTE buzhou_index_stmt;
DEALLOCATE PREPARE buzhou_index_stmt;

CREATE TABLE IF NOT EXISTS buzhou_event (
    event_id          VARCHAR(64)  PRIMARY KEY,
    span_id           VARCHAR(64)  NOT NULL,
    session_id        VARCHAR(128) NOT NULL,
    kind              VARCHAR(32)  NOT NULL,
    payload           LONGTEXT,
    created_at        TIMESTAMP    NOT NULL
);
SET @buzhou_index_count = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'buzhou_event' AND INDEX_NAME = 'idx_event_session');
SET @buzhou_index_ddl = IF(@buzhou_index_count = 0,
    'CREATE INDEX idx_event_session ON buzhou_event (session_id, created_at)',
    'SELECT 1');
PREPARE buzhou_index_stmt FROM @buzhou_index_ddl;
EXECUTE buzhou_index_stmt;
DEALLOCATE PREPARE buzhou_index_stmt;

CREATE TABLE IF NOT EXISTS buzhou_injection_snapshot (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id        VARCHAR(128) NOT NULL,
    turn_seq          INT          NOT NULL,
    messages          LONGTEXT     NOT NULL,
    budget_detail     LONGTEXT,
    created_at        TIMESTAMP    NOT NULL
);
SET @buzhou_index_count = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'buzhou_injection_snapshot' AND INDEX_NAME = 'idx_snapshot_session_turn');
SET @buzhou_index_ddl = IF(@buzhou_index_count = 0,
    'CREATE UNIQUE INDEX idx_snapshot_session_turn ON buzhou_injection_snapshot (session_id, turn_seq)',
    'SELECT 1');
PREPARE buzhou_index_stmt FROM @buzhou_index_ddl;
EXECUTE buzhou_index_stmt;
DEALLOCATE PREPARE buzhou_index_stmt;

CREATE TABLE IF NOT EXISTS buzhou_run_registry (
    session_id           VARCHAR(128) PRIMARY KEY,
    app_id               VARCHAR(128) NOT NULL,
    agent_name           VARCHAR(128) NOT NULL,
    status               VARCHAR(16)  NOT NULL,
    current_turn         INT          NOT NULL,
    last_completed_turn  INT          NOT NULL,
    owner_id             VARCHAR(128),
    updated_at           TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS buzhou_tool_call_log (
    session_id   VARCHAR(128) NOT NULL,
    tool_call_id VARCHAR(64)  NOT NULL,
    tool_name    VARCHAR(128) NOT NULL,
    args_hash    VARCHAR(128) NOT NULL,
    outcome      VARCHAR(24)  NOT NULL,
    result       LONGTEXT,
    occurred_at  TIMESTAMP    NOT NULL,
    PRIMARY KEY (session_id, tool_call_id)
);
