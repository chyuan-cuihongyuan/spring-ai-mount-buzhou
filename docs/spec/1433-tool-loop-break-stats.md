# 1433 — 工具循环打断分布读面

> 来源：L 会话第 34 轮 = effort #1433（票 T2169 / T2170 / impl 1086）。借鉴：Temporal workflow retry-loop detection（循环打断需要按目标分布——「哪个工具在烧配额」是 runaway 治理的行动面）。

## Problem Statement

`ToolLoopBreakerHook`（同工具同参循环打断，spec 138 族）只有全局 counter：**哪个工具被打断多少次、打断时 run 已到多长**无分布——「search 工具占全部打断的 80%」这类定向治理信号不可见。

## 目标

- `ToolLoopBreakerHook`（core/runaway）增量：
  - per-tool 打断计数表（256 封顶折 `__overflow__`——拒绝表同纪律）；
  - `brokenByToolSnapshot()`：工具 → 累计打断次数（次数降序同次数字典序）；
  - `brokenTotal()` 累计打断总次数 + `maxRunObserved()` 打断时点最长 run 水位（单调——「循环长到多长才被掐」量化）；
  - `resetBrokenForTest()` 清分布不清会话 run 状态；
  - beforeTool 打断路径单点记账；放行路径零动作；Block 语义逐位不变。

## 兼容性

纯增量读面：循环判定（key=工具|参数 hash、window 阈值、换参重置）/打断 Block 文案/MAX_SESSIONS 诚实降级逐位不变。

## Out of Scope

- per-session 分桶（MAX_SESSIONS 基数红线）。
- 参数相似度判定（hash 相等口径维持——语义去重是 RepetitionDetector 域）。
- 打断后的自动升级（读面不裁决）。
