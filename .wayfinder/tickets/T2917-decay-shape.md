---
id: T2917
title: 频次衰减竞速的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

老热点退烧与新热点顶替怎么读数？（spec 1858 / effort #1858 / R59）

## Resolution`

**Redis LFU counter decay 思想纯数学 `FrequencyDecay`（core/cache）**：
decayed(counter, periods) 每周期减半向下取整封底 0 + overtakePeriod(
counter, newHitsPerPeriod) 最小 t 使命中×t > 衰减值——新热点顶替老热点
的周期数（缓存自适应性读数：过长=老赖着、过短=抖动；零命中 -1 哨兵，
MAX_RACE_PERIODS 保险丝）。确定性纯函数（对照 Redis 概率增量口径）。

