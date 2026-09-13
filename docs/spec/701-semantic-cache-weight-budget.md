# 701 — 语义缓存权重预算驱逐

> 来源：G 会话第 2 轮 = effort #701（55 语义缓存驱逐策略深化）/ [T1002](../../.wayfinder/tickets/T1002-semantic-weight-budget.md) / [T1003](../../.wayfinder/tickets/T1003-semantic-weight-budget-verify.md) / impl 601。

## Problem

语义缓存驱逐按条数（maxEntries）：一条 50KB 长文响应与一条 200 字 FAQ 同权。长文注入几次就能把整批高频短 FAQ 挤出缓存——命中率塌方而 maxEntries 配置毫无异常。缓存内存占用也无预算口径（128 条 × 无界响应体 = 无界内存）。

## Solution

Caffeine weigher 思想（ben-manes/caffeine ≈16K star：`weigher` + `maximumWeight`）：

- `SemanticCache` 增 `maxWeightChars`（默认 0=关——零行为变化，opt-in）。
- 条目权重 = 响应文本字符数（各 Generation `getText()` 求和，null 安全）。
- put 后若 `totalWeight > maxWeightChars`：从 eldest 起腾挪直至 ≤ 预算；`weightEvictions()` 独立计数（**不混** LRU/TTL 的 `evictedCount` 口径——驱逐原因可分面排障）。超预算单条（权重>预算）拒存，同计 weightEvictions（口径=「因权重预算未存留总次数」）。
- maxEntries 条数上限保留（硬顶双保险——权重关或失灵时仍封顶条数）。
- readout：`maxWeightChars()` / `totalWeightChars()` / `weightEvictions()`。
- 装配：`buzhou.resilience.semantic-cache.max-weight-chars`（metadata json 登记——ConfigDoctor 已知键宇宙同步）。

## User Stories

1. 长文挤占：开启 max-weight-chars 后，50KB 长文触发腾挪，高频 FAQ 存活，命中率回升可观测（weightEvictions 增长 vs hits 回升）。
2. 内存预算：预算字符数即缓存体量上界（估算口径），容量规划有数可依。

## Implementation Decisions

- record 规范构造扩 5 组件 + 显式 `@ConstructorBinding`（多构造器绑定坑——R39/R48 两次踩中）；4 参兼容构造保留（既有直构调用零破坏）。
- 权重估算用字符数（非 token/字节）——口径稳定可解释；估算值缓存于条目（写入时一次计算）。
- 腾挪最坏 O(n)/写——条目量级数百与既有线性扫描同量级（perf 哨兵同域）。

## Testing Decisions

- 腾挪序：预算 100，写入 60+60（第二条触发腾挪 eldest）→ size=1、weightEvictions=1、totalWeight=60。
- 超预算拒存：预算 50 写 80 → 不存、size=0、weightEvictions=1。
- 默认 0：不腾挪不限重——与既有行为逐字节一致（size 上限仍 maxEntries）。
- readout 一致性：totalWeightChars 随 put/驱逐增减精确。

## Out of Scope

- ResponseCacheStore 同策略扩散（族内后续轮）。
- token 级精确权重（tokenizer 依赖不值得）。
- 按桶分级预算（单桶配额语义复杂度不抵收益）。

## Further Notes

借鉴定源：caffeine（≈16K）weigher 语义——「容量按权重不按条数」；L 系文献同思想（Redis maxmemory 按 bytes）。
