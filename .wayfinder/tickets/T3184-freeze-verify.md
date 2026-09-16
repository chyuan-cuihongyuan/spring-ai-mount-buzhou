---
id: T3184
title: 可冻结分段缓冲的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3183]
created: 2026-09-17
---

## Question

FreezableBuffer 合同（自动/手动封冻/drain/快照序/畸形）怎么钉住？（spec 2040 / effort #2040 / R41）

## Resolution

**七用例全绿**（buzhou-spill）：容量 3 第四 append 恰触发封冻（frozen=1
mutable=1 total=4）/ 手动封冻+空段 no-op / 5 元素两段 drain 内容精确
（v1v2/v3v4）+v5 保留+再 drain 空 / 7 元素快照追加序 / drain 后快照只
含 old3+new1 / 容量 1 每 append 即冻 / 畸形两型（容量 0、null append）
fail-fast。
