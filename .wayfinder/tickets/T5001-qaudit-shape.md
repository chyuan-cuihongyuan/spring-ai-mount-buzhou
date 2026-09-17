---
id: T5001
title: Q 会话 R1 对账门的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

3000 系对账门怎么落位？（spec 3000 / effort #3000 / R1）

## Resolution

**QSession3000LedgerAuditTest 四面互证**（P 系公式族第四应用）：
spec 3000–3149 ↔ README 行 ↔ 票 T5001+2(N−3000) 对 ↔ impl
2001+(N−3000)，spec 起点断言 3000 严格递增；号段 fetch 已通 +
本地全档双查空闲后声明（efforts #3000–#3149 / T5001–T5300 /
impl 2001–2150）；总图开图 + MAP.md 登记；P 雾区候选静脉不抢、
P 余量号段不占。
