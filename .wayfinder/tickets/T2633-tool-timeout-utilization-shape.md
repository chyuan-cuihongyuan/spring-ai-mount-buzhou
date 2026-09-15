---
id: T2633
title: 工具超时余量直方的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

ToolTimeoutUtilization 的形状怎么裁决？（spec 1716 / effort #1716 / R17）（spec 1716 验收/裁决）

## Resolution

实例面桶式：利用率 ratio=duration/limit（limit≤0 忽略），默认边界 {0.25,0.5,0.75,0.90,1.0}→6 桶（<25%/<50%/<75%/<90%/<100%/≥100% 超时档）+maxRatio 千分精度——Envoy 超时利用率思想，贴线桶堆积=限时该调。
