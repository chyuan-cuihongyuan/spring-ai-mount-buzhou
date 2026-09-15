---
id: T2830
title: 热点重平衡建议器的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2829]
created: 2026-09-16
---

## Question

贪心搬迁在容差/封顶/均衡/哨兵/畸形/确定性六面下正确吗？（spec 1814 / effort #1814 / R15）

## Resolution

**HotspotRebalancerTest 6 用例全绿**（mvn -pl buzhou-spill test
-Dtest=HotspotRebalancerTest）：容差内停手；量子 spread/2 封顶（5/0 搬 2
不反转）；均衡输入零建议；少于两节点/空表/null 哨兵；量子<1/负容差/空白
id/负负载 fail-fast；并列取 id 字典序确定性。首跑编译红（List.of 混 null
推断）改 Arrays.asList 显型后绿。

