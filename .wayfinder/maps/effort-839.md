# effort #839 — 泄漏疑似对象聚合器

- 会话：H 会话 800 系第 40 轮 ｜ spec [839](../../../docs/spec/839-leak-suspect-aggregator.md) ｜ 票 [T1179](../tickets/T1179-leak-suspect-aggregator.md)/[T1180](../tickets/T1180-leak-suspect-aggregator-verify.md) ｜ impl592
- 借鉴：资源泄漏检测聚合思想（S9 备选池启用——R40 原题滚动导出统计半撞：RollingJsonlWriter 已有 rotations/rotationFailures 计数）

## 勘察（排重）

- RollingJsonlWriter：rotations/rotationFailures 已有（换题）。
- ResourceLeakDetector：LeakListener 逐条报告——无聚合排行。
- LeakDetectorHolder：装配持有——无聚合面。
- grep -i `leak.*aggregat|suspect`：无命中。

## 决定

`LeakSuspectAggregator`（core.leak，实现 LeakListener）：onLeak 报告按描述键（截 64 字符稳键）聚合 count/maxAge/lastSeen——键封顶 32 超限并入 overflow 桶；snapshot count 降序典序破平+totalLeaks+truncated；null/空白报告忽略。可直接挂检测器监听（检测器零变更）。同键并发：长键聚合计数锁 per-agg 数组。

## 测试

同键计数+最大龄+排行/长描述截 64 稳键去重/溢出桶 32+1+truncated/脏报告三形态+空真——4 例全绿。

## 诚实边界

描述键粒度归检测器（动态后缀会分裂键——截断稳键缓解非根治）；不阻止泄漏（聚合面）；per-agg 锁非全局（并发正确性优先于全局快照原子性——快照近似口径）。
