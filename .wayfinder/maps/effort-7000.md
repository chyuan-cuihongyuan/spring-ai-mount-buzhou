# Effort #7000 总图 — U 会话 7000 系 50 轮自迭代（能力自补充与自进化第十一弹）

> 会话：U（A–T 字母已占用：T=6000 已收口封卷）；本轮直推 main 逐轮 commit（GitHub 离线时本地逐轮 commit、对账轮补 push）。
> 号段（本地全档实查空闲：specs 7000+ 全空、tickets U7201+ 全空、impl 2252+ 全空；T 系 6000 段已收口封卷）：**efforts #7000–#7049（50 轮）、specs 7000–7049、票 U7201–U7300（每轮 shape+verify 一对，shape=7201+2(N−7000)）、impl 2252–2301（impl = 2252+(N−7000)）**。
> 纪律：每轮四步（map→spec→tickets→implement）+ 双门（模块测试 + 全局三门：覆盖门/快照门/对账门）+ 一轮一 commit；**每 6 轮一对账轮**（U6k：快照批补登 + 全仓离线 verify + 台账核账）；README 纵深行逐轮即时登记（SpecCoverageTest 双向门）。
> 对账门：U1 即落 `USession7000LedgerAuditTest`（TSession6000LedgerAuditTest 同款公式族第八应用）：spec N → shape 票 7201+2(N−7000) / verify=+1 / impl 2252+(N−7000)，spec 起点断言 7000 严格递增。

## Destination

U 会话四步闭环持续推进：从高价值开源项目（>10K stars）借鉴思想，落小纵切组件 + 周期对账轮，全仓三门绿、README/api-surface/MAP/台账四面一致、逐轮 commit。

## 选题原则与借鉴定源（GitHub 高价值项目思想）

已实现带避让：每轮落轮前 grep 快照/README 复核（T-6000 全 50 轮、S-5000/R-4000/Q-3000/P-2000 全量及更早 A–S 系）；雾区候选不抢。本轮候选静脉（占坑即换下一候选）：

- **结构与编码族**：Z 数组（Gusfield 线性前缀面）、分块分解（sqrt decomposition 区间统计）、AVL 树（Adelson-Velsky 首个自平衡 BST）、行程编码（RLE——PNG/传真思想）
- **流式估计族**：HyperLogLog（Redis PF/Presto 基数）、Morris 近似计数（Morris 1978 收支计数）、CUSUM 变点（Page 1954/SPC 质量控制）、Thompson 采样（Thompson 1933 贝叶斯 bandit）、事件时间水位（Flink watermark 乱序缓冲）
- **存储检索族**：跳房子哈希（Herlihy 2008 邻域探测——T 系 Wave 6 遗珠）、Golomb/Rice 码（FLIF/WebP lossless 思想）、写前日志（PostgreSQL WAL/Redis AOF 校验重放）、倒排索引（Lucene/ES posting boolean 面）、MinHash LSH 分桶（near-dup 召回）
- **图算法族**：Bellman-Ford（负权单源+负环检测）、Floyd-Warshall（全对闭包）、Kruskal 最小生成树（并查集协同）、割点/桥（Tarjan DFS 低链接）、倍增 LCA（二倍增祖先跳）
- **几何对齐族**：凸包（Andrew monotone chain）、最近点对（分治剪枝）、点在多边形（射线法）、Bresenham 直线（整数光栅）、Needleman-Wunsch（序列对齐打分）
- **文本身份族**：Manacher 线性回文、Damerau-Levenshtein 双变换编辑距离、Soundex 语音编码（美国普查 1880）、BM25 评分（Okapi/Lucene/ES）、Snowflake 发号器（Twitter 分段 id+时钟回拨守卫）
- **公平治理族**：CFS 虚拟运行时调度（Linux vruntime）、精确滑窗日志限流、Huffman 规范前缀码（DEFLATE 思想）、LWW 寄存器（CRDT last-write-wins）、Cron 五域解析（quartz 思想）
- **治理信任族**：时间桶样本库（Dropwizard 滑动窗）、LSM 键序合并（LevelDB/RocksDB 多路归并+墓碑）、Shamir 门限秘密共享（GF(256)）、Chase-Lev 工作窃取双端队列、xxHash64（Kafka/ZSTD 官方向量）
- **U49 独件**：双端优先队列（min-max heap IntervalHeap——双端 O(1) 最值）

## 已裁决（Decisions so far）

- 号段占用 7000–7049/U7201–U7300/impl 2252–2301（本地全档实查 + T 系收口交接声明）。
- 直推 main 逐轮 commit；每轮定向 `git add` 自有文件；对账轮尝试 push（离线则记「离线」）。
- U1 = 对账门落位轮；U6k（6/12/18/24/30/36/42/48）= 对账轮；U50 = 收口对账轮。
- 全仓 verify 遇已入档满载偶红 flaky 按协议排除重跑（R48 口径）；从根跑为准（T42 入档）。
- T50 交接勘误：T 系对账门系列上界扩至 6050（T37 撞号平移后 effort=6000+T）；U 系公式以 7000 为基无此历史包袱。

## 排程表（滚动追加——Wave 1 先行，后续波次前沿推进后毕业）

| U | effort | 主题 | 票 | impl | 状态 |
|---|---|---|---|---|---|
| U1 | #7000 | 7000 系对账门落位（USession7000LedgerAuditTest 八应用） | U7201–U7202 | 2252 | ✅ |

## Not yet specified（雾区——候选静脉见「借鉴定源」，前沿推进后逐波毕业成排程行）

- Wave 1（U2–U5）：结构与编码族——Z 数组 / 分块分解 / AVL 树 / 行程编码。
- Wave 2（U7–U11）：流式估计族——HyperLogLog / Morris 计数 / CUSUM / Thompson 采样 / 事件时间水位。
- Wave 3（U13–U17）：存储检索族——跳房子哈希 / Golomb-Rice / 写前日志 / 倒排索引 / MinHash LSH。
- Wave 4（U19–U23）：图算法族——Bellman-Ford / Floyd-Warshall / Kruskal / 割点桥 / 倍增 LCA。
- Wave 5（U25–U29）：几何对齐族——凸包 / 最近点对 / 点在多边形 / Bresenham / Needleman-Wunsch。
- Wave 6（U31–U35）：文本身份族——Manacher / Damerau-Levenshtein / Soundex / BM25 / Snowflake。
- Wave 7（U37–U41）：公平治理族——CFS / 精确滑窗 / Huffman / LWW / Cron。
- Wave 8（U43–U47）：治理信任族——时间桶样本库 / LSM 合并 / Shamir / Chase-Lev / xxHash64。
- Wave 9（U49）：双端优先队列（独件）。
- U6/U12/U18/U24/U30/U36/U42/U48 = 对账轮；U50 = 收口对账轮。
- 每轮落轮前 grep 复核是否已被并行会话占坑，占坑即换静脉下一候选。

## Out of scope

- P–T 系余量号段保留各自会话续轮，U 不占用（T-6000 已收口不再占用）。
- P/Q/R/S/T 雾区候选静脉（其 map「借鉴定源」列出的未实现候选）原则上不抢；T 系 Wave 6 遗珠跳房子哈希由本轮 Wave 3 认领。
- 已 mined 带不重复实现（A–T 全系）。
- 修改持久化 SPI 公共签名或跨模块依赖边界（星形白名单硬约束）。
