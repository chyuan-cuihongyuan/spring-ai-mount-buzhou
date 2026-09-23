---
id: T6050
title: R 会话 R25 拓扑散布的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6049]
created: 2026-09-23
---

## Question

R25 合同怎么逐一验绿？（spec 4024 / effort #4024 / R25）

## Resolution

**验证通过**：TopologySpreadPlacerTest 五测全绿——均衡三域全可
+ 斜度 0；恰界两向（放宽松域 5−3=2 可/放拥挤域 6−3=3 截止）；
严档 maxSkew=1 交替演化（追平后双开）；数升序确定性 + 拥挤域
截止并存；畸形六型 fail-fast + 空图诚实 0。首版排序用例期望
漏算 z 域放置后斜度已按仿真修正。
