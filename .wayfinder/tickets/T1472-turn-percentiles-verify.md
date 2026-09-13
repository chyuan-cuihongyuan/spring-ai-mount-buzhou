---
id: T1472
title: 轮次时延分位数读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1471
created: 2026-09-14
---

## Question

J 会话第 11 轮：分位数读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（TurnTimingPercentilesTest，复用 DefaultTurnContext 骨架 + 纯函数直测双轨）：R-7 插值已知值（[10,20,30,40]：p50=25、p95=38.5、q=0/1 端点精确）；单样本任意 q 恒自身；越界 q 拒；空窗/未知会话零值行；一轮真实 beforeTurn/afterTurn 后 count=1 的冒烟断言（时间值只断非负——真钟不做时值断言）。定向 `mvn -pl buzhou-core test -Dtest='TurnTimingPercentilesTest,TurnTimingHookTest'` 绿。
