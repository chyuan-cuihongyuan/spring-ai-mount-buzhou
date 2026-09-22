---
id: T2992
title: O 系 R96 对账轮的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2991]
created: 2026-09-23
---

## Question)

快照双档同步与三门全绿可一次证明吗？（spec 1895 / effort #1895 / R96）

## Resolution`

**全仓 16 模块离线 verify 绿（mvn 退出码 0）**。五类型单测全绿 +
对账门四面互证绿 + 覆盖门绿 + 快照门绿（1106 行档一致）。
