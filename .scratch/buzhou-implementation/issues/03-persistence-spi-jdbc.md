# 03 — 持久化 SPI 契约与 JDBC 实现

**What to build:** 五 SPI（Message/Summary/SessionState/SessionLease/Observability）接口定稿；独立于实现的契约测试套件落地；buzhou-store-jdbc 用 Testcontainers（MySQL/PostgreSQL）全量过契约；unit-of-work 原子提交可用；进程重启后凭 sessionId 完整续接。

**Blocked by:** 01

**Status:** ready-for-agent

- [ ] 契约测试套件对内存与 JDBC 实现同套全绿
- [ ] 「一轮消息+state+摘要」unit-of-work 原子提交/回滚有测试
- [ ] DDL 与 08 spec 一致；观测写入排除在事务外
- [ ] demo：杀掉进程重启，会话历史+state 完整续接
