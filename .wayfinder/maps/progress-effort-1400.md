# L 会话进度台账（rolling，收口轮据此归档）

> **状态：进行中（2026-09-14 启动，目标 50/50）。** L 会话 1400 系自迭代——
> /goal ≥50 轮 wayfinder→spec→tickets→implement 闭环，全自主决策，借鉴 GitHub >10K star 项目思想。
> 号段：efforts #1400–#1449 ｜ specs 1400–1449 ｜ 票 T2101–T2200 ｜ impl 1053–1102 ｜ 分支 l-session-1400-series。
> 前情：C 300 系（PR）｜ D 400 系（PR #18）｜ E 500 系（PR #19）｜ G 700 系（PR #20）｜ H 800 系（PR #23）｜ I 900 系（进行中，spec 至 952+）｜ J 1000 系（进行中，spec 至 1047，声明 1000–1149）｜ K 1200 系（进行中，测试补全覆盖线，声明 1200–1349）。
> **号段声明先行**（G 撞号教训制度化，H 立桩先例延续）：本文件 + effort-1400.md 即 L 会话对 1400 系的占坑声明，先于 R1 动工提交并入 main；后续会话开 1500 系前必须 fetch+双查 main 的 progress-effort-*.md 与 effort-<N>.md 总图。

每轮工件配方（C/D/E/G/H/I/J 会话约定延续）：
MAP（`maps/effort-<N>.md` + `.wayfinder/MAP.md` 表登记一行）→ spec
（`docs/spec/<N>-<slug>.md`）→ 2 张票（`tickets/T<NNNN>-*.md` shape+verify，
frontmatter Type/Status，shape 票 Resolution 注明「用户常设授权 AFK、可推翻」）→ 实现+测试 → README「生产级纵深 X（L 会话 1400 系增量）」表加行（覆盖门：spec 文件名必须出现在 README）→ 新公共 api 面类型随轮 regenerate 快照（**必须带 `-Dbuzhou.api-snapshot.regenerate=true`**；嵌套 record 不进快照面）+ api-surface.md L 段加行 → 提交尾
`(resolve TXXXX-TYYYY, implNNN, specNNN, L 会话第 N 轮=effort#NNNN)`。

验证纪律（Windows 本机）：JDK21 inline + 串行 mvn（禁 -T）+ `-pl <mod> -am`
+ 排除集 `!ClasspathSkillScannerTest,!RunCommandToolTest,!RunCommandHardeningTest,!GuardAndHitlDemoTest,!TenantSandboxTest,!PropertyInvariantsTwoTest,!ErrorSignaturesTest,!UnsubscribedStreamTest,!TurnStallWatchdogTest`；
命令一律 `> /tmp/rN.log 2>&1; echo "MVN_EXIT=$?"` 后看日志。
已知 flaky 族（重跑即绿、与改动零交集即忽略）：webhook 族 / slow drip 流时序 /
WebhookOutboxLag / SpawnGatePriorityTest / SleepTimeConsolidationTest。

主题池（50 轮计划，**每轮勘察后可换——代码库 300+ effort 高度饱和，撞已有能力即换入备选池，换题注记入台账**；排重 grep 必须 -i 且按类名后缀查，每轮先 grep 后动工；回避 H 池 R1–R50+S1–S10、I 会话已落地 51+ 轮、J 会话已落地 47+ 轮、K 会话测试补全覆盖线）：

R1 ✅ 已落地=跨会话轮次并发水位观察者（会话内单飞使 per-model-call 并发失义，改跨会话聚合+顺带实证修复 guard-block span 泄漏）｜
R2 ✅ 已落地=core/hook（Hook 单点挂法免改 HookedToolCallback）｜
R3 ✅ 已落地=McpSchemaCompatGrader 纯函数（822 显式留白轴）｜
R4 ✅ 换题落地=EWMA 自适应超时（原题与 spec 178 RetryBudget 全撞→顺延 EWMA 超时轴）｜
R5 ✅ 换题落地=会话 id 熵审计（原题与 spec 160 基数守卫全撞→顺延 R19 题）｜
R6 评估确定性种子台账(FoundationDB deterministic simulation ≈15K)→core【高危待勘 EvalRunner】｜
R7 EWMA 自适应超时(Envoy timeout budget / Finagle ≈26K)→resilience｜
R8 工具调用 single-flight 合并(Go sync/singleflight, golang ≈130K)→tools【高危待勘】｜
R9 Spill 压缩债务水位(RocksDB pending compaction ≈30K)→spill【高危待勘 vs 815】｜
R7 ✅ 换题落地=租约续期健康读面（R10 题提前；原 R6 序列缺口无源换出）｜
R11 首 token 延迟 TTFT 分位读数(vLLM metrics ≈40K)→core/observability【高危待勘流式】｜
R12 护栏判定混淆矩阵读面(scikit-learn ≈21K)→guard【高危待勘 vs 判定分桶族】｜
R13 嵌入维度漂移审计(FAISS dim check ≈33K)→resilience【高危待勘 vs 804】｜
R14 检索结果重复率读数(LangChain MMR ≈105K 纯读面)→memory【高危待勘】｜
R11 ✅ 已落地=ToolArgsValidator 静态读数面（H R21 让位下半轴）｜
R16 事件序列缺口检测(Kafka offset gap ≈30K)→core【高危待勘 vs 900 丢弃分类】｜
R17 评估无进展 deadline 检测(k8s progressDeadlineSeconds ≈115K)→core｜
R18 技能内容寻址指纹(pnpm store CAS ≈32K)→skills【高危待勘 vs 813/832】｜
R19 会话 id 熵审计(nanoid alphabet entropy ≈27K)→core｜
R10 ✅ 换题落地=多租户 Jain 公平指数（R20 题提前；原 R10 令牌桶水位半撞换出）｜
R12 ✅ 已落地=取消延迟追踪（R21 题提前；与 824 正交）｜
R22 缓存驱逐计数读面(Caffeine eviction stats ≈16K)【高危待勘 prompt 缓存驱逐点】→core｜
R23 令牌桶水位读面(Guava RateLimiter ≈51K)【高危待勘 resilience 限流器】→resilience｜
R24 检查点年龄水位(Flink checkpoint age ≈25K)【高危待勘】→core｜
R25 舱壁队列水位读面(Resilience4j bulkhead ≈10K)【高危待勘】→resilience｜
R26 导出器丢弃计数读面(Jaeger exporter drop ≈21K)【高危待勘 observe-otel 队列】→observe-otel｜
R6 ✅ 换题落地=Dashboard 查询页守卫（原 R27 题提前到 R6，实证 listSessions 零钳制缺陷）｜
R28 日志字段预算读面(OTel logrecord limits, otel 规范/Java ≈2K 借规范思想)【高危待勘】→observability｜
R8 ✅ 换题落地=PII 合成探针自查（R29 题提前；原 R8 single-flight 与 ToolCallCoalescer 全撞）｜
R30 重连计数读面(Redisson reconnect ≈36K)【高危待勘 store-redis 连接 seam】→store-redis｜
R31 批量嵌入效率读数(sentence-transformers batch_size ≈17K)【高危待勘 vs 804】→resilience｜
R14 ✅ 已落地=结构化输出 REASK 读数（R32 题提前；机制已在补读数）｜
R33 离群驱逐读面(Envoy outlier detection ≈26K)【高危待勘 vs 811/814】→mcp/resilience｜
R34 Retry-After 解析器(RFC 7231/Envoy ≈26K)→resilience【高危待勘既有重试策略】｜
R35 冷热分层访问读数(MinIO tiering ≈52K)【高危待勘 vs 816】→spill｜
R36 组件版本偏斜报告(k8s version skew ≈115K)【高危待勘 starter 装配面】→starter｜
R9 ✅ 换题落地=断路器状态时长分析器（R37 题提前；原 R12 混淆矩阵与 JudgeCalibration 半撞换出）｜
R38 会话所有权转移计数(Kafka rebalance ≈30K)【高危待勘 vs 838 选举】→core｜
R15 ✅ 换题落地=Span 树拓扑（R39 core 侧通用化；时延维正交）｜
R40 Spill GC 候选标记统计(Go GC mark-sweep, golang ≈130K)【高危待勘 vs 843】→spill｜
R13 ✅ 换题落地=工具入参字节直方（R41 通用化；1401 对称镜像）｜
R42 技能加载失败原因分桶(LangChain loader taxonomy ≈105K)→skills【高危待勘】｜
R43 预算软/硬两档水位(k8s ResourceQuota scope ≈115K)【高危待勘 vs 1029】→core｜
R44 导出分块清单读数(S3 multipart, boto3 ≈10K)【高危待勘 RollingJsonlWriter】→core｜
R45 工具白名单漂移审计(Terraform drift ≈45K)【高危待勘 vs 1000/833】→guard｜
R46 健康探针时延读数(k8s probe latency ≈115K)→mcp/core【高危待勘 health seam】｜
R47 评估黄金集回归门(pytest golden ≈13K)→core【高危待勘 vs I gate/stability】｜
R16 ✅ 已落地=事件积压水位（R48 题提前；EventBusStats 瞬时值的并行读面不改形）｜
R49 摘要触发原因分布(V8 GC reason taxonomy ≈24K)【高危待勘 vs 1027】→memory｜
R50 收口终验(全仓 verify+快照再生+台账核查+README/覆盖门+MAP 达成)。

备选池（换题顺延用）：
S1 快照恢复演练计数(restic restore drill ≈31K)→core｜
S2 断路器状态变迁时刻簿(Resilience4j event stream ≈10K)→resilience【vs 814 mcp 侧，core/resilience 侧或开放】｜
S3 提示模板解析一致性指纹(LangChain prompt hashing ≈105K)→core【vs JCS 1273】｜
S4 Redis 慢命令榜(Redis SLOWLOG ≈68K)→store-redis【vs J T1453 tool 侧慢榜，存储侧轴或开放】｜
S5 会话导出字节水位(restic backup size stats ≈31K)→core｜
S6 嵌入批延迟分位(sentence-transformers ≈17K)→resilience【vs 828】｜
S7 MCP 重连退避抖动检测(gRPC channelz backoff ≈43K)→mcp【vs 840】｜
S8 JDBC 连接借还配对审计(HikariCP leak detection ≈20K)→store-jdbc【vs 828，借还失配轴】｜
S9 评估运行进度条读面(tqdm ≈30K 思想：done/total/eta 纯读面)→core【vs I prune】｜
S10 会话归档分层命中率(S3 lifecycle, MinIO ≈52K)→spill【vs S35】。

纪律备忘（沿 H 会话教训）：
- README 纵深行**逐轮即时登记**，严禁攒批（H 曾漏 43 行靠覆盖门兜底）。
- 台账轮号/effort 号错位时**立即补位轮**（H R38 跳号→R39 补位；spec 缺位立即自查）。
- 换题时：新题占原轮号，旧题移入备选池或标记 ruled-out，map/台账同步注记。
- 快照门必须带 `-Dbuzhou.api-snapshot.regenerate=true`；嵌套 record 不进快照（优先用嵌套，减门负担）。
- 排重 grep 双查：`docs/spec/` 文件名 + 全仓类名后缀（-i）。
