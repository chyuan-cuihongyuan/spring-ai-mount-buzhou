---
id: T6088
title: R 会话 R44 提交图世代号的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6087]
created: 2026-09-24
---

## Question

R44 合同怎么逐一验绿？（spec 4043 / effort #4043 / R44）

## Resolution

**验证通过**：CommitGraphTest 五测全绿——线性链 1/2/3 与
双向祖先判定；菱形 max+1；倒挂时间戳对照（gen 裁决正确拒、
时间戳启发式误判）；未知父/重复 id/未知 id fail-fast。
