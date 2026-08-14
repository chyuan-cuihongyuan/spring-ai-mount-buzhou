# 35 — 横切 · deleteSession 级联清理 + SessionCleaner

**What to build:** 删除会话 = 一次调用清干净：messages/summaries/state/lease/spans/events/snapshots/tool_call_log/run_registry + spill 文件 + embedding 缓存全部级联移除，InMemory/JDBC/Redis 三实现语义一致（契约测试背书）。

**Blocked by:** 31（store SPI 面先行稳定）

**Status:** ready-for-agent

- [ ] 持久化 SPI 增 deleteSession(sessionId)（default no-op）
- [ ] 各 store 实现（JDBC 事务内批量删 / Redis 按会话键集删 / InMemory 移除）
- [ ] spill 文件按会话清理接入；core SessionCleaner 协调器（一次级联，失败聚合报告）
- [ ] AgentSession close/显式删除路径接 SessionCleaner
- [ ] 契约测试：deleteSession 后全 store 无残留（新增 AbstractBuzhouStoresContractTest 契约）
