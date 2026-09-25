---
id: T6241
title: T 会话 T21 Geohash 地理哈希的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

位置邻近怎么用前缀剪枝？（spec 6020 /
effort #6020 / T21）

## Resolution

**Geohash（core/policy，源码 T18 预载）**：经纬交替二分
5 位 Base32——前缀共享即邻近；decode 包围盒+中心互逆；
lat/lon/精度/字符集 fail-fast。
