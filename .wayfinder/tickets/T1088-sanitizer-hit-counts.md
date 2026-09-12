---
id: T1088
title: 导出脱敏命中计数的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question
导出脱敏动了多少刀无证据——加命中计数吗？

## Resolution
**用户常设授权 AFK（可推翻）**

决策（G 会话第 44 轮 = effort #743 / spec 743 / impl 644）：Sanitizer 加 hitCounts/totalHits——PiiType 名+custom:规则名归因（detector.scan 过滤 enabledTypes + 自定义 pattern matcher 计数）；累计跨调用；CustomPiiRules 加 rules() 只读访问器。
