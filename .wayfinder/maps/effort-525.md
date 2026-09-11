# Wayfinder Map — Buzhou 评估集合成扩增（effort #525，E 会话第 26 轮）

> E 会话第 26 轮。勘察：评估数据集全靠手工/轨迹回流（72）——**种子
> 用例 LLM 变体扩增**（一条好用例生成 N 条同语义改写，扩覆盖不换语义）
> 空白。Ragas testset generation 思想。core eval 已有 lambda ChatModel
> 注入先例（runner）。

## Destination

`eval.EvalCaseAmplifier`（ctor ChatModel）：`amplify(seed, count)` →
新 EvalItem 列表——内置改写指令（保持语义与判定等价、改表层表述）+
逐行 JSON 解析（{"input","expected"}）；坏行跳过计数、零可解析 →
EVAL_OPERATION_INVALID 带模型输出预览；产出的 id 置空由 datasetStore
add 重排（与手工项同管道——回流同权）。诚实边界：**LLM 生成需人审**
（testset 教义——调用方决定入库与否，本类只产候选）；语义等价性不做
自动断言（判定等价是语义问题）。

## Notes

- 号段：spec 525 / T803–T804 / impl-428。
- 借鉴源：Ragas testset generation（种子→LLM 变体+人审）。

## Out of scope

- 自动入库；语义等价断言；多语言扩增策略。

## Tickets

- [x] [T803 变体生成与解析](../tickets/T803-case-amplifier.md)
- [x] [T804 坏行容错与语义边界](../tickets/T804-case-amplifier-parse.md)
