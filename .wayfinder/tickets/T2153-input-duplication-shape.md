---
id: T2153
title: 用户输入重复审计（UserInputDuplicationAudit）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 27 轮：用户输入重复形态审计面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：message 域无输入重复审计面（ToolCallCoalescer 是工具执行层合并，正交）；复读=最强挫败信号。

形状裁决：UserInputDuplicationAudit 纯函数（core/message）——analyze(List<String>)→DuplicationReport（归一化 trim+小写+空白折叠+截断 64；consecutiveDuplicatePairs+maxRepeatRun 含首条+distinctInputs+topRepeated ≥2 才入、容量 8、降序典序）+空输入哨兵；不裁决不拦截。

Out of scope：语义相似；告警联动；跨会话聚合。
