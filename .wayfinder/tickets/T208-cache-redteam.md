---
Type: task
Status: closed
blocked-by: T204-cache-write.md, T206-cache-ttl.md
---
## Question

对抗：键注入（消息含脏内容不串键）/ 工具响应不被缓存 / TTL 过期后不命中陈旧 /
容量压挤 LRU 序正确 / 命中重放不可变。

## Resolution

impl-173 落地四用例：元字符注入不串键 / TTL 过期不陈旧 / 热键压挤存活 / 重放只读语义。
4 绿。T208 关闭。
