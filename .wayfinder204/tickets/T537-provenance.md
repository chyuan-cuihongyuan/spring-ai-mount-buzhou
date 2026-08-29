---
Type: task
Status: closed
---
## Question

摘要→源消息 lineage：累积折叠 id、折入落账、回查。

## Resolution

done（2026-08-30）：impl-299；SummaryProvenance（LRU 64 会话 × 32 条/会话）
+ SummaryProvenanceListener 实现 CompactionListener（零管线侵入）。
