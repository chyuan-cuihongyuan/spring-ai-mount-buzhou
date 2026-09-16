---
id: T3215
title: 确定性散列公共件的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

P 系五件同款内联散列怎么收敛？（spec 2057 / effort #2057 / R58）

## Resolution

**DRY 收敛 `DeterministicHash`（core/metrics 纯静态）**：FNV-1a 64
（标准常量）+splitmix64 终结混合公共件化，五调用点（HLL/频率素描/
布谷鸟/哈希环/SimHash）私有内联删除收敛——哈希值不变（五件既有
36 用例零改动全绿=同一性证明），散列口径单点维护不再漂移。
