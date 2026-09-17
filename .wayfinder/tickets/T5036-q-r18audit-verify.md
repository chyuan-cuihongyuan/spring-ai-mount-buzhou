---
id: T5036
title: Q 会话 R18 对账轮的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5035]
created: 2026-09-18
---

## Question

R18 对账四件事怎么逐一验绿？（spec 3017 / effort #3017 / R18）

## Resolution

**验证通过**：快照 diff 恰 +5 与 Wave 3 清单一致；api-surface.md
五行齐（HLC 后拓扑+攒批、MinHash 后扫线+KMP+Fenwick 两段落位）；
CONTEXT 964→969；全仓 verify 16 模块 BUILD SUCCESS（Q 第三波）；
Q 对账门 spec 3000–3017 十八号四件套齐整；push。
