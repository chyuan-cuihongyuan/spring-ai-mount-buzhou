---
Type: task
Status: closed
---
## Question

type+规范化 payload 指纹环形抑制发射侧重复。

## Resolution

done（2026-08-30）：impl-315；EventDeduplicator（键排序 JSON 规范化——Map 序
不定等价同指纹；环形默认 1024；deduped 计数）。
