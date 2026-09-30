# Effort #9000 总图 — W 会话 9000 系 50 轮自迭代（能力自补充与自进化第十三弹）

> 会话：W（A–V 字母已占用：V=8000 已收口封卷——V50 收口声明「W 会话（9000 系）另开新图」）；本轮直推 main 逐轮 commit（GitHub 离线时本地逐轮 commit、对账轮补 push）。
> 号段（本地全档实查空闲：specs 9000+ 全空、tickets W9001+ 全空、impl 2353+ 全空；V 系 8000 段已收口封卷）：**efforts #9000–#9049（50 轮）、specs 9000–9049、票 W9001–W9100（每轮 shape+verify 一对，shape=9001+2(N−9000)）、impl 2353–2402（impl = 2353+(N−9000)）**。
> 纪律：每轮四步（map→spec→tickets→implement）+ 双门（模块定向测试 + 全局三门：覆盖门/快照门/对账门）+ 一轮一 commit；**每 6 轮一对账轮**（W6k：快照批补登 + verify + 台账核账）；README 纵深行逐轮即时登记（SpecCoverageTest 双向门）。
> 对账门：W1 即落 `WSession9000LedgerAuditTest`（VSession8000LedgerAuditTest 同款公式族第十应用）：spec N → shape 票 9001+2(N−9000) / verify=+1 / impl 2353+(N−9000)，spec 起点断言 9000 严格递增。

## Destination

W 会话四步闭环持续推进：从高价值开源项目（>10K stars）借鉴思想，落小纵切组件 + 周期对账轮，全仓三门绿、README/api-surface/MAP/台账四面一致、逐轮 commit、50 轮封卷。

## 选题原则与借鉴定源（GitHub 高价值项目思想）

已实现带避让：每轮落轮前 find/grep 快照复核（V-8000 全 50 轮、U-7000/T-6000/S-5000/R-4000 及更早 A–T 系全量）；雾区候选不抢。本轮候选静脉（占坑即换下一候选，备选池见排程表脚注）：

- **图匹配与割族**：Hopcroft–Karp 二分图最大匹配（1973 O(E√V)）、Edmonds–Karp BFS 增广最大流（1972——与 Dinic（8012）同族不同面）、Stoer–Wagner 无向全局最小割（1997 网络分区容错思想）、Bron–Kerbosch 极大团枚举（1973 带 pivot——社区发现/图分析思想）
- **树结构与剖解族**：Borůvka 森林合并 MST（1926——与 Kruskal 边排序不同面：全森林最小边同步合并的并行骨架）、重链剖分 HLD（树上路径查询）、重心剖分（点分治）、笛卡尔树（Treap 静态形态/RMQ↔LCA）、替罪羊树（重建式自平衡 BST）
- **编码压缩族**：LZW 字典压缩（GIF/UNIX compress 思想）、BWT 变换（bzip2 思想）、MTF 变换（bzip2 思想）、LZ77 滑窗引用压缩（Ziv–Lempel 1977——回窗 (offset,length) 引用 vs LZW（LZ78 系）字典生长——VarintCodec 已含 zigzag 故换替补）、rANS 熵编码（Zstd ANS 思想）
- **序列指纹与自动机族**：winnowing 指纹（MOSS 查重思想）、TF-IDF 词频-逆文档频率向量化（scikit-learn/Solr/ES 思想——BM25 已占不同面）、后缀自动机（Blumer 1985）、回文树 Eertree（Apostolico 思想）、莫尔斯编解码（电报独件）
- **随机化与 bandit 族**：EXP3 对抗 bandit（Auer 2001）、梯度偏好 bandit（Sutton & Barto）、Metropolis–Hastings MCMC（1953）、置换检验 PermutationTest（Fisher 频率学派镜像——bootstrap 已占）、Jackknife 刀切（Quenouille 1949）
- **数论与数值族**：Miller–Rabin 素性检测（BigInteger 同思想）、Karatsuba 分治乘法（1960）、平方根倒数速算（Quake III 0x5f3759df 思想）、Weiszfeld 几何中位数（1937）、de Casteljau 贝塞尔曲线（1959）
- **加密信任与随机源族**：HOTP 计数器口令（RFC 4226——TotpGenerator 的计数器姊妹面）、Diffie–Hellman 密钥交换（1976）、xorshift64 PRNG（Marsaglia 2003）、SplitMix64（JDK SplittableRandom 同款）、双调排序网络（Batcher 1968 GPU 思想）
- **决策优化族**：cron 五域解析（Vixie cron 思想）、PID 控制器（控制论/K8s 思想）、模拟退火（Kirkpatrick 1983）、2-opt 局部搜索（Croes 1958）、分支限界（Land–Doig 1960）
- **W49 独件**：A\* 启发式寻路（Hart 1968——游戏 AI/导航思想；与 Dijkstra（已有）贪心无启发不同面）

## 已裁决（Decisions so far）

- 号段占用 9000–9049/W9001–W9100/impl 2353–2402（本地全档实查 + V 系收口交接声明）。
- 直推 main 逐轮 commit；每轮定向 `git add` 自有文件；对账轮尝试 push（离线则记「离线」）。
- W1 = 对账门落位轮；W6k（6/12/18/24/30/36/42/48）= 对账轮；W50 = 收口对账轮。
- W0 占坑复核裁决：MerkleTree 已占（guard/AuditMerkleTree）→ W37 换 HOTP；ShamirSecretSharing 已占（crypto）→ 不再用；XxHash/Crc32 已占 → 不用。40 组件全档 find 复核空闲。
- W1 开工前 README 概念级复核勘误（find 名字差漏检三处）：LcaLifting 已占（U 系）→ W7 换 BoruvkaMst（Borůvka 森林合并——与 Kruskal 边排序不同面）；n-gram 特征提取已占 → W20 换 TfIdfVectorizer（BM25 已占不同面）；bootstrap 均值置信区间已占 → W28 换 PermutationTest（Fisher 置换检验——bootstrap 的频率学派镜像）。SplayTree/Treap/AVL/Van Emde Boas 均已占（T/U 系）→ 不用；T 会话（6000 系）进行中，树结构族每轮落轮前加查 README 表防并行占坑。
- 全仓 verify 遇已入档满载偶红 flaky 按协议排除重跑（R48/V48 口径延续）；从根跑为准。

## 排程表（预排全 50 轮——落轮前 grep 复核占坑即换，状态逐轮翻转）

| W | effort | 主题 | 票 | impl | 状态 |
|---|---|---|---|---|---|
| W1 | #9000 | 9000 系对账门落位（WSession9000LedgerAuditTest 第十应用） | W9001–W9002 | 2353 | ✅ |
| W2 | #9001 | HopcroftKarpMatcher 二分图最大匹配（Hopcroft–Karp 思想） | W9003–W9004 | 2354 | ⬜ |
| W3 | #9002 | EdmondsKarpMaxFlow BFS 增广最大流（Edmonds–Karp 思想） | W9005–W9006 | 2355 | ⬜ |
| W4 | #9003 | StoerWagnerMinCut 无向全局最小割（Stoer–Wagner 思想） | W9007–W9008 | 2356 | ⬜ |
| W5 | #9004 | BronKerboschCliques 极大团枚举（Bron–Kerbosch 带 pivot 思想） | W9009–W9010 | 2357 | ⬜ |
| W6 | #9005 | 对账轮（快照批补登 + verify 三门绿；6/50=12%） | W9011–W9012 | 2358 | ⬜ |
| W7 | #9006 | BoruvkaMst 森林合并最小生成树（Borůvka 并行骨架思想——LcaLifting 已占换替补） | W9013–W9014 | 2359 | ⬜ |
| W8 | #9007 | HeavyLightDecomposition 重链剖分（HLD 思想） | W9015–W9016 | 2360 | ⬜ |
| W9 | #9008 | CentroidDecomposition 重心剖分（点分治思想） | W9017–W9018 | 2361 | ⬜ |
| W10 | #9009 | CartesianTree 笛卡尔树（RMQ↔LCA 思想） | W9019–W9020 | 2362 | ⬜ |
| W11 | #9010 | ScapegoatTree 替罪羊树（重建式平衡思想） | W9021–W9022 | 2363 | ⬜ |
| W12 | #9011 | 对账轮（快照批补登 + verify 三门绿；24%） | W9023–W9024 | 2364 | ⬜ |
| W13 | #9012 | LzwCodec LZW 字典压缩（GIF 思想） | W9025–W9026 | 2365 | ⬜ |
| W14 | #9013 | BurrowsWheelerTransform BWT 变换（bzip2 思想） | W9027–W9028 | 2366 | ⬜ |
| W15 | #9014 | MoveToFrontTransform MTF 变换（bzip2 思想） | W9029–W9030 | 2367 | ⬜ |
| W16 | #9015 | Lz77Codec 滑窗引用压缩（LZ77 思想——VarintCodec 已含 zigzag 换替补） | W9031–W9032 | 2368 | ⬜ |
| W17 | #9016 | AnsCodec rANS 熵编码（Zstd ANS 思想） | W9033–W9034 | 2369 | ⬜ |
| W18 | #9017 | 对账轮（快照批补登 + verify 三门绿；36%） | W9035–W9036 | 2370 | ⬜ |
| W19 | #9018 | WinnowFingerprint winnowing 指纹（MOSS 思想） | W9037–W9038 | 2371 | ⬜ |
| W20 | #9019 | TfIdfVectorizer 词频-逆文档频率向量化（scikit-learn/Solr 思想——n-gram 已占换替补） | W9039–W9040 | 2372 | ⬜ |
| W21 | #9020 | SuffixAutomaton 后缀自动机（Blumer 思想） | W9041–W9042 | 2373 | ⬜ |
| W22 | #9021 | PalindromeTree 回文树（Eertree 思想） | W9043–W9044 | 2374 | ⬜ |
| W23 | #9022 | MorseCodec 莫尔斯编解码（电报独件） | W9045–W9046 | 2375 | ⬜ |
| W24 | #9023 | 对账轮（快照批补登 + verify 三门绿；48%） | W9047–W9048 | 2376 | ⬜ |
| W25 | #9024 | Exp3Bandit 对抗 bandit（Auer EXP3 思想） | W9049–W9050 | 2377 | ⬜ |
| W26 | #9025 | GradientBandit 梯度偏好 bandit（Sutton & Barto 思想） | W9051–W9052 | 2378 | ⬜ |
| W27 | #9026 | MetropolisHastings MCMC 采样（Metropolis 思想） | W9053–W9054 | 2379 | ⬜ |
| W28 | #9027 | PermutationTest 置换检验（Fisher 思想——bootstrap 已占换替补） | W9055–W9056 | 2380 | ⬜ |
| W29 | #9028 | JackknifeEstimator 刀切估计（Quenouille 思想） | W9057–W9058 | 2381 | ⬜ |
| W30 | #9029 | 对账轮（快照批补登 + verify 三门绿；60%） | W9059–W9060 | 2382 | ⬜ |
| W31 | #9030 | MillerRabinPrimality 素性检测（BigInteger 同思想） | W9061–W9062 | 2383 | ⬜ |
| W32 | #9031 | KaratsubaMultiplication 分治乘法（Karatsuba 思想） | W9063–W9064 | 2384 | ⬜ |
| W33 | #9032 | FastInverseSqrt 平方根倒数（Quake III 思想） | W9065–W9066 | 2385 | ⬜ |
| W34 | #9033 | GeometricMedian 几何中位数（Weiszfeld 思想） | W9067–W9068 | 2386 | ⬜ |
| W35 | #9034 | BezierCurve 贝塞尔曲线（de Casteljau 思想） | W9069–W9070 | 2387 | ⬜ |
| W36 | #9035 | 对账轮（快照批补登 + verify 三门绿；72%） | W9071–W9072 | 2388 | ⬜ |
| W37 | #9036 | HotpCounter 计数器动态口令（RFC 4226——Totp 姊妹） | W9073–W9074 | 2389 | ⬜ |
| W38 | #9037 | DiffieHellmanExchange DH 密钥交换（Diffie–Hellman 思想） | W9075–W9076 | 2390 | ⬜ |
| W39 | #9038 | XorShift64 伪随机数（Marsaglia 思想） | W9077–W9078 | 2391 | ⬜ |
| W40 | #9039 | SplitMix64 可分裂随机（JDK SplittableRandom 同款） | W9079–W9080 | 2392 | ⬜ |
| W41 | #9040 | BitonicSorter 双调排序网络（Batcher 思想） | W9081–W9082 | 2393 | ⬜ |
| W42 | #9041 | 对账轮（快照批补登 + verify 三门绿；84%） | W9083–W9084 | 2394 | ⬜ |
| W43 | #9042 | CronFieldParser cron 五域解析（Vixie cron 思想） | W9085–W9086 | 2395 | ⬜ |
| W44 | #9043 | PidController PID 控制器（控制论思想） | W9087–W9088 | 2396 | ⬜ |
| W45 | #9044 | SimulatedAnnealing 模拟退火（Kirkpatrick 思想） | W9089–W9090 | 2397 | ⬜ |
| W46 | #9045 | TspTwoOpt 2-opt 局部搜索（Croes 思想） | W9091–W9092 | 2398 | ⬜ |
| W47 | #9046 | BranchAndBound 分支限界（Land–Doig 思想） | W9093–W9094 | 2399 | ⬜ |
| W48 | #9047 | 对账轮（快照批补登 + verify 三门绿；96%） | W9095–W9096 | 2400 | ⬜ |
| W49 | #9048 | AStarSearch A\* 启发式寻路（Hart 1968 独件） | W9097–W9098 | 2401 | ⬜ |
| W50 | #9049 | 收口对账轮（快照补登 + verify 绿 + push 封卷；W 会话 50/50） | W9099–W9100 | 2402 | ⬜ |

> 备选池（撞坑即换）：Fletcher32 校验和 / MurmurHash3 / PCG 随机 / MiddleSquareWeyl / PrimeSieveAtkin 素数筛 / TonelliShanks 平方剩余 / FisherYates 已占避让 / GaleShapley 已占（StableMatching）避让 / Treap 已占避让 / 中点圆 / XiaolinWu 抗锯齿直线 / CatmullRom 样条 / 自然三次样条 / ExpDecayReservoir / ParticleFilter 粒子滤波 / QuineMcCluskey 简化 / Huffman 已占避让。

## Not yet specified（雾区——候选静脉见「借鉴定源」，备选池按需毕业）

- 每轮落轮前 grep 复核是否已被并行会话占坑，占坑即换静脉（备选池顺序取用）。
- 更远静脉（下一会话再议）：Rete 网络、Roaring 容器级压缩、序列化 schema 演进族、外存字符串 BWT 索引、跳跃表并发锁细化。

## Out of scope

- A–V 系余量号段保留各自会话续轮（V-8000 已收口不再占用）。
- A–V 全系已 mined 面不重复实现（每轮 find/grep 快照复核；MerkleTree/Shamir/XxHash/Crc32 已占即弃）。
- A–V 系雾区候选静脉原则上不抢。
