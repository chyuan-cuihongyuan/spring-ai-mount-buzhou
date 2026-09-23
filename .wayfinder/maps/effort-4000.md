# Effort #4000 总图 — R 会话 4000 系 50 轮自迭代（能力自补充与自进化第八弹）

> 会话：R（A–Q 字母已占用：J=1000/K=1200/L=1400+1700/M=1500/N=1600/O=1800/P=2000/Q=3000）；本轮直推 main 逐轮 commit（GitHub 离线时本地逐轮 commit、对账轮补 push，O 会话离线先例）；启动 2026-09-23。
> 号段（git fetch 不通（GitHub 离线）+ 本地全档实查双验证空闲：specs 3100+ 全空、tickets T5301+ 全空、impl 2101+ 全空；origin/main 与本地同步（status 无 ahead/behind））：**efforts #4000–#4049（50 轮）、specs 4000–4049、票 T6001–T6100（每轮 shape+verify 一对，shape=6001+2(N−4000)）、impl 2101–2150（impl = 2101+(N−4000)）**。
> 纪律：每轮四步（map→spec→tickets→implement）+ 双门（模块测试 + 全局三门：覆盖门/快照门/对账门）+ 一轮一 commit；**每 6 轮一对账轮**（R6k：快照批补登 + 全仓离线 mvn verify + 台账核账）；README 纵深行逐轮即时登记（SpecCoverageTest 双向门）。
> 对账门：R1 即落 `RSession4000LedgerAuditTest`（starter，QSession3000LedgerAuditTest 同款公式族第五应用）：spec N → shape 票 6001+2(N−4000) / verify=+1 / impl 2101+(N−4000)，spec 起点断言 4000 严格递增——预防式对账而非事后补救。

## Destination

50 轮连续 effort（#4000–#4049）全部四步闭环：从高价值开源项目（>10K stars）借鉴思想，落 40 个小纵切 + 9 个对账轮 + 1 个收口轮，全仓三门绿、README/api-surface/MAP/台账四面一致、逐轮 commit（GitHub 可达时 push）。

## 选题原则与借鉴定源（GitHub 高价值项目思想）

已实现带避让：每轮落轮前 grep 快照/README 夘核（Q-3000 全 41 轮、P-2000 全 66 轮、O-1800、L-1700、M-1500 及更早 A–N 系）；Q/P 雾区候选静脉原则上留给 Q/P 续轮，R 不抢。本轮候选静脉（占坑即换下一候选）：

- **素描与统计族**：Count-Min 素材计数（Cassandra/ClickHouse）、Space-Saving 频繁项 top-k（Metwally 2005）、Boyer-Moore 多数表决（MJRTY 1991）、KS 两样本检验（Kolmogorov-Smirnov）
- **编码与数据形态族**：Huffman 前缀码（Huffman 1952）、CRC32C 表驱动校验（Castagnoli/iSCSI）、增量+基准帧列存编码（Parquet/ORC delta）、Crockford Base32（人类可转录）、UUIDv7 时间有序+单调（RFC 9562）、JSON Patch 应用（RFC 6902）、Avro 读写模式解析（schema evolution）、三方合并（git merge-file/diff3）、Rope 文本缓冲（xi-editor）
- **存储引擎族**：区块 min/max 剪枝（DuckDB/Parquet zone map）、尺寸分层合并挑选（Cassandra STC）、Bitcask 键目录合并（Riak 追加日志）、slab 类装箱（Memcached growth factor）、HNSW 贪心层搜索（Malkov 2016/hnswlib）
- **队列与流控族**：CoDel 受控延迟（RFC 8290/fq_codel）、QUIC 反放大窗（RFC 9000 3×）、协作预算让出（tokio coop）、工作窃取对半分割（Cilk/Go scheduler）、OTel 尾采样预算
- **调度与放置族**：拓扑约束放置（K8s topology spread maxSkew）、监督者重启强度（Erlang/OTP max_restarts）、推测执行（Spark straggler 75%）、稳定匹配（Gale-Shapley 1962）、动态 snitch 惩罚（Cassandra badness）
- **定价与协议数族**：EIP-1559 基础费调节（ethereum ±1/8）、难度目标重定（Bitcoin 2016 块钳制）、祖先费率打包（Bitcoin CPFP 子付父）、语义化版本序（semver 2.0 先行版）、Cache-Control 指令裁决（RFC 9111）
- **时间序列与事务族**：标量卡尔曼（预测/更新+增益）、Holt-Winters 季节指数（statsmodels）、Theil-Sen 稳健斜率（scipy 成对斜率中位数）、DDSketch 相对误差分位（Datadog log 桶）、MVCC 快照可见性（Postgres xmin/xmax）
- **治理族**：提交图世代号（Git commit-graph generation）、封禁递升（fail2ban maxretry/bantime）

## 已裁决（Decisions so far）

- 号段占用 4000–4049/T6001–T6100/impl 2101–2150（fetch 不通离线 + 本地全档实查：specs 3100+、tickets T5301+、impl 2101+ 均空；origin/main 同步无超前）。
- 直推 main 逐轮 commit（O 会话离线先例）；每轮定向 `git add` 自有文件；对账轮尝试 push（离线则记「离线」）。
- R1 = 对账门落位轮（RSession4000LedgerAuditTest 四面互证）；R6k（6/12/18/24/30/36/42/48）= 对账轮；R50 = 收口对账轮（全量核账+里程碑）。

## 排程表（滚动追加——Wave 1 先行，后续波次前沿推进后毕业）

| R | effort | 主题 | 票 | impl | 状态 |
|---|---|---|---|---|---|
| R1 | #4000 | 4000 系对账门落位（RSession4000LedgerAuditTest 四面互证） | T6001–T6002 | 2101 | ✅ |
| R2 | #4001 | Count-Min 素描（Cassandra/ClickHouse 流式计数思想） | T6003–T6004 | 2102 | ✅ |
| R3 | #4002 | Space-Saving 频繁项 top-k（Metwally 2005 思想） | T6005–T6006 | 2103 | ✅ |
| R4 | #4003 | Boyer-Moore 多数表决（MJRTY 1991 流式多数思想） | T6007–T6008 | 2104 | ✅ |
| R5 | #4004 | KS 两样本检验（Kolmogorov-Smirnov 分布对比思想） | T6009–T6010 | 2105 | ✅ |
| R6 | #4005 | 对账轮（快照 +4（1129→1133）+ 全仓离线 verify 三门绿；6/50=12%） | T6011–T6012 | 2106 | ✅ |
| R7 | #4006 | Huffman 前缀码（Huffman 1952+deflate 规范码思想） | T6013–T6014 | 2107 | ✅ |
| R8 | #4007 | CRC-32C 校验（Castagnoli/iSCSI/LevelDB 思想） | T6015–T6016 | 2108 | ✅ |
| R9 | #4008 | 增量+基准帧编码（Parquet/ORC 列存 delta 思想） | T6017–T6018 | 2109 | ✅ |
| R10 | #4009 | Crockford Base32（Crockford 2001 人类可转录思想） | T6019–T6020 | 2110 | ✅ |
| R11 | #4010 | UUIDv7 时间有序（RFC 9562+单调计数器思想） | T6021–T6022 | 2111 | ✅ |
| R12 | #4011 | 对账轮（快照 +5（1133→1138）+ 全仓离线 verify 三门绿；12/50=24%） | T6023–T6024 | 2112 | ✅ |
| R13 | #4012 | 区块 min/max 剪枝（DuckDB zone map/Parquet 统计思想） | T6025–T6026 | 2113 | ✅ |
| R14 | #4013 | 尺寸分层合并挑选（Cassandra STC 思想） | T6027–T6028 | 2114 | ✅ |
| R15 | #4014 | Bitcask 键目录合并（Riak 追加日志思想） | T6029–T6030 | 2115 | ✅ |
| R16 | #4015 | Slab 类装箱（Memcached slab allocator 思想） | T6031–T6032 | 2116 | ✅ |
| R17 | #4016 | HNSW 贪心层搜索（Malkov 2016/hnswlib 思想） | T6033–T6034 | 2117 | ✅ |
| R18 | #4017 | 对账轮（快照 +5（1138→1143）+ 全仓离线 verify 三门绿；18/50=36%） | T6035–T6036 | 2118 | ✅ |
| R19 | #4018 | CoDel 受控延迟（RFC 8290/fq_codel 思想） | T6037–T6038 | 2119 | ✅ |
| R20 | #4019 | QUIC 反放大窗（RFC 9000 §8.1 防放大思想） | T6039–T6040 | 2120 | ✅ |
| R21 | #4020 | tokio 协作预算（coop budget 让出思想） | T6041–T6042 | 2121 | ✅ |
| R22 | #4021 | 工作窃取对半分割（Cilk/Go scheduler steal-half 思想） | T6043–T6044 | 2122 | ✅ |
| R23 | #4022 | 尾采样策略（OTel tail-based sampling 思想） | T6045–T6046 | 2123 | ✅ |
| R24 | #4023 | 对账轮（快照 +5（1143→1148）+ 全仓离线 verify 三门绿；24/50=48%） | T6047–T6048 | 2124 | ✅ |
| R25 | #4024 | 拓扑约束放置（K8s maxSkew 思想） | T6049–T6050 | 2125 | ✅ |
| R26 | #4025 | 监督者重启强度（Erlang/OTP max_intensity 思想） | T6051–T6052 | 2126 | ✅ |
| R27 | #4026 | 推测执行裁决（Spark speculative execution 思想） | T6053–T6054 | 2127 | ✅ |

## Not yet specified（雾区——候选静脉见「借鉴定源」，前沿推进后逐波毕业成排程行）

- Wave 2（R7–R11）：编码族——Huffman / CRC32C / 增量基准帧 / Crockford Base32 / UUIDv7。
- Wave 3（R13–R17）：存储引擎族——ZoneMap 剪枝 / 尺寸分层合并 / Bitcask 合并 / slab 装箱 / HNSW。
- Wave 4（R19–R23）：队列流控族——CoDel / QUIC 反放大 / tokio 协作预算 / 工作窃取 / OTel 尾采样。
- Wave 5（R25–R29）：调度放置族——拓扑散布 / OTP 重启强度 / Spark 推测 / 稳定匹配 / 动态 snitch。
- Wave 6（R31–R35）：定价协议族——EIP-1559 / 难度重定 / 祖先费率 / semver 序 / JSON Patch。
- Wave 7（R37–R41）：时序事务族——卡尔曼 / Holt-Winters / Theil-Sen / DDSketch / MVCC 快照。
- Wave 8（R43–R47）：图文本治理族——三方合并 / 提交图世代 / Rope / Avro 模式解析 / fail2ban 递升。
- Wave 9（R49）：Cache-Control 裁决；R50 = 收口对账轮。
- 每轮落轮前 grep 夘核是否已被并行会话占坑，占坑即换静脉下一候选。

## Out of scope

- Q-3000 系余量号段（#3041–#3149 / specs 3041–3149 / T5083–T5300 / impl 2042–2150）保留给 Q 会话续轮，R 不占用。
- Q/P 雾区候选静脉（其 map「借鉴定源」列出的未实现候选）原则上不抢。
- L-1700/M-1500/O-1800 已 mined 带不重复实现。
- 修改持久化 SPI 公共签名或跨模块依赖边界（星形白名单硬约束）。
