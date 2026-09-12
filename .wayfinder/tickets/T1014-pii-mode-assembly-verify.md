---
id: T1014
title: PII 假名化模式装配的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T1013
created: 2026-09-13
---

## Question

piiPreserveFormat 声明 → 输入缝假名化（形状保持非占位符）？缺省 MASK 零回归？装配摘要可见？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 32 轮）：① 声明 → 输入 `call \d{11} now`（形状保持、无 [PII:、原文不留痕）；② 缺省 → `call [PII:CN_PHONE] now` 既有语义；③ 装配摘要含双 hook；④ 全模块回归绿。
