---
id: T3130
title: 键压缩日志语义的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3129]
created: 2026-09-17
---

## Question

KeyCompaction 合同（最新胜/乱序幂等/墓碑族谱/对账/畸形）怎么钉住？（spec 2014 / effort #2014 / R15）

## Resolution

**九用例一次全绿**（buzhou-core）：最新 seq 胜 / 乱序幂等（正反序
同结果）/ 墓碑删除+tombstones 计数 / 墓碑后复活（更高 seq 非墓碑）/
旧墓碑不遮新值 / 同 seq 后见者 / 压缩率 1/3+空不除零 / null 集合空 /
畸形两型（null key、负 seq）fail-fast。
