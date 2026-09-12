---
id: T1007
title: 健康加权路由抑制原语验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1006]
created: 2026-09-12
---

## Question

联动正确性与状态机隔离如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 4 轮 = effort #703）：①attach 后 3 失败→OPEN：被压路=floor 邻路不变；恢复→CLOSED：权重回声明值；②未匹配名忽略+dampened() 读数；③floor<1 拒绝+listener 抛异常不伤状态机。buzhou-resilience 全模块零回归（C 会话排除集）。
