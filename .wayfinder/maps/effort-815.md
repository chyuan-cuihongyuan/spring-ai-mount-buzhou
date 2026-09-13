# effort #815 — Spill 写放大读数

- 会话：H 会话 800 系第 16 轮 ｜ spec [815](../../../docs/spec/815-spill-write-amplifier.md) ｜ 票 [T1131](../tickets/T1131-spill-write-amplifier.md)/[T1132](../tickets/T1132-spill-write-amplifier-verify.md) ｜ impl568
- 借鉴：RocksDB compaction stats（facebook/rocksdb ≈30K star）——bytes_written/bytes_logical 写放大口径

## 勘察（排重）

- SpillPairAudit/SpillPairHealth（707/728）：配对完整性——无字节放大维。
- SpillQuota：总量配额非放大率。
- grep -i `amplif|write.*bytes`：无命中。

## 决定

`SpillWriteAmplifier`（spill，记账脑）：recordWrite(logical, physical)——total counters（writes/logicalTotal/physicalTotal/amplificationRatio）+近窗 64 样本环（recentRatio 均值+recentP95Ratio 最近秩）；逻辑 ≤0 忽略（不制造 ∞ 假象）；物理负值忽略。喂点=store（data+meta）/markLinked（meta 重写）调用方。零写入全 0 空真。

## 测试

store+markLinked 链路放大率 1.3 精确/近窗滑动反映当前行为（早期 1.0 被挤出、总均值被稀释断言）/零负忽略/空真/混排 P95=5.0+近窗均值 1.3125（首跑窗口账笔误修正：后 64 样本=59×1.0+5×5.0）——5 例绿。

## 诚实边界

记账脑不挂钩 DiskSpillStore（侵入 store 主路径违背读数纪律——喂点归装配侧装饰，同 810 模式）；字符口径（sizeChars）与字节口径由喂点统一；ratio 是商不是差（meta 开销占比可由 (physical-logical)/physical 推导——不重复发口径）。
