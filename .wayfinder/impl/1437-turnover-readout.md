# impl 1437 — TurnoverReadout 库存周转读面（R37 = effort #1836 / spec 1836 / T2873-T2874）

**What**：`TurnoverReadout`（buzhou-spill 静态纯函数）——turns 周转次数
（-1/0 哨兵）+ depletionHorizonMillis 耗尽视界（向上取整保守、-1/0 哨兵）；
负库存/负消费/NaN 速率 fail-fast。

**Why**：供应链库存周转思想——存量只答「有多少」；周转（活性）与视界
（紧迫）互为倒数的一对读数让「TTL 激进 vs 预热扩容」分诊有据。

**Verify**：`TurnoverReadoutTest` 3 用例全绿。

**Status**：done（2026-09-16）
