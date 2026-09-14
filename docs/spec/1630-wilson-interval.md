# 1630 · A/B 胜率 Wilson 置信区间

> 来源：N 会话 R31（effort #1630 / T2411–T2412 / impl 1183）。

## Problem Statement

A/B 汇总只报胜率点估计：小样本下「0.7（n=10）」与「0.7（n=1000）」在报告里
同样确凿——结论强度不可见。正态近似区间在小样本/极端比例下出负值越界
（经典缺陷）；Wilson score 区间数学上不越界。

## Solution

`WilsonInterval.of(successes, n[, z])`（core/eval 纯函数，默认 z=1.96 即 95%）：
Wald-Wilson 原式无连续性校正。挂 `ab.run.completed` 事件 payload
（winRateAciLow/High，分母 = winsA+winsB 的 decided 口径——ties 不进符号检验
与 SPRT 同口径）。与 SPRT（spec 1605）互补：决策面 vs 报告面。

## Testing Decisions

- `WilsonIntervalTest` 四断言：区间含点估计且在 (0,1)；全胜/全败不越界；
  小样本区间宽于大样本；退化输入（n=0/越界/z≤0）零区间。
- 回归：PairwiseEvalRunnerTest 11 用例。

## Out of Scope

- 序贯修正区间（SPRT 停止后的事后区间有保守修正——报告面用标准 Wilson 够）。
- 落盘 summary 的区间字段（事件面先行——落盘 decode 兼容旧记录即可）。
