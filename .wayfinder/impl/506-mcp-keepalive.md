# 506 — MCP keepalive 空闲探活

**What to build:** DefaultMcpClientRegistry opt-in keepaliveInterval——周期对 ACTIVE 连接 listToolNames() 探活，失败（WARN + 计数）走 spec-changed 同口径重建（draining + addEntry），成功计数；未配置零变化。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] keepaliveInterval 构造参数（null/零 = 关）+ scheduleWithFixedDelay
- [x] probeOnce（ACTIVE 全量 listToolNames + ok/failed 计数 + tag server）
- [x] 失败重建（refreshLock 内 markDraining + addEntry，竞态守卫）
- [x] probeSuccessCount/probeFailureCount getter
- [x] 五组用例（真探活/重建/不误伤/零回归/竞态守卫）
- [x] spec 703 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-mcp -am test` 绿。commit 见本轮 `feat(mcp)` 提交。
