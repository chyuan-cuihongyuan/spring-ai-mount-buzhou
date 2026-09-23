# Spec 4035 — R 系 R36 周期对账（effort #4035，R36）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6071–T6072，impl 2136）。
> 对账门三十六号：Wave 6 五新类型快照补登 + 全仓离线 verify + 台账核账。

## Problem Statement

Wave 6（R31–R35）五件新类型未入公共面快照——全仓 verify
快照门必红；档案计数需对齐。

## Solution

- 快照补登：1153→1158（Eip1559BaseFee/DifficultyRetarget/
  AncestorFeerate/SemVerOrder/JsonPatchApplier——policy×5）；
- api-surface.md 同步 +5 行；CONTEXT 计数 1153→1158；
- **环境确定性清零（O 会话 R78 先例）**：R36 全仓 verify 实测
  `AsyncObservabilityPipelineTest.fullQueueBlocksRatherThanDrops`
  在全仓负载下挂死 40min（jstack：close→flush→`queue.put(token)`
  无界阻塞——flushTimeout 只护 put 之后的 await 不护 put 本身；
  单跑/模块跑 7 测全绿不复现）。硬化 `flush()`：token 入队改
  `offer(…, flushTimeout)` 限时，超时直接同步 `drainBatch()` 兜底
  ——flush 路径不再有无限等待（at-least-once 不变；背压 put
  语义 spec 39 §B 不动）；
- 全仓 16 模块 `mvn verify`（三门全绿）；
- RSession4000LedgerAuditTest 台账核账（spec 4000–4034
  卅一轮四件套零缺位）。

## User Stories

1. 作为对账审计者，公共面快照与实际类型集一致——门不白设。
2. 作为后续轮作者，绿基线起跑。

## Testing Decisions

- 全仓 verify 退出码 0 即验；对账门自跑（范围已纳入 4000–4034）。

## Out of Scope

- 不做文档批量重整（只对计数与新行）。

## Further Notes

- 里程碑：36/50=72%。
