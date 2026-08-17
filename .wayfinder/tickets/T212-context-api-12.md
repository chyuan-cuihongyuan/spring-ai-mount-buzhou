---
Type: task
Status: closed
blocked-by: T203-cache-advisor-key.md, T204-cache-write.md, T205-cache-stream.md, T206-cache-ttl.md, T207-cache-metrics.md
---
## Question

术语节（精确响应缓存/终态边界/惰性过期）+ effort#12 公共面 + 新键元数据入档 + 绑定验证。

## Resolution

impl-177 落地：术语节 4 条 + api-surface 节 + metadata 3 键（impl-172 随批）+ 绑定验证。
破坏性变更 1 处入档（canonical 构造组件数）。T212 关闭。
