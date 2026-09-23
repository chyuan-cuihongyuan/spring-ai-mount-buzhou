---
id: T6142
title: S 会话 S21 Bounded Mailbox 有界信箱的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6141]
created: 2026-09-24
---

## Question

S21 合同怎么逐一验绿？（spec 5020 / effort #5020 / S21）

## Resolution

**验证通过**：BoundedMailboxTest 四测全绿——两策略分叉
（拒新 false / 逐旧纳新 dropped++）；FIFO 次序；空取 null；
capacity≤0/null fail-fast；确定性回放。
