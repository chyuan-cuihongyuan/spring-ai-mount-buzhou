-- V3（T109 / spec 30）：会话索引表——枚举/过滤查询面（最终一致，权威数据在五 store）。
CREATE TABLE IF NOT EXISTS buzhou_session_index (
    session_id         VARCHAR(128)  PRIMARY KEY,
    app_id             VARCHAR(256),
    agent_name         VARCHAR(256),
    status             VARCHAR(16)   NOT NULL,
    created_at_ms      BIGINT        NOT NULL,
    last_active_at_ms  BIGINT        NOT NULL,
    turn_count         INT           NOT NULL DEFAULT 0,
    tags               VARCHAR(2048)
);
-- MySQL 8 无 CREATE INDEX IF NOT EXISTS：information_schema 探测 + PREPARE/EXECUTE 幂等（V1 同法）
SET @buzhou_index_count = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'buzhou_session_index' AND INDEX_NAME = 'idx_session_index_active');
SET @buzhou_index_ddl = IF(@buzhou_index_count = 0,
    'CREATE INDEX idx_session_index_active ON buzhou_session_index (last_active_at_ms DESC)',
    'SELECT 1');
PREPARE buzhou_index_stmt FROM @buzhou_index_ddl;
EXECUTE buzhou_index_stmt;
DEALLOCATE PREPARE buzhou_index_stmt;
