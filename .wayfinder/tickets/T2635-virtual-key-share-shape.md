---
id: T2635
title: 虚拟键份额读面的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

VirtualKeyShareStats 的形状怎么裁决？（spec 1717 / effort #1717 / R18）（spec 1717 验收/裁决）

## Resolution

实例面：record(key)（null/空按 _anonymous_ 桶）+census→ShareCensus(total/份额降序 LinkedHashMap 保序（unmodifiableMap 非 Map.copyOf——copyOf 丢序）/HHI Σ份额² 0..1 无样本 −1)+resetForTest——OpenRouter 多键遥测+经济学 HHI 集中度。
