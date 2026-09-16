---
id: T3115
title: 频率素描的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

缓存准入的访问频率门控怎么定容账户化？（spec 2007 / effort #2007 / R8）

## Resolution

**Caffeine W-TinyLFU 线程安全频率素描 `FrequencySketch`
（core/metrics）**：4bit 槽 Count-Min 板（long[] 16 槽/词）+ 相邻两槽
较小者 increment（防独占倾斜）+ frequency=min(两槽)（下界语义——
碰撞只低估，同 key 反复 ≈n/2 序不变）+ 饱和 15 封顶 + 与 HLL 同款
确定性散列。门控只需序不需精确——newcomer<victim 即拒。
