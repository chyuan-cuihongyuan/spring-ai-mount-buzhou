# 902 — pass@k 无偏估计器

> 来源：I 会话第 3 轮 = effort #902（[T1255](../../.wayfinder/tickets/T1255-pass-at-k-shape.md) / [T1256](../../.wayfinder/tickets/T1256-pass-at-k-verify.md) / impl 655）。借鉴：OpenAI HumanEval / Codex 论文 [pass@k 无偏估计](https://arxiv.org/abs/2107.03374)（§2.1）。

## Problem Statement

本仓评估口径只有单次 passRate（`EvalRunResult`）与 A/A 抖动检测（spec 513）。生成类任务（代码/结构化输出）的正确口径是「k 次采样至少一次通过的概率」——直接采样估计有偏（k 大时趋 0 假象），HumanEval 给出无偏估计公式。本仓无此口径。

## 目标

- 新公共纯函数类 `EvalPassAtK`（core.eval，api 面）：
  - `estimate(int n, int c, int k)`：单项无偏估计 `1 − ∏_{i=0}^{k−1} (n−c−i)/(n−i)`（连乘形式，无组合数阶乘溢出）；
  - `aggregate(int[] passCounts, int n, int k)`：逐项 `estimate` 后算术平均（论文口径；每项同为 n 次采样才公平，c_i=passCounts[i]）；
- 参数校验：n ≥ 1、0 ≤ c ≤ n、1 ≤ k ≤ n（违者 IllegalArgumentException）；
- 纯函数不触 store、不引依赖（EvalFlakinessDetector 同型纪律）；k 次采样由宿主多次 `run` 后自行喂入（spec 513「k 次留宿主循环」边界一致）。

## 兼容性

纯增量新类型，零既有行为变化。
