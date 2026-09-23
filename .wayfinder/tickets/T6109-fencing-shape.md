---
id: T6109
title: S 会话 S5 Fencing Token 世代令牌护栏的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

锁易主后旧持有者的迟到写怎么物理拦住？（spec 5004 /
effort #5004 / S5）

## Resolution

**FencingTokenGuard（core/transaction）**：Chubby fencing
思想——acquire 发严格递增 token，tryWrite 三态裁决（ACCEPT/
STALE_TOKEN/UNKNOWN_TOKEN + NO_LOCK）；release 不重置世代；
嵌套 Verdict 不另立面。
