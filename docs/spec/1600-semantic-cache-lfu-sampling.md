# 1600 · 语义缓存 LFU 采样驱逐（Redis allkeys-lfu 思想）

> 来源：N 会话 R1（effort #1600 / T2351–T2352 / impl 1153）。借鉴对象：Redis
> `maxmemory-policy allkeys-lfu` + `maxmemory-samples`（>10K star 项目）——驱逐时不再盲取
> eldest，而是采样 N 个候选、淘汰其中访问计数最低者，保护热条目不被一次性扫描负载冲刷。

## Problem Statement

语义缓存（spec 55 / spec 701）当前驱逐策略是纯 LRU：容量超限与权重预算腾挪都取 eldest
（最久未访问者）。经典 LRU 缺陷在这个负载下会真实发生：FAQ 型热条目只要一段时间未被查询，
一批一次性扫描式写入（put 后永不命中）就会把它挤出缓存——热条目的价值远高于 scan 污染
条目，但驱逐算法看不见「命中次数」这个信号。

## Solution

给 `SemanticCacheStore` 增加可选的 **LFU 采样驱逐**：驱逐时从 LRU 序（eldest 侧）取前
`evictionSampleSize` 个候选组成采样窗口，淘汰窗口内**命中计数最低**者（平局取更老者，
保持 LRU 底线）；窗口内被跳过的 eldest 若命中数更高即视为「热条目被保护」计数。
默认 `evictionSampleSize=0`（关）——行为与现状完全一致，opt-in。

条目命中计数：`findNearest` 命中路径 +1，封顶 `1_000`（> 【推演】Redis LFU 用 255 上限
+ 概率对数递增 + 周期衰减；本缓存条目量级数百、TTL 分钟~小时级天然兜底衰减，简化为线性
计数 + 封顶，测试可控且语义足够）。新条目初始计数 0（写入本身不代表查询价值；scan 污染
条目在窗口内天然先于被命中过的条目出局）。

## User Stories

1. 作为运维者，我想让语义缓存驱逐时看见命中频率，所以热 FAQ 条目不会被一次性扫描写入冲刷出缓存。
2. 作为运维者，我想保持默认行为零变化，所以不配置新参数时缓存驱逐语义与升级前完全一致。
3. 作为运维者，我想观测采样驱逐是否真的在保护热条目，所以需要「热条目被保护次数」读数。
4. 作为开发者，我想配错参数时启动即失败，所以负采样数要 fail-fast 抛出带修法的异常。
5. 作为运维者，我想让权重预算腾挪（spec 701）与容量驱逐同享频率信号，所以两条驱逐路径统一采样语义。
6. 作为开发者，我想命中计数不无界增长，所以计数有封顶常量、超出不再递增。

## Implementation Decisions

- `SemanticCacheStore` 新增构造参数 `evictionSampleSize`（≤0 = 关，语义同 1 即纯 eldest）；
  旧构造器全部保留并委托默认 0——既有调用方零改动。
- `CacheEntry` 增加命中计数字段（可变计数器内嵌 record，读写均在 synchronized 方法内）。
- 驱逐统一收敛到私有 `evictOne`：容量超限路径与权重预算腾挪路径共用；采样窗口 =
  迭代序（LRU 序）前 `max(1, sampleSize)` 个。
- 计数与观测：`evictions` / `weightEvictions` 口径不变；新增 `hotPreservedCount()`
  （采样生效使非 eldest 被淘汰的次数——窗口 eldest 命中数严格大于被选victim时 +1）。
- 配置面 `buzhou.resilience.semantic-cache.eviction-sample-size`（默认 0=关，校验 ≥0）；
  `ResilienceProperties.SemanticCache` 主构造扩参，既有兼容构造委托新参 0。
- 装配点 `ResilienceModule` 传参至 store 构造。

## Testing Decisions

- 新测试类 `SemanticCacheLfuEvictionTest`（行为面只测外部可观察行为）：
  - 默认关：满容量驱逐仍取 eldest（高频触达老条目后写入新条目，老条目仍出局）——零变化断言。
  - opt-in：老条目高频触达 + 采样开 → 驱逐淘汰窗口内低频新条目而非高频老条目；`hotPreservedCount` ≥1。
  - 命中计数封顶：反复命中后计数停在封顶值（经由驱逐窗口行为间接断言或读数断言）。
  - 权重预算腾挪路径同款采样语义。
  - 参数校验：负 `evictionSampleSize` 构造抛 `IllegalArgumentException`；属性组负值抛同款。
- 属性组校验沿用 `ResilienceProperties` 现有 compact constructor fail-fast 风格；
  装配测试沿用 `ChunkingEmbeddingAssemblyTest` 构造传参断言风格。
- Prior art：`SemanticCacheWeightBudgetTest`（权重腾挪路径）、`SemanticCacheStoreTest`（构造校验）。

## Out of Scope

- `ResponseCacheStore`（精确 key LRU）的同款采样驱逐——若要做另立 spec。
- Redis 式概率对数计数与周期衰减（简化线性计数已满足本量级，见 Solution 推演注）。
- TinyLFU/Count-Min Sketch 频率准入（admission）：语义缓存 key 不可精确哈希（相似≠相同），
  准入思想不适配，不做。

## Further Notes

- 与 spec 701（权重预算驱逐）正交叠加：腾挪循环里每一跳都走采样选择。
- 观测指标建议（后续健康面轮可接）：`hotPreservedCount / evictions` 比值 = 采样驱逐的实效率。
