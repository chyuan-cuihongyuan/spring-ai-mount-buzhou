# effort #840 — MCP 建连遥测读数

- 会话：H 会话 800 系第 41 轮 ｜ spec [840](../../../docs/spec/840-mcp-connect-telemetry.md) ｜ 票 [T1181](../tickets/T1181-mcp-connect-telemetry.md)/[T1182](../tickets/T1182-mcp-connect-telemetry-verify.md) ｜ impl593
- 借鉴：gRPC channelz 连接遥测（grpc/grpc ≈43K；823 keepalive 借鉴姊妹面）

## 勘察（排重）

- McpConnectionFactory：建连执行——无遥测面。
- McpConcurrencyView（722）：并发占用（建连后）——建连本身缺位。
- 822 能力快照：建连后内容——正交。
- grep -i `connect.*telemetry|dial.*time`：无命中。

## 决定

`McpConnectTelemetry`（mcp，纯读数）：record(server, success, durationMillis)——per-server 成败+连续失败 streak+lastDuration（负值=未知保留旧值）+近窗 16 成功率；server 封顶 32（synchronized servers 双检建态+truncated）；worstFirst 按失败数降序（问题 server 排前）；null/空白忽略。喂点=工厂/注册表装配侧。

## 测试

计数+streak 清零+近窗 0.5+lastDuration/未知时长保留旧值/worstFirst 失败降序/封顶 32+truncated+超封顶 null+脏入参——4 例全绿（ArrayList import 修正）。

## 诚实边界

建连耗时口径由工厂定义（含 retry 则含重试时长）；streak 无自动惩罚；读数不重连。
