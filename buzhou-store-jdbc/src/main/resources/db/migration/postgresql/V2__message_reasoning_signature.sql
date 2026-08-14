-- V2（PostgreSQL）：buzhou_message 增补 reasoning_signature 列——演示旧库增量升级路径。
-- 场景：迁移机制上线前的旧库由老版建表语句建成（无该列），基线判定采纳 V1 后由本脚本补列；
-- PG 支持 ADD COLUMN IF NOT EXISTS，重跑安全（新版基线已含该列时为无害 no-op）。
ALTER TABLE buzhou_message ADD COLUMN IF NOT EXISTS reasoning_signature VARCHAR(512);
