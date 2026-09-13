---
id: T1255
title: pass@k 无偏估计器的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-13
---

## Question

I 会话第 3 轮：pass@k（HumanEval/Codex 无偏估计）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 3 轮 = effort #902 / spec 902 / impl 655）：缺口成立——本仓评估口径只有单次 passRate（EvalRunResult）与 A/A 抖动（spec 513），无「k 次采样至少一次通过」的概率无偏估计。落点 core.eval 新公共纯函数类 `EvalPassAtK`（与 EvalFlakinessDetector 同型：纯函数不触 store、不引依赖）：`estimate(n, c, k)` 用 HumanEval 论文 §2.1 无偏估计 `1 − ∏_{i=0}^{k−1} (n−c−i)/(n−i)`（连乘形式数值稳定、不引组合数溢出）+ `aggregate(int[] passCounts, int n, k)` 逐项估计后算术平均（论文口径）。宿主自行多次跑 run 后喂状态（n 次采样从哪来不做框架化——k 次留宿主循环与 spec 513 诚实边界一致）。
