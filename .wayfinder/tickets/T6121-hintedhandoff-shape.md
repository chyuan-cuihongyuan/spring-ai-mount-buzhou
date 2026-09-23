---
id: T6121
title: S 会话 S11 Hinted Handoff 暂代投递的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

目标暂不可用怎么不丢不阻塞且恢复后按序回放？（spec 5010 /
effort #5010 / S11）

## Resolution

**HintedHandoff（core/recovery）**：Cassandra hinted handoff
思想——route 健康直投/下线记 hint（主路径不阻塞），
markDown/markUp 状态机（重复转换 IAE），markUp FIFO 回放清
队；恢复后再下线独立累积。
