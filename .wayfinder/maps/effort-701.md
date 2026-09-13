# effort #701 — 语义缓存权重预算驱逐

- 会话：G 会话 700 系第 2 轮 ｜ spec [701](../../../docs/spec/701-semantic-cache-weight-budget.md) ｜ 票 [T1002](../tickets/T1002-semantic-weight-budget.md)/[T1003](../tickets/T1003-semantic-weight-budget-verify.md) ｜ impl601
- 借鉴：Caffeine（ben-manes/caffeine ≈16K star）weigher+maximumWeight——按条目权重（而非条数）驱逐

## 勘察（排重）

- SemanticCacheStore（55）：LRU 按条数（maxEntries=128）——大响应与抖动条目同权，几百条短 FAQ 能被几十条长文反复挤出。
- 611 维度漂移/639 hitRate 是读数面；驱逐策略本身未深化。
- eviction 相关 13 处命中均为各面自有计数，无权重预算族。

## 决定

`maxWeightChars`（默认 0=关，opt-in）：条目权重=响应文本字符数（多 Generation 求和）；put 后腾挪至 totalWeight≤预算（eldest 先出，weightEvictions 独立计数不混 LRU/TTL 口径）；超预算单条拒存同计（javadoc 声明口径）。maxEntries 条数上限保留（硬顶双保险）。SemanticCache record 扩规范构造 5 组件+@ConstructorBinding（R39/R48 坑位规避），4 参兼容委托；yml `buzhou.resilience.semantic-cache.max-weight-chars` + metadata 登记面。

## 测试

权重腾挪序/超预算拒存/默认 0 零行为/计数口径分离+readout。

## 诚实边界

权重=字符数估算（非 token/字节精确值——口径一致性比精确性重要）；腾挪最坏 O(n) 每写（条目量级数百，线性扫描已有 perf 哨兵同量级）。
