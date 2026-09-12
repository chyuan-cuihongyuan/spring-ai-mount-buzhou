# Spec 504 — MCP 服务器级聚合熔断（effort #504）

> wayfinder map：`.wayfinder/maps/effort-504.md`（T759–T760）。E 会话第 5 轮。

## Problem Statement

工具面熔断（131）是 per-tool 粒度；MCP server 宕机时其**全部工具**连环
失败——per-tool 各自独立跳闸慢（每工具都要攒满自己的窗），模型在跳闸
前的窗口内反复撞死 server。需要 per-server 聚合断路：一台 server 一个
键，任一工具失败都计入同一状态机。

## Solution

`mcp/breaker/McpServerBreaker` + registry 接线（resilience4j/
Envoy per-host 聚合思想；状态机复用 core `ToolCircuitBreaker`——键=
服务器名，星形依赖内合法复用不造第二状态机）：

- **decorate(serverName, callback)**：包装回调——
  `tryAcquirePermission(server)` 拒绝 → `IllegalStateException` 结构化
  文案（server 名 + OPEN 语义 + 改道指引）经标准工具错误路径回模型，
  不碰网络；放行 → 真调，record 成功/失败（HALF_OPEN 探测名额、一败
  重开语义全继承）。
- **snapshot()**：per-server `ToolCircuitBreaker.View`（state/窗内成败/
  blocked/冷却剩余）——健康面/面板可接。
- **接线**：DefaultMcpClientRegistry 新增 7 参构造（6 参兼容委托，
  internal 自由改）——`toolCallbacksFor` 装配时 decorate（套在
  RefCountingToolCallback 内层，引用计数/生命周期语义不变）；危险模式
  扫描仍用原始 cb 名不受影响。
- **yml**：`buzhou.mcp.server-breaker.{enabled, window-size,
  failure-rate-percent, cooldown, half-open-trials}`——enabled 默认关
  （opt-in，131 同族）；McpModule.Builder `serverBreaker(Config)`。

## User Stories

1. 作为 MCP 宿主，我想让宕机 server 的全部工具快速失败，so 模型立即
   得到「该 server 暂不可用」的结构化信号改道，而非每次都等超时。
2. 作为运维，我想看每台 server 的熔断状态，so 哪台 server 在连败、
   被拦了多少次调用一屏可见。

## Implementation Decisions

- 状态机复用 core ToolCircuitBreaker（不复制 resilience4j 语义）。
- 只记**异常**失败——MCP result 内协议级 isErrorCode 不在 ToolCallback
  面上，不计数（诚实边界）。
- 与 per-tool 131 正交两层：server 级先聚合断路，工具级独立细粒度。

## Testing Decisions

- 装饰器：失败率满窗跳闸 → OPEN 调用快速失败（delegate 未触）→
  Clock 注入冷却耗尽 → HALF_OPEN 探测 → 全成 CLOSED / 一败重开。
- registry 集成：breaker 配置后 toolCallbacksFor 返回包装回调，
  OPEN 时调用快速失败（FakeMcp 假件）。
- yml：server-breaker.enabled=true 装配、缺省不装配（callbacks 不包装）。

## Out of Scope

- OPEN 摘除目录工具；server 健康探针；跨实例共享。

## Further Notes

- 新公共类型 `McpServerBreaker` 随轮 regenerate 快照 + api-surface.md
  加行。
