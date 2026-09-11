# Spec 525 — 评估集合成扩增（effort #525）

> wayfinder map：`.wayfinder/maps/effort-525.md`（T803–T804）。E 会话第 26 轮。

## Problem Statement

评估数据集全靠手工录入/轨迹回流——一条好用例难以低成本扩覆盖。
Ragas testset generation：种子用例 → LLM 生成 N 条同语义改写 → 人审
入库。

## Solution

`eval.EvalCaseAmplifier`（ctor ChatModel，lambda 注入先例同 runner）：

- `amplify(seed, count)` → `List<EvalItem 候选>`：内置改写指令（保持
  语义与判定等价、改表层表述；count 条；每行 JSON {"input","expected"}），
  围栏剥离后逐行解析。
- 坏行跳过计数（unparseableLines）；零可解析 → EVAL_OPERATION_INVALID
  带模型输出预览（前 500 字符）。
- 产出 id=null 的候选（入库走 datasetStore.add 与手工项同管道重排 id）；
  source 溯源清空（合成项非回流项）。
- **人审教义**：调用方决定入库与否——本类只产候选，不做语义等价断言。

## User Stories

1. 作为评测方，我想一条好用例扩增出 N 条同语义变体， so 小数据集快速
   扩覆盖（人审后入库）。

## Implementation Decisions

- 内置指令写死语义保持约束（可覆写 prompt 留后续）。
- 解析容错：围栏剥离 + 逐行隔离——单坏行不影响好行。

## Testing Decisions

- 假模型返回 3 行 JSON → 3 候选（id 空溯源空）；围栏+1 坏行 → 2 候选
  +skipped 1；零可解析 → 异常带预览；count 传令牌进指令。

## Out of Scope

- 自动入库；语义断言；多语言。

## Further Notes

- 新公共类型 `EvalCaseAmplifier` 随轮 regenerate 快照 + api-surface.md
  加行。
