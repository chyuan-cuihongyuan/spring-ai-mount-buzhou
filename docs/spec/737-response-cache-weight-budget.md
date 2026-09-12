# 737 — 响应缓存权重预算

> 来源：G 会话第 38 轮 = effort #737（701 权重预算在 ResponseCacheStore 的对称落地）/ [T1074](../../.wayfinder/tickets/T1074-response-cache-weight.md) / [T1075](../../.wayfinder/tickets/T1075-response-cache-weight-verify.md) / impl 637。

## Problem

精确响应缓存（ResponseCacheStore）与语义缓存同构：按条数 LRU——大响应与小响应同权，长文几次注入挤出高频短条目；内存无预算口径。701 只给语义缓存做了权重预算。

## Solution

701 同款（Caffeine weigher）对称落地：

- 构造器扩 `maxWeightChars`（默认 0=关零行为）；条目权重=响应字符数（复用 SemanticCacheStore.estimateChars 同口径——两缓存口径一致）。
- put：替换同键先回收旧权重；超预算单条拒存（weightEvictions 计数）；写入后腾挪 eldest 至预算内。
- TTL 过期即弃（get 路径）同步回收权重。
- readouts：`maxWeightChars()` / `totalWeightChars()` / `weightEvictionCount()`——独立于 evictedCount 口径。

## Out of Scope

yml 装配（response-cache 配置组接线留后续——与 723 同模式）；token 级权重。
