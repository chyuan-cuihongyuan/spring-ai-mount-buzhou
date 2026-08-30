---
Type: task
Status: closed
---
## Question

相同输入窗口重复计数超阈值 block；窗口复位；LRU 有界。

## Resolution

done（2026-08-30）：impl-297；InputFloodGuardHook（SHA-256(strip) 键 +
TTL 滚动窗 + per-session LRU 1024 + blocked 计数 + Clock 注入）。
