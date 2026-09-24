---
id: T6192
title: S 会话 S46 Robin Hood Hash Table 劫富济贫哈希表的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6191]
created: 2026-09-25
---

## Question

S46 合同怎么逐一验绿？（spec 5045 / effort #5045 / S46）

## Resolution

**验证通过**：RobinHoodHashTableTest 四测全绿——换位场景
钉住（跨环绕簇 maxProbeDistance=3）；后向搬移余键全可查；
100 键 upsert+删除回归；fail-fast。（初版 shiftBack 单索引
死循环已修——双索引空洞/探测分离。）
