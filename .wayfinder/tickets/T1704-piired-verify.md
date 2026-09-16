---
id: T1704
title: PII 出站脱敏读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1703
created: 2026-09-15
---

## Question

J 会话第 122 轮：PiiEventRedStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（PiiEventRedStatsTest，桩 listener 骨架——见既有 Pii 测试）：含 PII 事件 → redacted=1；干净事件 → cleanPassthrough=1；守恒恒等式；resetForTest 归零。定向 `mvn -pl buzhou-guard -am test -Dtest='PiiEventRedStatsTest'` 绿。
