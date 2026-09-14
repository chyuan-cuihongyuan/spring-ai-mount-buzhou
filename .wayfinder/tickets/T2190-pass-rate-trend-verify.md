---
id: T2190
title: Theil–Sen 斜率方向判定的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2189
created: 2026-09-14
---

## Question

如何证明趋势方向判定与离群抗噪？

## Resolution

**用户常设授权 AFK（可推翻）**

`EvalPassRateTrendTest` 六测全绿（`mvn -pl buzhou-core -am test`）：样本不足哨兵；单调改进/退化方向判定；**离群 run 不扭曲**（0.5 系列混 0.9 → STABLE——中位数抗噪核心断言）；稳定死区（±0.005/run 内）；passRates 保序回读。
