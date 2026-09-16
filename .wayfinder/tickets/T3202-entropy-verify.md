---
id: T3202
title: 香农熵读数的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3201]
created: 2026-09-17
---

## Question

ShannonEntropy 合同（零/上界/零频/手算/换算/畸形）怎么钉住？（spec 2050 / effort #2050 / R51）

## Resolution

**七用例一次全绿**（buzhou-core）：全集中 0（100/0/0 与单类）/ 均匀
四类恰 2 bits + 归一化恰 1 / 零频类不改熵且上界按非零 2 计归一化仍
1 / (3,1) 手算 0.811278±1e-5 / nats÷bits=ln2 换算 / (7,3) 归一化 ∈
(0,1) / 畸形六型（null、空表、base 1、零总量、负频数、normalized
null）fail-fast。
