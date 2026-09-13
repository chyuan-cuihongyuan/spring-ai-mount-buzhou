# 837 — 限流键热点读数

> 来源：H 会话第 39 轮（补位轮）= effort #837 / [T1177](../../.wayfinder/tickets/T1177-ratelimit-key-hotspot.md) / [T1178](../../.wayfinder/tickets/T1178-ratelimit-key-hotspot-verify.md) / impl 591。
> 借鉴：Envoy per-connection rate limit 键域观测。
> 补位注记：R38 跳号致 effort 837 缺位——本轮即填补（G 会话 spec745 教训即时应用）。

## Problem

虚拟密钥接入后限流键域（模型×维度×key）膨胀：「额度被谁消耗/有无热点键独占」无热力面——TagCardinalityGuard 管指标标签不管限流键。

## Solution

`RateLimitKeyHotspot`（resilience.ratelimit，纯读数）：

- **键聚合**：record(key, amount, atMillis)——requests/amountSum/lastSeen；键封顶 128 超限并入溢出桶。
- **排行**：top(n) requests 降序典序破平；totalRequests/distinctKeys。
- **精度**：amount 累计走 ×1000 毫账 AtomicLong（免 double CAS 竞争）。

## 兼容性

纯新增；RateLimitBackend SPI 零变更（喂点=策略层装配侧）。

## 诚实边界

键语义/amount 单位归调用方；溢出桶不可回溯；喂点手动。
