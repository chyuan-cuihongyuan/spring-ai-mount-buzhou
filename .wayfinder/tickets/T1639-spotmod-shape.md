---
id: T1639
title: Spotlight×Moderation 顺序协作组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1629
created: 2026-09-15
---

## Question

J 会话第 92 轮：guard 双缝 hook 协作的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：SpotlightHook（ORDER 80 包裹）先于 ContentModerationHook（ORDER 210 检查）执行——**包裹后的标记文本中词表命中的检测行为**（包裹原文被标记字符交织，词表子串可能被破坏）语义组合无验证。纯测试轮第七弹。

形状裁决：新增 `SpotlightModerationComboTest`（buzhou-guard）——钉住真实协作语义：①包裹先于词表检查发生（同缝幂等跳过后 moderation 看到的已是包裹文本）②两读面（SpotlightStats/ModerationStats）在同一 afterTool 调用中的计数一致性。零生产改动。
