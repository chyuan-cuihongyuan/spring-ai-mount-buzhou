# 835 — 尾采样决策台账

> 来源：H 会话第 36 轮 = effort #835 / [T1171](../../.wayfinder/tickets/T1171-tail-sampling-decision-log.md) / [T1172](../../.wayfinder/tickets/T1172-tail-sampling-decision-log-verify.md) / impl 588。
> 借鉴：OTel Collector tail_sampling。
> 辨义注记：TurnErrorSampler 为 eval 域——正交不撞。

## Problem

trace 被采样器丢掉后无决策留痕：「这条 trace 为什么没了/保留率多少/哪类原因丢最多」不可查。

## Solution

`TailSamplingDecisionLog`（observability，纯记账）：

- **决策入账**：record(traceId, KEPT|DROPPED, reason, atMs)——环形明细 64（挤最老计 ringDropped）+按 (决策×原因) 聚合（键封顶 16+溢出桶）。
- **报告**：keptRatio/byReason 计数降序/recent 新→旧。
- **喂点**：采样器装配侧——采样行为零变更。

## 兼容性

纯新增；采样器零变更。

## 诚实边界

记账不改行为；原因语义归策略；明细挤老不可溯（聚合保留）。
