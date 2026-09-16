---
id: T3230
title: Z 序曲线的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3229]
created: 2026-09-17
---

## Question

ZOrderCurve 合同（往返/手算/交织/格紧致/对称）怎么钉住？（spec 2064 / effort #2064 / R65）

## Resolution

**七用例全绿**（首跑 1 红教训：逐对邻域紧界断言不成立——Z 序局部
性是格内/渐近语义，借位位结构致邻点标量跳变；改数学可证的格内紧
致断言后 7/7）：往返含 int 极值 / 手算 (1,0)=1(0,1)=2(1,1)=3(2,0)=4
(0,2)=8 / 交织结构 3→0b0101 / 16×16 格全 <256 / 4×4 格极差 <64 /
8×8 格 <4096 / scalarGap 对称。
