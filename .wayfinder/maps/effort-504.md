# Wayfinder Map — Buzhou MCP 服务器级聚合熔断（effort #504，E 会话第 5 轮）

> E 会话第 5 轮。勘察：工具面熔断（131 ToolCircuitBreaker，core）是
> **per-tool 名**粒度；MCP server 挂掉时其**全部工具**连环失败——每工具
> 各自独立跳闸慢且模型在跳闸前的窗口内反复撞死 server。per-server 聚合
> 断路（一台 server 一个键）空白。

## Destination

`mcp/breaker/McpServerBreaker`（复用 core `ToolCircuitBreaker` 状态机——
键=服务器名，星形依赖规则内合法复用）：`decorate(serverName, callback)`
包装回调——`tryAcquirePermission` 拒绝 → IllegalStateException 结构化
文案（「熔断 OPEN……改用其他方式或稍后重试」经标准工具错误路径回模型
可改道）不碰网络；放行 → 真调 record 成功/失败。`snapshot()` 出
per-server View 观测面。接线：DefaultMcpClientRegistry（internal 自由
改）新增 7 参构造（6 参兼容委托）——`toolCallbacksFor` 装配时套
decorate（RefCountingToolCallback 内层，引用计数语义不变）。
McpModule.Builder `serverBreaker(Config)` + fromYml `server-breaker` 键
（enabled/window-size/failure-rate-percent/cooldown/half-open-trials，
默认关）。危险模式扫描（registry 493 行）用原始 cb 名不受包装影响。

## Notes

- 号段：spec 504 / T759–T760 / impl-407。
- 借鉴源：resilience4j CircuitBreaker（131 同源）+ Envoy per-host
  outlier/ejection 的聚合粒度思想。
- 诚实边界：只记**异常**失败（MCP result 内 isErrorCode 协议级错误不在
  ToolCallback 面上不计数）；与 per-tool 131 正交两层（server 先断
  tool 未及断也不撞线）。

## Out of scope

- OPEN 时从目录摘除工具（漂移检测域）；server 半开健康探针（165 是
  per-tool 面）；跨实例共享熔断（已否决族）。

## Tickets

- [x] [T759 McpServerBreaker 装饰器](../tickets/T759-mcp-server-breaker.md)
- [x] [T760 registry 接线与 yml](../tickets/T760-mcp-breaker-assembly.md)
