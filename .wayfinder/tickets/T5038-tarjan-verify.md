---
id: T5038
title: Q 会话 R19 Tarjan SCC 的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5037]
created: 2026-09-18
---

## Question

R19 合同怎么逐一验绿？（spec 3018 / effort #3018 / R19）

## Resolution

**验证通过**：TarjanSccFinderTest 八测全绿——双点环单组件 [0,1]、
菱形 DAG 四单点无环、反拓扑序（{1} sink 先出）、自环单点算环、
双环相连两组件（sink 侧 {2,3} 先出）+四环成员、2 万深链 2 万组件
不爆栈（迭代生存证明）、幂等+计数、越界/负容量 fail-fast。
