---
id: T6005
title: R 会话 R3 Space-Saving 频繁项的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

频繁项名单之外还要名次与读数怎么一次到位？（spec 4002 / effort #4002 / R3）

## Resolution

**SpaceSavingTopK（core/metrics）**：Metwally 2005——k 槽精确计数，
满榜新键淘汰最小者并继承其计数 +1（最小计数即误差界），保留者恒
≥ 真值单侧高估；top() 降序确定性 + minCount 误差界 + 守恒账。
与 MisraGries（要候选）/CountMinSketch（要点查）成素描三件。
