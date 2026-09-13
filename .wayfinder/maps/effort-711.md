# effort #711 — 消息序列连续性审计

- 会话：G 会话 700 系第 12 轮 ｜ spec [711](../../../docs/spec/711-turn-sequence-audit.md) ｜ 票 [T1022](../tickets/T1022-turn-seq-audit.md)/[T1023](../tickets/T1023-turn-seq-audit-verify.md) ｜ impl611
- 借鉴：Apache Kafka（≈30K star）offset/high-watermark 审计——序列连续性是存储正确性的第一证据

## 勘察（排重）

- StoreFsck（341）查孤儿摘要/残留状态/泄漏租约/悬挂观测——**不查消息序列连续性**；消息层（turnSeq,seqInTurn）跨 store 三实现（memory/redis/jdbc）落库，序列断裂=store 级丢数据/重复写入。
- grep TurnSequence/sequence audit：零命中。

## 决定

`TurnSequenceAudit`（core.cleanup 纯函数）：audit(List<Marker>)——Marker(turnSeq,seqInTurn) 由调用方从任意 store 的消息投影；单遍扫描判定三类 finding：GAP（turn 内 seq 断号/turn 缺号）、DUPLICATE（同序对重复）、OUT_OF_ORDER（序对乱序到达）；空表=零发现；null fail-fast。纯原语不接 store（宿主对 dump/读出结果随手跑——接线归 housekeeper 后续轮）。

## 测试

健康序列零发现/turn 内断号/重复序对/乱序/空表与 null。

## 诚实边界

起始约定=（0,0）连续（store 语义）；跨 store 语义差异由调用方投影时归一；不修复只报告。
