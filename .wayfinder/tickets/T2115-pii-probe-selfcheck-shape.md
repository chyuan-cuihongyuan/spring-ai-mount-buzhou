---
id: T2115
title: PII 检测器合成探针自查（PiiProbeSelfCheck）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 8 轮（换题轮）：检测器例行自查面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察换题：原题 single-flight 与 ToolCallCoalescer（core/exec）全撞——换入 R29 题（guard/pii 无自查面，PiiHitStats 是命中计量非检测能力自查）。

形状裁决：`PiiProbeSelfCheck` 纯函数——内建确定性合成池（正例 EMAIL×2/CN_PHONE×2/BANK_CARD×1 测试 PAN/IPV4×2 + 负例×4）+ probe(detector) → ProbeReport(recalls 典序, falsePositives) + recall()/overallRecall() 派生；误报哨兵恒 0；**身份证号不入池**（校验位合法合成号撞真实号红线，spec 如实入档）。

Out of scope：台账化；调度接线；CustomPiiRules 类型扩展。
