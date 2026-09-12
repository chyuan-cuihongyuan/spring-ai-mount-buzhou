# 703 — MCP keepalive 空闲探活

> 来源：G 会话第 4 轮 = effort #703（借鉴 gRPC keepalive pings / nginx upstream 探测思想）/ [T957](../../.wayfinder/tickets/T957-mcp-keepalive-shape.md) / [T958](../../.wayfinder/tickets/T958-mcp-keepalive-verify.md) / impl 506。
> 选题注记：原列主题「429/503 Retry-After 尊重退避」缺口核查已被 spec 10 覆盖（DefaultErrorClassifier 解析 + ResilienceAdvisor 钳制尊重）——ruled-out 顺延。

## 背景

MCP 长连接空闲期死掉（server 重启、网络断、LB 静默丢弃）要到下一次工具调用才暴露——失败延迟到用户 Turn 内，体验与延迟双输。spec 524 的建连退避只在「建连时」生效；spec 504 聚合熔断只在「调用时」触发。缺的是空闲期的主动活性探测。

## 目标

- `DefaultMcpClientRegistry` 增 opt-in `keepaliveInterval`（null/零 = 关，默认零行为变化）：注册表既有 scheduler 上 `scheduleWithFixedDelay` 周期探活。
- 探活动作：对每条 ACTIVE 连接调 `listToolNames()`——轻量 RPC，与漂移基线（spec 18）同源；伪连接/自定义实现默认空表零开销。
- 成功：`buzhou.mcp.keepalive.ok`（tag server）+ 成功计数；失败：`buzhou.mcp.keepalive.failed` + WARN + **重建该条目**——与 refresh 的 spec-changed 路径同口径（markDraining 引用计数排空 + addEntry 原样重建），refreshLock 内执行，条目已被摘除（refresh 竞态/shutdown）则不动作。
- 探活成功/失败计数 getter（观测/编程面；健康段接线归装配轮）。

## 非目标

不做 gRPC 式 HTTP/2 PING 帧语义（协议层无此 seam）；不做探活失败后的告警规则接线（告警引擎既有面可吃 `keepalive.failed` 指标）。

## 测试

fake 连接 listToolNames 计数（真探活）；探活异常 → failed 计数 + 工厂重建被调 + 新连接 ACTIVE + 旧连接排空；良性邻居不误伤；未配置时调度不启动零回归；已摘除条目不重复动作。

## 兼容性

opt-in 纯增量；默认构造逐字节不变。
