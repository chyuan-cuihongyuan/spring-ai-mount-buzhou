-- V2（MySQL）：buzhou_message 增补 reasoning_signature 列——演示旧库增量升级路径。
-- 场景：迁移机制上线前的旧库由老版建表语句建成（无该列），基线判定采纳 V1 后由本脚本补列。
-- MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，经 information_schema.COLUMNS 判定 +
-- PREPARE/EXECUTE 按需执行——新版基线已含该列时为无害 no-op（重跑安全）。
SET @buzhou_column_count = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'buzhou_message' AND COLUMN_NAME = 'reasoning_signature');
SET @buzhou_column_ddl = IF(@buzhou_column_count = 0,
    'ALTER TABLE buzhou_message ADD COLUMN reasoning_signature VARCHAR(512)',
    'SELECT 1');
PREPARE buzhou_column_stmt FROM @buzhou_column_ddl;
EXECUTE buzhou_column_stmt;
DEALLOCATE PREPARE buzhou_column_stmt;
