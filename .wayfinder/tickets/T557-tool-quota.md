---
Type: task
Status: closed
---
## Question

per-tool 会话内调用上限：会话态计数 + 超限 block + 通配默认。

## Resolution

done（2026-08-30）：impl-306；ToolQuotaHook（order 250；列名优先于 "*"；
放行即计被拒不计；会话态生命周期即配额窗）。
