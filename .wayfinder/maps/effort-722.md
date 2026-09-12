# effort #722 — MCP 每连接并发占用视图

- 会话：G 会话 700 系第 23 轮 ｜ spec [722](../../../docs/spec/722-mcp-concurrency-views.md) ｜ 票 [T1044](../tickets/T1044-mcp-concurrency-views.md)/[T1045](../tickets/T1045-mcp-concurrency-views-verify.md) ｜ impl622
- 借鉴：etcd/线程池监控惯例——闸门必须有占用读数

## 勘察（排重）

- 610 并发闸（Semaphore）黑盒：limit/剩余许可/在途不可见——排障「server 假死是否卡闸」只能 jstack。
- grep ConcurrencyView：零命中。

## 决定

`McpConcurrencyView(server,limit,available,inFlight)` 公共 record（limit=-1 哨兵=未设）+`McpClientRegistry.concurrencyViews()` default 空（实现未支持零面）+DefaultMcpClientRegistry 覆写（Entry 存 limit 原值+availablePermits+既有 inFlight）。

## 测试

真 registry+gated 工具：空闲/在飞/释放三态占用精确；接口 default 空视图+哨兵。

## 诚实边界

队列长度不做（JDK Semaphore 不暴露）；快照口径（历史/时序归 OLAP 下游）。
