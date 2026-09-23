---
id: T6040
title: R 会话 R20 反放大窗的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6039]
created: 2026-09-23
---

## Question

R20 合同怎么逐一验绿？（spec 4019 / effort #4019 / R20）

## Resolution

**验证通过**：QuicAmplificationWindowTest 五测全绿——千字节 3 倍
恰界（3000 可/3001 不可）+ 2999 扣减余 1；两笔累积 450/200 扣减；
超发 IllegalStateException + 恰尽再发拒；验证解除全速 + credit
MAX 语义；初始授信 1200 覆盖握手 + 畸形六型 fail-fast。
