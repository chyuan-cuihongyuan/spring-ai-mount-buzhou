# 722 — MCP 每连接并发占用视图

> 来源：G 会话第 23 轮 = effort #722（610 并发闸的读数面）/ [T1044](../../.wayfinder/tickets/T1044-mcp-concurrency-views.md) / [T1045](../../.wayfinder/tickets/T1045-mcp-concurrency-views-verify.md) / impl 622。

## Problem

610 给每连接装了 Semaphore 闸——但闸是黑盒：limit 多少、还剩几个许可、在途几个调用，全部不可见。排障「MCP server 假死是不是卡在我们的并发闸上」只能 jstack；容量规划（该调大 limit 吗）没有占用数据。

## Solution

- `McpConcurrencyView`（mcp，公共 record）：`(server, limit, available, inFlight)`——limit=-1 哨兵=未设上限（available 同 -1）。
- `McpClientRegistry.concurrencyViews()`：server → view；接口 default 空（实现未支持零面）；DefaultMcpClientRegistry 覆写——Entry 存 limit 原值 + `Semaphore.availablePermits()` + 既有 inFlight 计数。

## Testing Decisions

- 真 registry + gated 工具：空闲 available=limit；工具在飞时 available=limit-1、inFlight=1；释放后回落。
- 接口 default 空视图 + UNSET 哨兵断言。

## Out of Scope

- 阻塞队列长度（JDK Semaphore 不暴露——改动信号量实现不值得）。
- 占用历史/时间序列（快照口径——OLAP 下游）。
