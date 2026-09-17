# Effort #3000 总图 — Q 会话 3000 系 150 轮自迭代（能力自补充与自进化第七弹）

> 会话：Q（A–P 字母已占用：J=1000/K=1200/L=1400+1700/M=1500/N=1600/O=1800/P=2000）；本轮**直推 main 逐轮 push**（K/O/P 会话先例，用户常设授权 GitHub 自动提交）；启动 2026-09-18。
> 号段（git fetch 已通（origin/main 与本地同步无超前）+ 本地全档实查双验证空闲：maps/specs 2500+ 全空、tickets T4001+ 全空、impl 1701+ 全空）：**efforts #3000–#3149（150 轮）、specs 3000–3149、票 T5001–T5300（每轮 shape+verify 一对，shape=5001+2(N−3000)）、impl 2001–2150（impl = 2001+(N−3000)）**。
> 纪律：每轮四步（map→spec→tickets→implement）+ 双门（模块测试 + 全局三门：覆盖门/快照门/对账门）+ 一轮一 commit 一 push；**每 6 轮一对账轮**（R6k：快照批补登 +5 + 全仓 mvn verify + 台账核账）；README 纵深行逐轮即时登记（SpecCoverageTest 双向门）。
> 对账门：R1 即落 `QSession3000LedgerAuditTest`（starter，PSession2000LedgerAuditTest 同款公式族第四应用）：spec N → shape 票 5001+2(N−3000) / verify=+1 / impl 2001+(N−3000)，spec 起点断言 3000 严格递增——预防式对账而非事后补救。

## Destination

150 轮连续 effort（#3000–#3149）全部四步闭环：从高价值开源项目借鉴思想，在十大机制上落 ~125 个小纵切（调度公平/缓存驱逐/序与因果/流式统计/退避探测/文本检索/图与聚合/批量流控/LLM 采样族，避开 P-2000 已实现带与 P 雾区候选静脉）+ 25 个对账轮（R6k），全仓三门绿、README/api-surface/MAP/台账四面一致、逐轮 push GitHub。

## 选题原则与借鉴定源（GitHub 高价值项目思想）

已实现带避让：每轮落轮前 grep 快照/README 夘核（P-2000 全 66 轮、O-1800、L-1700、M-1500 及更早 A–N 系）。P 雾区候选静脉（GOAWAY/Raft 快照截断/etcd lease/Little's law/t-digest/Zstd/G1 去重/RabbitMQ ack timeout/SQLite busy/Prometheus staleness/LangGraph diff/MemGPT 冲突/SGLang radix/Aeron/BoltDB/SRE 分页抑制等）原则上留给 P 续轮，Q 不抢。本轮候选静脉（占坑即换下一候选）：

- **调度族**：CFS vruntime 有序公平、stride/lottery 彩票调度、EDF 最早截止期、MLFQ 多级反馈、indexed binary heap（decrease-key）、时间轮（Netty/Kafka hashed wheel）、deadline queue 惰性取消、SFQ 起始时间公平队列
- **缓存驱逐族**：ARC 自适应替换、2Q、LIRS、CLOCK/second-chance、GD-Size（GreedyDual）、LRU-K、Redis 4.0 采样 LFU、TinyLFU 准入过滤、TTL jitter 防同步过期、概率早过期（stampede 预热）、serve-stale（RFC 8767 / stale-if-error RFC 5861）、负缓存 TTL（RFC 2308）
- **序与因果族**：Lamport 时钟、向量时钟、HLC 混合逻辑时钟（CockroachDB）、OR-Set/2P-Set CRDT、K8s resourceVersion 乐观并发、wait-die/wound-wait 时间戳排序、wait-for graph 死锁检测
- **流式统计族**：Welford 在线方差+可合并、P² 流式分位数（Jain-Chlamtac）、Morris 近似计数、Holt 双参数指数平滑预测、Croston 间歇需求、CUSUM 变点、Page-Hinkley、MAD 稳健 z 分数、Grubbs 检验、run test（Wald–Wolfowitz）、滞后 k 自相关、SPRT 序贯检验（Wald）、Beta-Bernoulli 贝叶斯 A/B、median-of-means、alias method O(1) 加权采样、Box-Muller 高斯
- **退避与探测族**：AWS full jitter、decorrelated jitter、BGP flap dampening（路由抖动惩罚）、Happy Eyeballs 双栈竞速、TCP RTO（RFC 6298 SRTT+方差）+Karn 算法、慢启动阈值、Hedged requests 延迟对冲（tail-tolerant）、重试预算（Envoy retry budget）、attempt deadline 分片、gRPC per-try timeout
- **文本与检索族**：MinHash（Jaccard 素描）、SimHash、BM25、TF-IDF、RRF 倒数排名融合、MMR 最大边际相关、Aho-Corasick 多模式、radix/patricia 压缩前缀树、KMP 失配函数、Rabin-Karp 滚动哈希、内容定义分块 CDC、Damerau-Levenshtein、Jaro-Winkler、Bitap 模糊、Myers diff（LCS）、Norvig 拼写纠正、Elias gamma/Golomb-Rice 编码、RLE、MTF 变换、Geohash
- **图与聚合族**：Union-Find（路径压缩+按秩合并）、Kahn 拓扑排序、Tarjan SCC、扫线最大并发区间、区间调度贪心（活动选择）、0/1 背包 DP、首次适应递减装箱（context packing）、Merkle 树反熵、Power-of-two-choices、一致性哈希有界负载（Vimeo CHBF）、Maglev、jump consistent hash、Rendezvous/HRW
- **批量与流控族**：批量攒批（size+linger 双阈值，Kafka producer）、NAPI 自适应中断合并（事件攒批）、Flink watermark 迟到边界、计数/翻滚/滑动窗口触发器、脏页回写节流（writeback throttle）、group commit 攒刷盘、Reactive Streams demand 记账、coalesce-by-key 最新胜、幂等消费去重窗、动态信号量 resize、striped lock（Guava）
- **LLM 采样族**：top-p nucleus、top-k+温度、repetition/frequency/presence penalty、self-consistency 多数投票、min-p 采样
- **其他**：SLO 多窗多烧率（Google SRE burn rate）、错误预算滚动门、Alertmanager 告警分组+静默窗、指标基数限制器（bounded cardinality）、OTel 一致性采样（parent-consistent）、K8s PDB 中断预算（max-unavailable）、K8s Job backoffLimit、HPA 稳定窗、DNS TTL 缓存、跳表（有序表概率平衡）、四叉树、Roaring 位图容器（简化）、常数时间比较、HKDF（RFC 5869）、OOM badness 记分

## 已裁决（Decisions so far）

- 号段占用 3000–3149/T5001–T5300/impl 2001–2150（fetch 已通：origin/main 与本地同步无超前；本地全档 maps/specs 2500+、tickets T4001+、impl 1701+ 均空）。
- P 会话遗留 R66 对账轮（快照 1056/CONTEXT 955，全仓 verify 十一波连续绿）与孤儿 impl 875/876 已代收口提交（df9a3692/4a904d02）——工作树净空起跑，Q 系 1056 基线起算。
- 直推 main 逐轮 push（P 会话先例 + 用户常设授权）；每轮定向 `git add` 自有文件。

## 排程表（滚动追加——Wave 1 先行，后续波次前沿推进后毕业）

| R | effort | 主题 | 票 | impl | 状态 |
|---|---|---|---|---|---|
| R1 | #3000 | 3000 系对账门落位（QSession3000LedgerAuditTest 四面互证） | T5001–T5002 | 2001 | ✅ |
| R2 | #3001 | Welford 在线方差（Welford 增量/Chan 并合并思想） | T5003–T5004 | 2002 | ✅ |
| R3 | #3002 | 并查集（Union-Find 路径压缩+按秩合并思想） | T5005–T5006 | 2003 | ✅ |
| R4 | #3003 | EDF 最早截止期队列（实时调度经典思想） | T5007–T5008 | 2004 | ✅ |
| R5 | #3004 | top-p 核采样（GPT-2 nucleus sampling 思想） | T5009–T5010 | 2005 | ✅ |
| R6 | #3005 | 对账轮（快照 +4（1056→1060）+ 全仓 verify 三门绿 + push；6/150=4%） | T5011–T5012 | 2006 | ✅ |
| R7 | #3006 | P² 流式分位数（Jain-Chlamtac 五标记思想） | T5013–T5014 | 2007 | ✅ |
| R8 | #3007 | 混合逻辑时钟（CockroachDB HLC 思想） | T5015–T5016 | 2008 | ✅ |
| R9 | #3008 | Morris 近似计数器（Morris 1978 概率计数思想） | T5017–T5018 | 2009 | ✅ |
| R10 | #3009 | ARC 自适应替换缓存（Megiddo-Modha 四链思想） | T5019–T5020 | 2010 | ✅ |
| R11 | #3010 | MinHash Jaccard 素描（Broder 1997 近重复思想） | T5021–T5022 | 2011 | ✅ |

## Not yet specified（雾区——候选静脉见「借鉴定源」，前沿推进后逐波毕业成排程行）

- Wave 2+：调度族→缓存驱逐族→流式统计族→退避探测族→文本检索族→图聚合族→批量流控族→LLM 采样族（每轮落轮前 grep 夘核是否已被并行会话占坑，占坑即换静脉下一候选）。

## Out of scope

- P-2000 系余量号段（#2066–#2149 / specs 2066–2149 / T3233–T3400 / impl 1617–1700）保留给 P 会话续轮，Q 不占用。
- P 雾区候选静脉（其 map「借鉴定源」列出的未实现候选）原则上不抢。
- L-1700/M-1500/O-1800 已 mined 带不重复实现。
- 修改持久化 SPI 公共签名或跨模块依赖边界（星形白名单硬约束）。
