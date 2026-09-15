# Spec 1717 — 虚拟键份额读面（effort #1717，R18）（effort #1717，R18）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2635–T2636，impl 1317，impl OpenRouter 多键路由遥测 + 经济学 HHI 集中度）。借鉴：VirtualKeys 多键轮换无份额账：负载是否真被摊开、还是被某一把独吞（配置错/粘滞 bug）——集中度不可见。

## Problem Statement

`VirtualKeyShareStats`（core/budget，实例面线程安全）：record(key) 逐次计入（null/空按 _anonymous_ 桶）+census()→ShareCensus(total/份额降序表（LinkedHashMap 保序）/HHI Σ份额² 0..1 无样本 −1)+resetForTest。单键 HHI=1 完全集中；均分 n 把 HHI=1/n。

## Solution

作为配额管理者，HHI 从 0.33 漂向 0.9 → 轮换失效，某键将先撞限。

## User Stories

1. 17170
2. 17171
3. 17172

## Implementation Decisions

- 17173

## Testing Decisions

- 17174

## Out of Scope

- 17175

## Further Notes

- 17176
