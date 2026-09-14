---
id: T1640
title: Spotlight×Moderation 顺序协作组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1639
created: 2026-09-15
---

## Question

J 会话第 92 轮：顺序协作组合如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（SpotlightModerationComboTest）：连续两 hook 同 ctx 调用（先 Spotlight 后 Moderation）双读面计数一致 + 各自守恒。定向 `mvn -pl buzhou-guard -am test -Dtest='SpotlightModerationComboTest'` 绿。
