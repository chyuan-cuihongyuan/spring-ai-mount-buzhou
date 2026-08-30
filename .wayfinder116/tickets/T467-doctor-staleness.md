---
Type: task
Status: closed
---
## Question

体检健康段陈旧度：TTL 过期转 UNKNOWN(stale) + reexamine 手动刷新。

## Resolution

done（2026-08-30）：impl-283；ConfigDoctorHealth 双构造（TTL 可选默认无 =
零变化）+ Examined 事实对 + stale 详情三键 + 红队 3 例 + 既有 2 例回归。
