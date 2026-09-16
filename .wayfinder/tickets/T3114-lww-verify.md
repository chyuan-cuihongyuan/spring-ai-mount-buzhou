---
id: T3114
title: LWW 寄存器的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3113]
created: 2026-09-17
---

## Question

LastWriteWinsRegister 合同（定序/平局/幂等/收敛/畸形）怎么钉住？（spec 2006 / effort #2006 / R7）

## Resolution

**八用例一次全绿**（buzhou-core）：单调 ts 采纳 / 迟到旧写拒+superseded
计数 / ts 平局 writer 字典序胜+conflict 计数 / 完全幂等零计数 / 同
writer 同 ts 异值 first-wins+冲突 / merge 双向收敛（LWW 终值一致）/
空寄存器 current=null / 畸形五型（null value、null writer、负 ts、
null merge、null other）fail-fast。
