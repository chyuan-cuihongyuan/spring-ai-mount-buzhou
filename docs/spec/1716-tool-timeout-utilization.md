# Spec 1716 — 工具超时余量直方（effort #1716，R17）（effort #1716，R17）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2633–T2634，impl 1316，impl Envoy 请求超时利用率）。借鉴：工具限时（ToolTimeoutOverrides）设完就忘：限时是余量充足还是常年贴线、还是虚设（全在低位）——利用率分布缺位，调限时全凭感觉。

## Problem Statement

`ToolTimeoutUtilization`（core/hook，实例面线程安全）：record(durationMillis, limitMillis) 记利用率 ratio（limit≤0 忽略——无预算不谈利用率），默认边界 {0.25,0.5,0.75,0.90,1.0} → 6 桶（<25%/<50%/<75%/<90%/<100%/≥100% 超时档）+maxRatio 千分精度上限+bucketCounts/total。

## Solution

作为限时调参者，≥90% 桶堆积 → 限时该上调。

## User Stories

1. 17160
2. 17161
3. 17162

## Implementation Decisions

- 17163

## Testing Decisions

- 17164

## Out of Scope

- 17165

## Further Notes

- 17166
