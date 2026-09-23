---
id: T3040
title: O 系 R120 对账轮的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3039]
created: 2026-09-23
---

## Question)

快照双档同步与三门全绿可一次证明吗？（spec 1919 / effort #1919 / R120）

## Resolution`

**全仓 16 模块离线 verify 绿（mvn 退出码 0）**。五类型单测全绿 +
对账门四面互证绿 + 覆盖门绿 + 快照门绿（1126 行档一致）。
