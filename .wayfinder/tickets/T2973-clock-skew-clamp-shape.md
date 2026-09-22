---
id: T2973
title: 时钟偏斜校正的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

观测树父子区间失真的钳位规则怎么确定？（spec 1886 / effort #1886 / R87）

## Resolution`

**Zipkin/Brave 钳位语义纯计算 `ClockSkewClamp`（core/observability）**：
clamp（负偏斜平移保持时长 → 终点越界收缩时长下限 0，返回
ClampedSpan{begin,end,skewApplied}）+ skewMillis 偏斜读数。只动子
不动父；区间合法性双查 fail-fast。落轮 grep 复核无占坑。
