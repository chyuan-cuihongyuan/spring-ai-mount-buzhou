# 908 — k 次 run 稳定性矩阵

> 来源：I 会话第 9 轮 = effort #908（[T1267](../../.wayfinder/tickets/T1267-k-stability-shape.md) / [T1268](../../.wayfinder/tickets/T1268-k-stability-verify.md) / impl 661）。借鉴：Google [FlakyTest](https://github.com/google/flaky-tests) / k 次 A/A 稳定性矩阵——两 run 是 k=2 特例，k>2 才能区分「偶发翻转」与「系统性震荡」。

## Problem Statement

`EvalFlakinessDetector.analyze(A, B)` 只支持两 run（spec 513 诚实边界：「两 run 才能判抖动；k 次留宿主循环」）。宿主跑了 5 个 run 后没有现成读数：哪些项在 5 次里 verdict 稳定（可信任）、哪些翻转（不可信/需修 prompt 或换 judge）——k=2 只能两两比对，O(k²) 且无全景。

## 目标

- `EvalFlakinessDetector.analyzeK(List<EvalRunResult> runs)` 静态方法：
  - 逐项跨 run 对齐：出现在全部 run 的项入分母（单侧项=漂移，同两 run 版口径不入分母）；
  - 每项 verdict 红绿映射复用既有 `isRed`（pass=绿、fail/error=红从严）；
  - 全 run 同色 = `stable`；存在异色 = `flaky`；
  - 返回公共 record `KStabilityReport(int runCount, int compared, int stableItems, int flakyItems, double flakyRate, List<KItemVerdict> verdicts)`；
  - `KItemVerdict(String itemId, List<String> statuses, boolean stable)`（statuses 与入参 run 同序）；
  - 校验：`runs.size() >= 2` fail-fast；runId 重复 fail-fast（同 run 自比无意义）；
  - `flakyRate = flaky / compared`（0 项约定 0.0——空集合法状态纪律）；
- 既有两 run `analyze` 零变化。

## 兼容性

纯增量：公共类新增静态方法 + 公共嵌套 record，零既有行为变化。
