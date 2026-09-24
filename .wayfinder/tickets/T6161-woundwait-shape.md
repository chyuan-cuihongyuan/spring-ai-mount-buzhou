---
id: T6161
title: S 会话 S31 Wait-Die/Wound-Wait 死锁预防时序裁决的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

并发事务抢资源怎么从根防死锁（不成环）？（spec 5030 /
effort #5030 / S31；原拟 Jump Hash 撞 spec 3020 换静脉）

## Resolution

**WoundWaitGate（core/transaction）**：PostgreSQL/DB2 死锁
预防经典思想——时间戳定年龄（显式注入+id tie-break），
WOUND_WAIT 年长枪伤年轻持有者/年轻等待，WAIT_DIE 年长等待/
年轻自裁——等待图单向环不可得；release 交接年长等待者；
abortedCount 读数；畸形 fail-fast。
