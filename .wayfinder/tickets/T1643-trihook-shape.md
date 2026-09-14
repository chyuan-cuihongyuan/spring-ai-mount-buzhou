---
id: T1643
title: guard 三 hook 链顺序协作组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1631
created: 2026-09-15
---

## Question

J 会话第 94 轮：guard 三 hook 链协作的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：SecretScanHook（ORDER 40 脱敏）→ SpotlightHook（80 包裹）→ ContentModerationHook（210 检查）三 hook 在 afterTool 缝顺序协作——**三读面在链中的计数一致性**无验证。纯测试轮第九弹。

形状裁决：新增 `TriHookChainReadoutTest`（buzhou-guard）——同 ctx 三 hook 顺序调用（含 PII/词表混合内容）后 SpotlightStats/ModerationStats 各自守恒保持 + 链路语义（先脱敏后包裹再检查）计数一致。SecretScanStats 在 scanner 层不入组合断言（形状不同在册）。零生产改动。
