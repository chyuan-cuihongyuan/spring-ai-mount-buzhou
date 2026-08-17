# 36 — stores · InMemory 有界化 + 容量配额

**What to build:** 内存套件可长跑：各 InMemory store 有容量上限与会话级数据移除；事实台账（消息/摘要/状态）超额明确抛 QuotaExceededException 绝不静默丢；可再生集合（观测流水）容量触发近似逐出。

**Blocked by:** 29（QuotaExceeded 分类）、35（会话移除语义）

**Status:** ready-for-agent

- [ ] buzhou.store.in-memory.max-sessions（默认 1000）+ per-session 消息上限（默认 5000）
- [ ] 事实台账超额抛 QuotaExceededException；观测类 LRU 逐出（采样近似，容量可配）
- [ ] InMemoryUnitOfWork 锁对象随会话移除
- [ ] 上限全部可配 + 启动校验
- [ ] 单测：超额拒绝类型正确、逐出只碰可再生集合、会话关闭后内存回落
