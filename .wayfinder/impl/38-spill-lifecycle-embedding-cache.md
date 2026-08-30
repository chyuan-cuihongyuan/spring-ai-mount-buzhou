# 38 — memory+spill · 增长治理 + embedding 缓存 + 后台任务治理

**What to build:** 磁盘与成本有界：spill TTL 清理由 sweeper 调度、总量/单会话配额（超限拒绝并提示模型分页）、启动孤儿扫描；embedding 结果缓存（embed once，三消费方共用）；EpisodeLedger 序号持久恢复；SleepTime 整理器队列上限/退避/摘除/排空。

**Blocked by:** 37（sweeper 调度）、29（分类）

**Status:** ready-for-agent

- [ ] spill deleteExpired 接入 sweeper；maxTotalBytes/maxFilesPerSession 配额（超限拒绝落盘+回喂提示）
- [ ] 启动孤儿扫描（引用会话不存在的文件：报告 + 清理，幂等）
- [ ] CachedEmbeddingProvider（内容 hash 键、LRU 默认 512）装饰 RecallSearch/EpisodeLedger/SemanticChunkIndex
- [ ] EpisodeLedger 序号从持久状态恢复（重启不归零不覆盖）
- [ ] SleepTimeScheduler：pending 上限 64/会话（超限丢弃计数）、perSession 会话结束摘除、close 接进生命周期、失败指数退避 cap 60s
- [ ] examples/单测：配额拒绝行为、孤儿扫描幂等、同内容二次检索不重复 embed、重启后 episode 序号延续
