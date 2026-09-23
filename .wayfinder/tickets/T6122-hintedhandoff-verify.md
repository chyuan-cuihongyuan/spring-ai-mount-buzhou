---
id: T6122
title: S 会话 S11 Hinted Handoff 暂代投递的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6121]
created: 2026-09-24
---

## Question

S11 合同怎么逐一验绿？（spec 5010 / effort #5010 / S11）

## Resolution

**验证通过**：HintedHandoffTest 五测全绿——健康/下线路由
分叉；下线累积 FIFO 回放清队；恢复后再下线独立累积；重复
状态转换 IAE；未知目标 fail-fast。
