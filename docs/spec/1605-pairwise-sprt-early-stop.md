# 1605 · A/B 成对评估 SPRT 序贯提前终止（Wald SPRT 思想）

> 来源：N 会话 R6（effort #1605 / T2361–T2362 / impl 1158）。借鉴对象：Wald 序贯
> 概率比检验（SPRT）——现代 A/B 平台（GrowthBook / Statsig）sequential testing 同源：
> 证据累积到统计显著即停，不为已定的结论支付剩余实验成本。

## Problem Statement

A/B 成对评估（spec 71）对数据集全量执行：双 runtime 各跑一遍 + 逐项 judge 裁决。
当一侧优势压倒性（例如连续全胜）时，结论在第 5 项已具统计显著性——剩余几十项的
模型调用与 judge 成本是纯浪费。评估预算闸（spec 520）按预算硬截断（截到哪算哪，
无方向结论）；SPRT 是「证据驱动」的停——达界即停且带显著性保证。

## Solution

`PairwiseSprtPolicy(α, β)`（默认 0.05/0.10）：符号检验口径（每对非平局项计 +1/-1，
平局/错误不进检验分母）。序贯判定 `decide(winsA, winsB)`：

- LLR = sign(p̂−0.5) × [winsA·ln(2p̂) + winsB·ln(2(1−p̂))]（**方向分离**——MLE 双侧
  极端不得算错方向）
- LLR ≥ ln((1−β)/α) → PREFER_A；LLR ≤ ln(β/(1−α)) → PREFER_B；否则 CONTINUE

`PairwiseEvalRunner.compare(...)` 新 5 参重载（+policy，null=现状全量）：每项裁决
落位后序贯判定，达界即置停——后续未起项 skipped（verdict/error 双空，与 error 桶
诚实分离）；`PairwiseSummary` 扩 `skipped` 与 `sprtDecision` 字段（落盘 encode/decode
与 ab.run.completed 事件同步，旧记录 decode 缺省 0/null）。

## User Stories

1. 作为评估者，我想在优势显著时提前拿到结论，所以压倒性优势的对比不再支付全量成本。
2. 作为评估者，我想未达显著时继续跑满，所以 CONTINUE 语义保证结论的显著性边界。
3. 作为运维者，我想跳过与错误诚实分离，所以 skipped 独立成桶不混入 error。
4. 作为开发者，我想默认零变化，所以不传 policy 的既有 compare 全量执行照旧。

## Implementation Decisions

- 平局不进符号检验（winsA+winsB = 非平局数）——judge 无判别力时检验自动稀释。
- 诚实边界：SPRT 假定项序可交换——数据集构建序即序贯序，系统性排序偏差破坏检验
  效力（结论保守，但有效性依赖数据集纪律）。
- skipped 项 byIndex 位保持 null（Arrays.asList 容 null——List.of 拒 null）。

## Testing Decisions

- `PairwiseSprtPolicyTest`（复用 PairwiseEvalRunnerTest 伪件模式）：
  ① 判定器边界：连胜 4 项 CONTINUE / 5 项 PREFER_A（ln18≈2.89 边界钉死）/ 对称
  PREFER_B / 零样本与均势 CONTINUE；
  ② 非法 α/β fail-fast；
  ③ 集成：40 项数据集 + A 恒胜 → 第 5 项停（winsA=5、skipped=35、sprtDecision=
  PREFER_A、total=40）；
  ④ 未启用：8 项全量 winsA=8、skipped=0、sprtDecision=null。
- 回归：eval 包全量（235 用例）绿。

## Out of Scope

- 双侧/多臂（>2 runtime）序贯检验。
- SPRT 边界的 α-spending 校正（当前单次对比单检验，无多重比较场景）。

## Further Notes

- 落盘记录含 skipped/sprtDecision——历史 run 回读零损（旧记录缺省字段）。
