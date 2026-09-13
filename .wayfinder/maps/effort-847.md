# effort #847 — 数据集近重复读数

- 会话：H 会话 800 系第 48 轮 ｜ spec [847](../../../docs/spec/847-dataset-near-duplicate.md) ｜ 票 [T1195](../tickets/T1195-dataset-near-duplicate.md)/[T1196](../tickets/T1196-dataset-near-duplicate-verify.md) ｜ impl600
- 借鉴：Cleanlab 数据质量思想（cleanlab/cleanlab ≈10K）；714 字符 trigram Jaccard 同源扩散

## 勘察（排重）

- EvalDatasetMeta/Csv：数据集存取+标签——近重复对账缺位。
- 714 相似度判定器：判定执行（eval 时）——数据集治理面不同域。
- grep -i `duplicate|dedup`：EventDeduplicator 事件域——数据集域缺位。

## 决定

`DatasetNearDuplicateStats`（core.eval，纯函数）：analyze(items, threshold)——trigram Jaccard 两两对账+并查集成簇：duplicatePairs（明细封顶 32 对 #i~#j）/largestCluster/uniqueRatio（簇根数/条数）/itemsConsidered vs itemsTotal（条目封顶 200 截断+truncated 如实——O(n²) 诚实口径）；threshold (0,1] fail-fast；脏条目跳过但计 itemsTotal。

## 测试

精确重复成簇（3 选 2 对+unique 0.5）/近重复阈值 0.8 一对/全唯一率 1.0/条目封顶 200 截断+truncated/脏条目跳过但计数/阈值 fail-fast——6 例全绿。

## 诚实边界

O(n²) 两两比较（封顶 200 截断如实——大集先采样）；trigram 字符级（语义近重复非目标）；簇=传递闭包（链式相似会并大簇——口径声明）。
