# 1092 — Spotlight×Moderation 顺序协作组合测试轮

> 来源：J 会话第 92 轮 = effort #1092（[T1639](../../.wayfinder/tickets/T1639-spotmod-shape.md) / [T1640](../../.wayfinder/tickets/T1640-spotmod-verify.md) / impl 844）。纯测试轮第七弹（R81/R82/R84/R86/R87/R88 先例）。

## Problem Statement

SpotlightHook（ORDER 80 包裹）与 ContentModerationHook（ORDER 210 检查）在 afterTool 缝顺序协作——**包裹后文本的词表检测行为与双读面计数一致性**无验证。

## 目标

新增 `SpotlightModerationComboTest`（buzhou-guard）：同 ctx 连续两 hook 调用（先 Spotlight 后 Moderation）——双读面计数一致 + 各自守恒保持。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- order 重排实验（既有顺序语义维持）。
