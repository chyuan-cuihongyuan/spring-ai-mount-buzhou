# 814 — MCP 断路器变迁台账

> 来源：H 会话第 15 轮 = effort #814 / [T1129](../../.wayfinder/tickets/T1129-mcp-breaker-journal.md) / [T1130](../../.wayfinder/tickets/T1130-mcp-breaker-journal-verify.md) / impl 567。
> 借鉴：Resilience4j 事件流（702 同模式扩散）。

## Problem

MCP server 聚合熔断只有现态快照（504 snapshot）：哪台 server 最近反复跳闸、平均多久恢复——变迁史缺位（模型域 702 有、server 域没有）。

## Solution

`McpBreakerTransitionJournal`（mcp.breaker）+ McpServerBreaker 可选挂接：

- **检测**：decorate 包装三路径（成功/失败/拒收）后 stateOf 与 lastStates 差分，状态变化入账（同态忽略）。
- **台账**：环形明细 64（server/from/to/atMs，挤最老计 dropped）+ per-server 聚合 32（trips=→OPEN、recovers=→CLOSED、transitions 总计）。
- **挂接**：2 参构造（config, journal），null = 原行为（1 参构造委托）。

## 兼容性

McpServerBreaker 仅加构造器+旁路差分调用——断路器语义零变更（504 既有测试回归绿）。

## 诚实边界

采样型差分：HALF_OPEN 瞬时态不可见（cooldown 期满到探测完成之间的中间态）；System.currentTimeMillis 记时；只读无清零。
