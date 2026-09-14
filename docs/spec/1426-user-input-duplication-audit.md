# 1426 — 用户输入重复审计

> 来源：L 会话第 27 轮 = effort #1426（票 T2153 / T2154 / impl 1079）。借鉴：Rasa 对话分析（重复用户输入是最强挫败信号——「问题没被听懂」用户就开始复读）。

## Problem Statement

会话历史的 USER 输入重复形态无审计面：复读（连续重发同问题）、高频问题（同输入多轮反复）散落在 history 明细里——「这个会话的用户已经放弃了」只能在事后人工翻记录时发现。ToolCallCoalescer 是工具层合并（执行优化），与用户输入层的挫败信号正交。

## 目标

- `UserInputDuplicationAudit`（core/message，纯函数静态面，private 构造）：
  - `analyze(List<String> userInputTexts)` → `record DuplicationReport(totalInputs, consecutiveDuplicatePairs, maxRepeatRun, distinctInputs, topRepeated)`；
  - 归一化口径：trim + 小写 + 内部空白折叠 + 截断 64 字符（隐私与基数纪律）；
  - `consecutiveDuplicatePairs`（相邻归一化相同——复读直接信号）/ `maxRepeatRun`（最长复读游程，含首条）/ `distinctInputs`（多样性对比）；
  - `topRepeated`：≥2 次才入榜（次数降序平名典序）、封顶 8；
  - 空输入零报告哨兵。
- 纯函数零状态：不裁决不拦截（是否升级人工归宿主）。

## 兼容性

纯函数零 IO；对 history 只读。

## Out of Scope

- 语义级相似（同义改写检测——embedding 域，EmbeddingSelfCheck 正交）。
- 自动升级/告警联动（读面不裁决）。
- 跨会话聚合（单会话口径显式）。
