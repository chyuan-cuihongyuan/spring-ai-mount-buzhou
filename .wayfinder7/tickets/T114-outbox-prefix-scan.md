---
Type: task
Status: open
---
## Question

outbox 到期扫描性能注记（spec 24 fog）：每 200ms 全量 getAll 合成会话，故障积压万条级时全量读放大。SessionStateStore 增前缀扫描接口是否值得？

## Resolution

AFK 自决：值得但轻量。SessionStateStore 增 `default Map<String,StateEntry> scanByPrefix(String sessionId, String prefix) { 过滤 getAll }`（内存默认即正确）；JDBC 实现 `WHERE session_id=? AND state_key LIKE prefix||'%'`（走键索引）；Redis 实现 SCAN 匹配（Lua/keys 模式按现有键布局）；WebhookOutbox.due/deadLetters/pendingCount 全部改走 scanByPrefix。契约：三实现行为等价（scanByPrefix ⊆ getAll 按 prefix 过滤）。产 spec 33 §C + impl-89。
