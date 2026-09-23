---
id: T6110
title: S 会话 S5 Fencing Token 世代令牌护栏的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6109]
created: 2026-09-24
---

## Question

S5 合同怎么逐一验绿？（spec 5004 / effort #5004 / S5）

## Resolution

**验证通过**：FencingTokenGuardTest 六测全绿——易主拦截图景
（A 旧写 STALE/B 新写 ACCEPT）；未来 token UNKNOWN；无锁
NO_LOCK；release 后旧 token 仍拒；多锁独立；畸形 id fail-fast。
