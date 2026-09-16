---
id: T3101
title: P 会话 2000 系对账门的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

P 会话 150 轮（spec 2000–2149）四类工件链怎么用常驻测试互证防漏登？（spec 2000 / effort #2000 / R1）

## Resolution

**L/O 系对账门公式族第三应用 `PSession2000LedgerAuditTest`（starter）**：
号段常量 2000–2149 + 票号公式 shape=T3101+2(N−2000)/verify=+1 + impl
公式 1551+(N−2000)，四断言（票对存在/impl 存在/README 含号/spec 自 2000
严格递增），扫现有 spec 文件驱动范围自扩展——后续轮落地自动纳入对账。
