# 1154 — MCP 连接最大寿命

**What to build:** 注册表 opt-in 到寿退役——LifetimePolicy + createdAt + retireExpiredOnce（在飞推迟），
Builder/yml `max-lifetime` 装配 + 观测读数。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] DefaultMcpClientRegistry：LifetimePolicy record + Entry.createdAt + retireExpiredOnce/safeRetire + 调度接线
- [x] 观测：retiredCount / deferredRetireCount + buzhou.mcp.lifetime.retired 指标
- [x] McpModule：Builder.maxLifetime + yml `max-lifetime` 解析
- [x] 测试：McpLifetimeRetireTest 四断言全绿

## Done

验证：`mvn -pl buzhou-mcp test` 全绿（McpRealProtocolTest 无 Docker 按设计 skip 语义不变）。
