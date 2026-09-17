---
id: T5039
title: Q 会话 R20 TTL 抖动的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

同批键同时到期的雷群怎么免疫且副本一致？（spec 3019 / effort #3019 / R20）

## Resolution

**TtlJitter（core/cache，纯函数）**：按键确定性抖动——r∈[−1,1) 由
键哈希（复用 DeterministicHash）派生，TTL=base×(1+r·jitter) 带宽
夹持+1ms 兜底。同一键跨实例恒同 TTL（副本一致+到期错开两得），
不同键带宽内铺开——随机抖动副本漂移病与固定 TTL 雷群病双根治。
