---
id: T6062
title: R 会话 R31 EIP-1559 基础费调节的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6061]
created: 2026-09-23
---

## Question

R31 合同怎么逐一验绿？（spec 4030 / effort #4030 / R31）

## Resolution

**验证通过**：Eip1559BaseFeeTest 七测全绿——满块 8e9→9e9 /
空块乘法回落（9e9→7.875e9 非对称）/ 恰目标不变三证；floor 语义（base=7 空块→6）；
地板止跌；超弹性/负用量/畸形定构 fail-fast；钳制不变量
（任意合法用量单步 ≤ base/8）+ 确定性回放同轨迹。
