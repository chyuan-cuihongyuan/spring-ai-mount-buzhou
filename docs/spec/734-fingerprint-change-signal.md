# 734 — 数据集指纹变更信号

> 来源：G 会话第 35 轮 = effort #734（82 指纹入档的消费信号）/ [T1068](../../.wayfinder/tickets/T1068-fingerprint-change.md) / [T1069](../../.wayfinder/tickets/T1069-fingerprint-change-verify.md) / impl 634。

## Problem
82 让每次 run 记录携带数据集指纹——但「数据集变了」这个事实埋在记录对比里：连续跑批时指纹变化（同参对比失效）无人提示，diff 结论可能张冠李戴。

## Solution
EvalRunner 内置信号：当前 run 指纹 vs 最近历史 run 指纹不同 → `lastFingerprintChanged()` 置位+`buzhou.eval.fingerprint.changed` 计数+INFO（diff 明细归 EvalRunDiff——本面只做「变了」的一眼信号）。首跑 false；读历史失败降级跳过。

## Out of Scope
diff 明细（EvalRunDiff 既有）；自动重跑。
