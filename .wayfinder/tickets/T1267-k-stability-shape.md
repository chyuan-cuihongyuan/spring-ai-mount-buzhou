---
id: T1267
title: k 次 run 稳定性矩阵的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-13
---

## Question

I 会话第 9 轮：EvalFlakinessDetector（spec 513）只支持两 run A/A，其诚实边界明确留位「不自动重跑 k 次（k 次留宿主循环）」——k 个 run 的逐项稳定性矩阵是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 9 轮 = effort #908 / spec 908 / impl 661）：缺口成立且 spec 513 显式留位。落点 `EvalFlakinessDetector.analyzeK(List<EvalRunResult> runs)` 静态扩展（同纪律：纯函数不触 store；k 次仍由宿主跑）：① 逐项跨 run 对齐（单侧项=漂移不入分母，两 run 版同口径）；② 每项 k 个 verdict 的「主导态占比」= 一致率（红绿映射复用 isRed：pass=绿、fail/error=红从严）；③ 全一致 = 稳定、否则 = 抖动。返回 record `KStabilityReport(int runCount, int compared, int stableItems, int flakyItems, double flakyRate, List<KItemVerdict> verdicts)`（KItemVerdict(itemId, statuses, stable)，status 有界枚举文本）。k<2 fail-fast；runId 去重 fail-fast（同 run 比 两次无意义——诚实校验）。与 pass@k（spec 902 通过概率口径）互补：本面是 verdict 一致性口径。
