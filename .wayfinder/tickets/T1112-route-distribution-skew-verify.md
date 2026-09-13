---
id: T1112
title: 路由分布倾斜读数验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1111]
created: 2026-09-13
---

## Question

份额/偏差/基尼的数值正确性如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 6 轮 = effort #805）：RouteDistributionReadoutTest 6 例——三路由非打平偏差降序 0.40/0.21/0.19+份额期望 1e-9 精确/缺权重与零流量偏差行/基尼三已知值（均匀 0、2 路由全集中 0.5、无流量 0）/空报告/收集器封顶 32+totalRecorded 含截断+联动分析/fail-fast。教训注记：Map.of 无序+两路由偏差恒互反——确定性序必须显式破平。
