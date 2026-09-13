# effort #835 — 尾采样决策台账

- 会话：H 会话 800 系第 36 轮 ｜ spec [835](../../../docs/spec/835-tail-sampling-decision-log.md) ｜ 票 [T1171](../tickets/T1171-tail-sampling-decision-log.md)/[T1172](../tickets/T1172-tail-sampling-decision-log-verify.md) ｜ impl588
- 借鉴：OpenTelemetry Collector tail_sampling processor（决策留痕+策略原因）
- **辨义注记**：TurnErrorSampler 是 eval 域错误采样（非观测 trace 域）——不撞，正交。

## 勘察（排重）

- SpanStatusDistribution：状态分布——无采样决策维。
- TurnErrorSampler/TurnSamplerHook：eval 域采样——观测域缺位。
- grep -i `tailsample|tail.?sampling`：无命中。

## 决定

`TailSamplingDecisionLog`（observability）：record(traceId, decision, reason, atMs)——决策二值闭集+原因开集（键封顶 16 超限并入 overflow 桶）；环形明细 64（挤最老计 ringDropped）；keptRatio/byReason 计数降序；脏入参忽略；空真。喂点=采样器装配侧。

## 测试

KEPT/DROPPED 聚合+保留率 0.5+原因降序+recent 新→旧/环挤老 6 条 dropped+顶端最新/原因封顶 16+1 溢出桶带决策维/脏入参四形态+空真——4 例全绿。

## 诚实边界

决策记账不改变采样行为（喂点手动）；原因语义归采样器策略；环形明细挤老后不可溯（聚合仍在）。
