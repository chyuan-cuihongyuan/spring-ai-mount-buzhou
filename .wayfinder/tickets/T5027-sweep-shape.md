---
id: T5027
title: Q 会话 R14 扫线最大并发的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

历史区间的并发峰值怎么精确单遍度量？（spec 3013 / effort #3013 / R14）

## Resolution

**SweepLineIntervals（core/metrics，纯函数）**：事件点 +1/−1 扫线，
半开 [start,end) 语义（同刻 −1 先处理——相接不算并发，闭区间
相邻段重复计数病的根治）+SweepResult(峰值,首发点,总数)+空集
哨兵+零宽贡献零+破缺 fail-fast。
