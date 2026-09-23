---
id: T6076
title: R 会话 R38 Holt-Winters 季节指数的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6075]
created: 2026-09-24
---

## Question

R38 合同怎么逐一验绿？（spec 4037 / effort #4037 / R38）

## Resolution

**验证通过**：HoltWintersIndexTest 五测全绿——线性+季节五季
学习逐位预测容差内；常值序列季节指数收敛近零 + 水平收敛；
确定性回放；参数越界/period<2/非有限观测 fail-fast；预热
未毕业 forecast/level ISE。
