# Spec 1890 — 阻塞期审计（effort #1890，R91）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2981–T2982，impl 1491）。借鉴：
> Oracle ASH（Active Session History）会话状态二元采样——任何时刻
> 会话要么 ON_CPU 在算、要么 WAITING 在等（锁/I/O/网络）；
> 「慢 = 算得慢还是等得久」的第一分诊面。

## Problem Statement

轮次/工具执行慢的排查两眼一抹黑：耗时 30 秒是 CPU 算不动、锁没抢到
还是 I/O 等不到——总时长一把尺没有分诊能力，优化方向（加算力/
查锁/换盘）全靠猜。

## Solution

`WaitProfileAudit`（core/metrics，静态纯函数 + 嵌套 Profile）：

- `profile(onCpu, lockWait, ioWait, elapsed)`：账面守恒（onCpu +
  lockWait + ioWait = elapsed，不等即畸形 fail-fast）→ Profile
  （三类时长 + 阻塞比 + 主导类）；
- `dominantClass(...)`：主导状态——CPU > I/O > 锁序取大（并列按
  固定序，确定性）；
- `blockingRatio(...)`：阻塞比 =（锁 + I/O）/ 总时长。

## User Stories

1. 作为性能分诊者，onCpu 2s / lock 20s / io 8s / 总 30s → 主导锁
   等待、阻塞比 93%——优化方向直指锁而非算力。
2. 作为报告作者，阻塞比读数接入轮次观测——慢轮次自动分诊。
3. 作为确定性评审者，并列按 CPU > I/O > 锁固定序——同输入同输出。

## Implementation Decisions

- 纯函数零状态；账面守恒（sum == elapsed）fail-fast；枚举串
  ON_CPU/IO_WAIT/LOCK_WAIT 固定序。

## Testing Decisions

- 锁主导/IO 主导/CPU 主导三例；并列固定序一例；守恒破坏 fail-fast
  两例（和 > 总/和 < 总）；阻塞比精确断言。

## Out of Scope

- 不做采样采集（归观测管线）；不做锁源定位。

## Further Notes

- 与 StealTimeReadout（tick 被偷）互补：那是算力被外部拿走，这是
  生命周期内部的时间构成。
