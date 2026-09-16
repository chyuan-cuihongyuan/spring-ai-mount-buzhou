---
id: T3122
title: 判定决策缓存的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3121]
created: 2026-09-17
---

## Question

DecisionCache 合同（TTL 边界/惰性清除/LRU/失效/命中率/畸形）怎么钉住？（spec 2010 / effort #2010 / R11）

## Resolution

**八用例一次全绿**（buzhou-guard）：TTL 界内 999 命中/恰 1000 过期 /
过期清除后重查才计 miss / LRU 驱逐最久未用（get 提升 a 后 b 被逐）/
invalidate 后=miss / 覆盖回填刷新 ts（旧 ts 过期新 ts 未过仍命中）/
命中率 2/3 精确+空缓存 0 不除零 / 回拨宽进 / 畸形六型 fail-fast。
