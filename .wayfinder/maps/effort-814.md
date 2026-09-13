# effort #814 — MCP 断路器变迁台账

- 会话：H 会话 800 系第 15 轮 ｜ spec [814](../../../docs/spec/814-mcp-breaker-journal.md) ｜ 票 [T1129](../tickets/T1129-mcp-breaker-journal.md)/[T1130](../tickets/T1130-mcp-breaker-journal-verify.md) ｜ impl567
- 借鉴：Resilience4j EventConsumer（resilience4j/resilience4j ≈9.7K，resilience4j 事件流模式扩散轮——702 同模式推广）

## 勘察（排重）

- McpServerBreaker（504）：snapshot() 现态面——无变迁史。
- CircuitTransitionJournal（702）：模型域——MCP server 域缺位（本轮即扩散补位）。
- grep -i `mcp.*journal|server.*transition`：无命中。

## 决定

`McpBreakerTransitionJournal`（mcp.breaker，702 同模式）+ McpServerBreaker 可选 2 参构造（null=原行为）：decorate 包装内 recordSuccess/recordFailure/拒收三路径后 journalIfChanged——stateOf 现态与 lastStates 差分，变_names 即入账（同态忽略）。台账：环形 64（挤最老计 dropped）+per-server 聚合 32（trips/recovers/transitions，封顶明细仍记）+快照（recent 新→旧、byServer transitions 降序）。

## 测试

全生命周期 CLOSED→OPEN→(瞬时半开不被采样)→CLOSED：trips=1 recovers=1 transitions=2+OPEN 快速失败不重复入账/68 轮×2 条挤 72 dropped 精确+同态忽略/聚合封顶 32+明细 67 全记+recent 顶端最新/null journal 原语义（504 既有 4 例回归绿）——4 例绿。

## 诚实边界

检测点在成败记录后——HALF_OPEN 瞬时态不被采样（采样型差分的固有口径，702 的 transition() 直喂模式是另一种实现）；System.currentTimeMillis 记时（装饰器内零开销取舍）；只读无清零。
