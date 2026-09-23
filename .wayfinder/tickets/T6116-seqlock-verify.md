---
id: T6116
title: S 会话 S8 Seqlock 序号锁的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6115]
created: 2026-09-24
---

## Question

S8 合同怎么逐一验绿？（spec 5007 / effort #5007 / S8）

## Resolution

**验证通过**：SeqLockTest 四测全绿——序号奇偶算术；跨写校验
失败；并发烟测（二元组永不撕裂 + 终态可见）；畸形 writeEnd
IAE；确定性序号回放。
