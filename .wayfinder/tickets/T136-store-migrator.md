---
Type: task
Status: closed
---
## Question

跨 store 会话迁移（T122 dashboard 之外的数据面）：JDBC→Redis/反向的会话迁移无工具（只有文档级 export/import）。是否补迁移器？

## Resolution

AFK 自决：补轻量工具（非自动服务）。core `SessionMigrator.migrate(AgentRuntime source, AgentRuntime target, String sessionId, boolean keepIds)`：source.exportSession → target.importSession（复用两条成熟管线；keepIds 语义沿用）；返回目标 Id；指标 `buzhou.session.migrations`。examples 演示 JDBC(H2)→内存迁移。产 spec 38 §B + impl-109。
