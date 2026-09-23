# Effort #5000 总图 — S 会话 5000 系 50 轮自迭代（能力自补充与自进化第九弹）

> 会话：S（A–R 字母已占用：R=4000 已收口）；本轮直推 main 逐轮 commit（GitHub 离线时本地逐轮 commit、对账轮补 push）。
> 号段（本地全档实查空闲：specs 5000+ 全空、tickets T6101+ 全空、impl 2151+ 全空；R 系 4000 段已收口封卷）：**efforts #5000–#5049（50 轮）、specs 5000–5049、票 T6101–T6200（每轮 shape+verify 一对，shape=6101+2(N−5000)）、impl 2151–2200（impl = 2151+(N−5000)）**。
> 纪律：每轮四步（map→spec→tickets→implement）+ 双门（模块测试 + 全局三门：覆盖门/快照门/对账门）+ 一轮一 commit；**每 6 轮一对账轮**（S6k：快照批补登 + 全仓离线 verify + 台账核账）；README 纵深行逐轮即时登记（SpecCoverageTest 双向门）。
> 对账门：S1 即落 `SSession5000LedgerAuditTest`（RSession4000LedgerAuditTest 同款公式族第六应用）：spec N → shape 票 6101+2(N−5000) / verify=+1 / impl 2151+(N−5000)，spec 起点断言 5000 严格递增。

## Destination

S 会话四步闭环持续推进：从高价值开源项目（>10K stars）借鉴思想，落小纵切组件 + 周期对账轮，全仓三门绿、README/api-surface/MAP/台账四面一致、逐轮 commit。

## 选题原则与借鉴定源（GitHub 高价值项目思想）

已实现带避让：每轮落轮前 grep 快照/README 复核（R-4000 全 50 轮、Q-3000/P-2000 全量及更早 A–N 系）；Q/P/R 雾区候选不抢（R 的 Cache-Control/HNSW 等已落）。本轮候选静脉（占坑即换下一候选）：

- **采样与统计族**：蓄水池抽样（Vitter R，Spark sample）、Roaring bitmap 压缩位图（Lucene/Spark）、指数直方图滑窗计数（Datar-Gionis；P 系指数直方图为 HDR 分位面，本件为滑窗计数面——同族不同面）、MinHash 近似 Jaccard（近重复族与 SimHash 不同面）
- **一致性与共识族**：Rendezvous/HRW 哈希（Cassandra 副本选择）、Jump consistent hash（Google）、有界负载一致哈希（Google vultr）、向量时钟因果序（Dynamo）、Lamport 时钟全序（分布式观测）
- **存储引擎族**：LSM leveled 压实挑选（RocksDB/Cassandra LCS——R14 为 size-tiered 不同面）、Merkle 树反熵对账（Dynamo/Cassandra）、fencing token 世代令牌（Chubby/Kleppmann）、clock-sweep 缓存驱逐（PostgreSQL 缓存）、GCRA 漏桶计量（ATM/Redis cell）
- **执行与并发族**：Disruptor 环形缓冲（LMAX）、ticket lock 公平排队（Linux 内核）、seqlock 读侧一致（Linux 内核）、epoch 回收（EBR 并发内存）、延迟调度 locality wait（Spark）
- **治理族**：hinted handoff 暂代投递（Cassandra）、ISR 同步副本伸缩（Kafka）、deadline 传播预算（gRPC）、自适应并发梯度限流（Netflix gradient2）、doublewrite 双写缓冲（InnoDB）

## 已裁决（Decisions so far）

- 号段占用 5000–5049/T6101–T6200/impl 2151–2200（本地全档实查 + R 系收口交接声明）。
- 直推 main 逐轮 commit；每轮定向 `git add` 自有文件；对账轮尝试 push（离线则记「离线」）。
- S1 = 对账门落位轮；S6k（6/12/18/24/30/36/42/48）= 对账轮；S50 = 收口对账轮。
- 全仓 verify 遇已入档满载偶红 flaky 按协议排除重跑（R48 口径）；-rf 续跑不满足快照门全 reactor 口径——从根跑为准（R48 入档）。

## 排程表（滚动追加——Wave 1 先行，后续波次前沿推进后毕业）

| S | effort | 主题 | 票 | impl | 状态 |
|---|---|---|---|---|---|
| S1 | #5000 | 5000 系对账门落位（SSession5000LedgerAuditTest 六应用） | T6101–T6102 | 2151 | ✅ |
| S2 | #5001 | Roaring 压缩位图（容器化自适应压缩思想） | T6103–T6104 | 2152 | ✅ |
| S3 | #5002 | Fisher-Yates 无偏洗牌（Durstenfeld 变体思想） | T6105–T6106 | 2153 | ✅ |
| S4 | #5003 | 有界负载一致哈希（Google bounded-loads 思想） | T6107–T6108 | 2154 | ✅ |
| S5 | #5004 | Fencing Token 世代令牌护栏（Chubby/Kleppmann 界碑思想） | T6109–T6110 | 2155 | ✅ |
| S6 | #5005 | 对账轮（快照 +4（1169→1173）+ 全仓 verify 三门绿；6/50=12%） | T6111–T6112 | 2156 | ✅ |
| S7 | #5006 | Ticket Lock 票据锁（Linux 内核 FIFO 公平自旋思想） | T6113–T6114 | 2157 | ✅ |
| S8 | #5007 | Seqlock 序号锁（Linux 内核奇偶标写入期思想） | T6115–T6116 | 2158 | ✅ |
| S9 | #5008 | Disruptor 环形缓冲（LMAX 预分配+序标两段式思想） | T6117–T6118 | 2159 | ✅ |
| S10 | #5009 | Clock-Sweep 缓存驱逐（PostgreSQL 使用计数+环形指针思想） | T6119–T6120 | 2160 | ✅ |
| S11 | #5010 | Hinted Handoff 暂代投递（Cassandra 记账回放思想） | T6121–T6122 | 2161 | ✅ |
| S12 | #5011 | 对账轮（快照 +5（1173→1178）+ 全仓 verify 三门绿；12/50=24%） | T6123–T6124 | 2162 | ✅ |
| S13 | #5012 | DoubleWrite 双写缓冲（InnoDB 崩溃恢复暂存思想） | T6125–T6126 | 2163 | ✅ |
| S14 | #5013 | Group Commit 组提交（WAL 一次落盘合并多事务思想） | T6127–T6128 | 2164 | ✅ |
| S15 | #5014 | 延迟调度（Spark locality wait 预算思想） | T6129–T6130 | 2165 | ✅ |
| S16 | #5015 | SIEVE 缓存驱逐（2024 论文 lazy promotion 思想） | T6131–T6132 | 2166 | ✅ |
| S17 | #5016 | Two-Phase Commit 协调器（prepare 投票+裁决状态机思想） | T6133–T6134 | 2167 | ✅ |
| S18 | #5017 | 对账轮（快照 +5（1178→1183）+ 全仓 verify 三门绿；18/50=36%） | T6135–T6136 | 2168 | ✅ |
| S19 | #5018 | Read Repair 读修复（Dynamo 读时对账思想） | T6137–T6138 | 2169 | ✅ |
| S20 | #5019 | 热点 Key 探测器（确定性采样+阈值告警思想） | T6139–T6140 | 2170 | ✅ |
| S21 | #5020 | Bounded Mailbox 有界信箱（Akka 溢出策略思想） | T6141–T6142 | 2171 | ✅ |
| S22 | #5021 | Sparse Index 稀疏索引（LSM 块首键二分定位思想） | T6143–T6144 | 2172 | ✅ |
| S23 | #5022 | 区间树 stabbing 查询（CLRS 居中区间树思想） | T6145–T6146 | 2173 | ✅ |
| S24 | #5023 | 对账轮（快照 +5（1183→1188）+ 全仓 verify 三门绿；24/50=48%） | T6147–T6148 | 2174 | ✅ |
| S25 | #5024 | MemTable 内存表（LSM 可变表+满表滚动思想） | T6149–T6150 | 2175 | ✅ |
| S26 | #5025 | Skip List 跳跃表（Pugh 概率多层链思想） | T6151–T6152 | 2176 | ✅ |
| S27 | #5026 | 平滑加权轮询（nginx smooth WRR 思想） | T6153–T6154 | 2177 | ✅ |
| S28 | #5027 | Deficit Round Robin 亏空调度（Shreedhar-Varghese 思想） | T6155–T6156 | 2178 | ✅ |
| S29 | #5028 | Segment Log 分段日志（Kafka 段滚动+最旧段淘汰思想） | T6157–T6158 | 2179 | ✅ |

## Not yet specified（雾区——候选静脉见「借鉴定源」，前沿推进后逐波毕业成排程行）

- Wave 1（S2–S5）：采样统计族——蓄水池抽样 / Roaring bitmap / 指数直方滑窗计数 / MinHash。
- Wave 2（S7–S11）：一致性共识族——Rendezvous 哈希 / Jump hash / 有界负载 / 向量时钟 / Lamport 时钟。
- Wave 3（S13–S17）：存储引擎族——leveled 压实 / Merkle 反熵 / fencing token / clock-sweep / GCRA。
- Wave 4（S19–S23）：执行并发族——Disruptor 环 / ticket lock / seqlock / epoch 回收 / 延迟调度。
- Wave 5（S25–S29）：治理族——hinted handoff / ISR 伸缩 / deadline 预算 / 自适应并发 / doublewrite。
- S6/S12/S18/S24 = 对账轮；S30–S50 后续波次（雾区）。
- 每轮落轮前 grep 复核是否已被并行会话占坑，占坑即换静脉下一候选。

## Out of scope

- Q/P/R 系余量号段保留各自会话续轮，S 不占用（R-4000 已收口不再占用）。
- Q/P/R 雾区候选静脉（其 map「借鉴定源」列出的未实现候选）原则上不抢。
- 已 mined 带不重复实现（A–R 全系）。
- 修改持久化 SPI 公共签名或跨模块依赖边界（星形白名单硬约束）。
