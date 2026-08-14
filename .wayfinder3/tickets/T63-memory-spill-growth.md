---
id: T63
title: memory+spill · 增长治理与 embedding 成本护栏
type: grilling
status: closed
assignee: ""
blocked-by: [T55]
created: 2026-08-14
---

## Question

进程内与磁盘的增长如何封顶？需裁决：① InMemory 各 store 有界化（容量上限 + 逐出策略——SessionQuota 语义：事实台账 NOEVICTION 超额拒绝、可再生集合 VOLATILE_LRU）；② spill 生命周期（deleteExpired 调度接线 + maxTotalBytes/maxFilesPerSession 配额 + 启动孤儿扫描 + linked 文件治理）；③ embedding 缓存（向量落盘、embed once——RecallSearch/EpisodeLedger/SemanticChunkIndex 三处去重）；④ EpisodeLedger sequence 持久化（重启不归零）；⑤ SleepTimeScheduler 队列上限/perSession 摘除/close 接线/失败退避。

## Resolution

**ratify（用户常设授权，可推翻）→ spec 13 §growth-8**：InMemory 有界化（max-sessions 默认 1000 / per-session 消息 5000）；事实台账超额抛 QuotaExceededException（noeviction）、可再生集合采样近似逐出（volatile 族语义）；RunRegistry COMPLETED 窗口 PT24H；spill deleteExpired 接入 sweeper（PT1H）+ maxTotalBytes/maxFilesPerSession 配额 + 启动孤儿扫描；CachedEmbeddingProvider（内容 hash 键 LRU 512，三消费方共用）；EpisodeLedger 序号持久恢复；SleepTimeScheduler 队列上限 64/会话、perSession 摘除、close 接线、失败指数退避 cap 60s。
