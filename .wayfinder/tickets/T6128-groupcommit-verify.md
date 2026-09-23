---
id: T6128
title: S 会话 S14 Group Commit 组提交的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6127]
created: 2026-09-24
---

## Question

S14 合同怎么逐一验绿？（spec 5013 / effort #5013 / S14）

## Resolution

**验证通过**：GroupCommitLogTest 五测全绿——同组合并（3
append 1 sync）；sync 后新组 LSN 连续；空组空同步上沿不变；
LSN 严格递增与上沿单调；null/空 record fail-fast。
