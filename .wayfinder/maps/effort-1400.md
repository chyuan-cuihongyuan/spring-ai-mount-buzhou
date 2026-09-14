# Wayfinder Map — L 会话 1400 系：借鉴高价值开源项目的 50 轮自迭代（effort #1400 总图）

> **L 会话**（2026-09-14 启动）：继 C（300）/ D（400）/ E（500）/ F（600）/ G（700）/ H（800）/ I（900）/ J（1000）/ K（1200）之后的第十条自迭代线。
> **号段裁决（号段声明先行）**：L 会话占用 spec **1400–1449**、票 **T2101–T2200**（每轮 2 张：shape + verify）、impl **1053–1102**（每轮 1 片）、efforts **#1400–#1449**。本文件 + progress-effort-1400.md 即占坑声明，先于 R1 动工提交入 main。
> **开工前双查**：fetch 后实查 origin/main @ 0d1de35c——I 会话 900 系进行中（spec 至 952+）、J 会话 1000 系进行中（spec 至 1047，声明 1000–1149）、K 会话 1200 系进行中（测试补全覆盖线，声明 1200–1349）；**1400 系完全空闲**，无任何 spec/map/ticket 占用。
> **并存声明**：I/J/K 会话 map 文件与本线**互不触碰**；每轮开工先 `git fetch` 双查 main，push 被拒即 `pull --rebase` 后重推。
> 用户常设授权（沿 F/G/H/I/J/K 会话）：**全程 AFK，不问用户**——每轮 = wayfinder（决策票）→ to-spec → to-tickets → implement → git 自动提交推送 GitHub。

## Destination

**50 个完整自迭代 loop 全部完成**——每轮从高价值 GitHub 项目借鉴一个思想，落地为本仓一个小而完整、带测试、默认零行为变化或 opt-in 的机制改进；每轮 Conventional Commits 提交并推送 GitHub；周期性（每 10 轮）+ 收口轮跑全仓 `mvn -B -ntp clean verify` 绿（16 模块 + 快照门 + SpecCoverage 覆盖门）。

## Notes

- 每轮固定四步产物：决策票（shape 票同轮开+解决，Resolution 注明「用户常设授权 AFK、可推翻」）→ `docs/spec/<14NN>-<slug>.md`（+README「生产级纵深 X（L 会话 1400 系增量）」表行，SpecCoverageTest 门）→ impl 切片 → 模块代码+测试。
- 每轮模块级定向测试必须绿；新公共 api 面类型入轮再生 API 快照（`-Dbuzhou.api-snapshot.regenerate=true`，优先嵌套 record 不进快照面）；每轮 commit 后 push 本分支，周期性合并 origin/main 防漂移。
- 选题纪律：每轮开工先缺口核查（grep spec+代码，**含 H 池 R1–R50+S1–S10 与 I/J/K 已落地主题一并回避**——回避清单见 progress-effort-1400.md 头注），已实现则台账记 `ruled-out` 顺延备选池；代码库 300+ effort 高度饱和，排重 grep 必须 `-i` 且按类名后缀查。
- 代码规范：无魔法数字（static final 常量或配置）、SLF4J 占位符、record/sealed 优先；进程级静态读面须配 reset 注入点与注释说明；测试无 Mockito——手写 fake/匿名类/lambda stub。
- 模块依赖边界不变：feature 模块互不直接依赖，跨机制协作走 core 事件总线或 core SPI。

## Decisions so far

（每轮 shape 票 Resolution 的 gist 逐轮补登于此）

- [跨会话轮次并发水位观察者的形状裁决](../tickets/T2101-turn-concurrency-shape.md) — TurnConcurrencyTracker implements SessionObserver（once-per-turn seam，Hook 的流式 afterModel 逐 chunk 会漏账故不用）；started/ok/failed 三总量 + active/peakActive 水位 + 守恒式 started=ok+failed+active；同轮实证修复 DefaultAgentSession 两处 guard-block 路径 onTurnStart 后无终结回调的 TURN span 泄漏（非流式补 onTurnEnd/流式补 onTurnError）——HikariCP 池读面思想。
- [工具结果字节直方分桶的形状裁决](../tickets/T2103-result-size-histogram-shape.md) — ToolResultSizeHistogram implements BuzhouHook（opt-in afterTool 单点，零裁决零侵入）：五幂次边界桶（256/1K/4K/16K/64K）+溢出桶 + executed/failed/totalBytes，守恒式 successes=Σbuckets、executed=successes+failed；UTF-8 口径与 J R46–R47 对齐——Prometheus histogram 思想。
- [MCP 工具入参 schema 破坏性变更分级的形状裁决](../tickets/T2105-schema-compat-grader-shape.md) — McpSchemaCompatGrader 纯函数（McpDirectoryDiff 显式留白的 schema 轴）：客户端守恒视角四破坏轴（removed/type_changed/newly_required/enum_narrowed）+fail-closed，嵌套 SchemaCompatVerdict 典序 reasons——buf breaking 思想。
- [EWMA 自适应超时推荐器的形状裁决](../tickets/T2107-adaptive-timeout-shape.md) — **换题轮**（原题 retry budget 与 spec 178 RetryBudget 全撞）：AdaptiveTimeout 纯推导器——EWMA(α=0.3,CAS 无锁)+clamp(⌈EWMA×3⌉,floor,ceiling)+预热哨兵(<3 样本 empty)+stats() 无副作用；不接线执行路径——Envoy timeout budget/Finagle 自适应超时思想。
- [会话 id 熵审计的形状裁决](../tickets/T2109-session-id-entropy-shape.md) — **换题轮**（原题基数守卫与 spec 160 全撞）：SessionIdEntropyAudit 纯函数——字母表下界估计（观测字符类保守求和）+bits=length×log2(alphabet) nanoid 同款+四档闭集（WEAK<64/STRONG≥112≈UUIDv4）+批量四桶；只读不裁决。
- [Dashboard 查询页守卫的形状裁决](../tickets/T2111-query-page-guard-shape.md) — **实证缺陷修复**：listSessions 零钳制（size=1000 万即无界读 store）与 filtered 路径 200 钳制不对称 + 两路径裸 NFE 游标解析——MAX_PAGE_SIZE=200 常量+normalizePageSize/parseCursor 共享辅助两路径同源，翻页语义逐位不变——Grafana query limit 思想。
- [租约续期健康读面的形状裁决](../tickets/T2113-lease-renewal-readout-shape.md) — **换题轮**（序列缺口无序号不伪实现/TTFT 已有）：SessionLeaseGuard 增量 failures+minRemainingAtRenewal 水位+lastRenewalAt+renewalStats()（-1/0 哨兵），recordRenewalSuccess 提取公共记账，语义逐位不变——Redisson watchdog 健康审计思想。
- [PII 检测器合成探针自查的形状裁决](../tickets/T2115-pii-probe-selfcheck-shape.md) — **换题轮**（single-flight 与 ToolCallCoalescer 全撞）：PiiProbeSelfCheck 纯函数——内建确定性合成池正例逐类召回+负例误报哨兵（恒 0），基线标定锚=内建池×内建检测器全召回；身份证号不入池（校验位合成号撞真实号红线）——spaCy/Presidio 评测思想。
- [断路器状态时长分析器的形状裁决](../tickets/T2117-circuit-duration-analyzer-shape.md) — **换题轮**（混淆矩阵与 JudgeCalibration 数学全同半撞）：CircuitStateDurationAnalyzer 纯函数——journal Transition 按模型积段→逐状态 total/segments/max+openShare crash-loop 画像，无序容忍+采样窗口径入档——Resilience4j state duration 思想。
- [多租户配额公平指数的形状裁决](../tickets/T2119-fairness-index-shape.md) — **换题轮**（令牌桶水位与 available()+KeyHotspot 半撞）：FairnessIndex 纯函数——Jain 指数 J=(Σx)²/(n·Σx²)+dominantShare 单点吃满+shares 降序行动面+isFair(0.9)+全零 -1 哨兵——Kafka client quota 公平性思想。
- [工具入参校验读数的形状裁决](../tickets/T2121-validation-stats-shape.md) — H R21 让位的下半轴：ToolArgsValidator 静态计数内置 validate()（三调用点自动全覆盖）——validations/accepted 守恒+七错误桶 MARK_* 标记单源+桶非互斥入档+无可校验结构不入账；Pydantic ValidationError 思想。
- [取消延迟追踪读面的形状裁决](../tickets/T2123-cancel-latency-shape.md) — CancelLatencyTracker（单会话实例构造期绑定，TurnErrorSampler 同型）：onCancel 仅在途轮记未决键+轮终结消费入环（环 64+P50/P95 recent-rank+trackedTotal 累计）——「取消信号→实际停止」时延显形，与 H 824 原因分布正交——Temporal cancellation latency 思想。
- [工具入参字节直方的形状裁决](../tickets/T2125-input-size-histogram-shape.md) — **换题轮**（R49 与 SummaryDegradeReasons H 844 + CompactionRatioStats 覆盖）：ToolInputSizeHistogram implements BuzhouHook（beforeTool 单点，1401 结果侧对称镜像）——arguments 序列化 UTF-8 字节同款五幂次桶+溢出，守恒 executed=Σbuckets，钩内自序列化成本口径入档——Datadog DogStatsD read/write 对称思想。
- [结构化输出 REASK 读数的形状裁决](../tickets/T2127-structured-output-stats-shape.md) — StructuredOutputStats 公共静态面（埋点在 chatForEntity 五点，ToolArgsValidator 先例）：漏斗五计数+双守恒式（attempts=首过+再问解析+失败；reasks=再问解析+失败）+firstPassRate 派生——Instructor max_retries 可观测面思想，机制已在补读数。
- [Span 树拓扑读面的形状裁决](../tickets/T2129-span-topology-shape.md) — **换题轮**（R44 与 RollingJsonlWriter spec 712 覆盖）：SpanTreeTopology 纯函数（core/observability）——Topology 深度/扇出/根/孤儿计数+kind 直方降序+环防护 visited 收敛——Jaeger DAG 结构形状思想（时延维正交）。
- [事件总线积压水位读数的形状裁决](../tickets/T2131-backpressure-watermark-shape.md) — EventBackpressureStats 进程级静态面（公共类+internal 分发器埋点）：depthWatermark 深度历史峰值+blockedPushes BLOCK 限时等待计数（>0=容量打满）+Snapshot/reset——Kafka consumer lag 思想，EventBusStats 公共 record 不改形。
- [事务计量装饰器的形状裁决](../tickets/T2133-uow-instrumented-shape.md) — InstrumentedUnitOfWork implements UnitOfWork（opt-in 装饰器覆盖全部 SPI 实现）：begun/completed/failed/inFlight 守恒+失败异常类 Top 榜（有界 8 并 OTHERS）+异常原样上抛+deleteSession 透传——pg_stat_database xact + Seata 事务度量思想。
- [舱壁在飞峰值水位读面的形状裁决](../tickets/T2135-bulkhead-peak-shape.md) — **换题轮**（R24 checkpoint 无时戳/R31 无批量 API 均不硬来）：AgentBulkhead 增量 peakInFlight/peakSaturation（acquire 成功路径采样、拒绝不采样不虚高、256 折叠纪律、internal 扩展无新公共类型）——HikariCP 池饱和度思想。
- [Redis 慢操作榜的形状裁决](../tickets/T2137-redis-slow-oplog-shape.md) — **换题轮**（R33 与 mcp breaker 域临界→S4 题）：RedisSlowOpLog 进程级静态面（严格大于阈值入榜+FIFO 32 新→旧+totalSlowOps 水位+动态阈值+reset），RedisMessageStore 三操作 finally 埋点（deleteSession 混叠显式出域）——Redis SLOWLOG 客户端侧思想。
- [预算分档分类器的形状裁决](../tickets/T2139-budget-tier-shape.md) — BudgetTierClassifier 纯函数（core/budget）：classify→Tier 三档（WARN_RATIO=0.8/HARD_RATIO=1.0 含端点）+UNKNOWN 畸形哨兵（-1 比率）+四桶计数+verdicts 饱和度降序+tightest(n)——k8s ResourceQuota + SRE headroom 思想；与 1029/806/817 三面辨义。
- [评估运行年龄台账的形状裁决](../tickets/T2141-eval-age-ledger-shape.md) — **换题轮**（R46 无 probe seam→S9 题注册表年龄化）：EvalRunAgeLedger 公共静态面（Registry.Registration begin/close 双点埋点，registrationId 序号）——Snapshot active/oldestActiveAgeMillis 卡死异味哨兵（无活跃 -1）/maxCompletedDurationMillis 水位/closed——tqdm + k8s 运行时长异味思想。
- [实验分桶均衡审计的形状裁决](../tickets/T2143-experiment-balance-shape.md) — ExperimentBalanceAudit 纯函数（core/experiment）：两段式 forBuckets→withAssignments（零桶计入不能装不存在）+最大份额偏移≤5pp 含端点（1e-9 卫生余量）+无样本 -1 哨兵不冒充均衡——A/A test 思想。
- [Hook 顺序碰撞审计的形状裁决](../tickets/T2145-hook-order-audit-shape.md) — HookOrderAudit 纯函数（core/hook）：analyze→同序碰撞组显形（组内名字典序=兜底序即脆性，重命名即变序；组间 order 升序；唯一 order 不占报告）——Spring ordered-bean 审计思想，只读不裁决。
- [Embedding 质量自查探针的形状裁决](../tickets/T2147-embedding-selfcheck-shape.md) — EmbeddingSelfCheck 纯函数（core/spi）：内建合成句对（相似×2+无关对照×2）穿测 EmbeddingProvider——序判定（cos(sim)>cos(dis) 逐对+minMargin>0，免绝对阈值口径）+orderHolds 回归哨兵+dimension 显形——OpenAI cookbook/sentence-transformers 语义自检思想。
- [轮次限速 per-key 拒绝榜的形状裁决](../tickets/T2149-turn-ratelimit-blocked-shape.md) — TurnRateLimitHook 增量 blockedByKeys（256 折叠纪律）+blockedSnapshot 次数降序典序+resetBlockedForTest 清榜不清桶——Cloudflare WAF top-rules 思想，限流治理按 key 的拒绝分布显形。
- [会话历史形态审计的形状裁决](../tickets/T2151-conversation-shape-shape.md) — ConversationShapeAudit 纯函数（core/message）：roleHistogram 降序典序+连续同角色非 TOOL 异常对数+空内容（带 toolCalls 空 content 为正常形态）+maxTurnGap 乱序写入嫌疑——MLflow 数据画像思想，上下文污染前的形态信号。
- [用户输入重复审计的形状裁决](../tickets/T2153-input-duplication-shape.md) — UserInputDuplicationAudit 纯函数（core/message）：归一化（trim+小写+空白折叠+截断 64）+consecutiveDuplicatePairs+maxRepeatRun+topRepeated（≥2 入榜容量 8 降序典序）——Rasa 对话分析思想，复读=最强挫败信号。
- [保留清扫新鲜度追踪器的形状裁决](../tickets/T2155-sweep-freshness-shape.md) — RetentionSweepFreshness implements Consumer<RetentionSweepReport>（addSweepListener seam 零侵入挂载）：sweepCount/lastSweepAt/staleMillis 调用方时钟/maxGapMillis 间隔水位/failureCount 复用 fullySucceeded——Airflow scheduler heartbeat 思想。
- [工具目录重名审计的形状裁决](../tickets/T2157-tool-duplicate-audit-shape.md) — ToolCatalogDuplicateAudit 纯函数（core/exec）：HarnessToolCallingManager HashMap 按名建索引重名静默覆盖（后到者胜）——analyze 名字清单显形重名组（≥2 同名、名字典序）；本地/MCP 同名遮蔽不可解释的装配事故显形——Spring bean 重名 fail-fast / Maven Enforcer 思想。
- [运行状态分布与滞后审计的形状裁决](../tickets/T2159-run-status-distribution-shape.md) — RunStatusDistribution 纯函数（core/recovery）：statusHistogram 全枚举预置+runningLagMax/turnLag=崩溃暴露窗口（currentTurn−lastCompletedTurn）+worst offenders 滞后降序典序容量 3——Temporal workflow stats 思想。
- [会话关闭耗时读数的形状裁决](../tickets/T2161-close-stats-shape.md) — SessionCloseStats 进程级静态面（公共类）：closed/closeFailures/last+maxCloseDurationMillis 水位+resetForTest；close() 埋点只增记账（清理优先/异常聚合/幂等语义逐位不变）——k8s graceful shutdown terminationGracePeriod 思想。
- [指标命名校验器的形状裁决](../tickets/T2163-metric-name-audit-shape.md) — MetricNameAudit 纯函数（core/metrics）：validate→NameVerdict 违规闭集（EMPTY/WHITESPACE/PREFIX/SEGMENT_EMPTY/CASE/CHARS）首违不短路一次看全，段规则与 starter 门同源——Prometheus metric naming 规范思想。
- [会话 spawn 统计读面的形状裁决](../tickets/T2165-spawn-stats-shape.md) — SessionSpawnStats 进程级静态面（公共类）：attempts/successes/collisions/steals 漏斗+守恒 attempts=successes+collisions+activePeak spawn 时点采样口径显式——HikariCP 建连统计思想，id 规划错误从静默变漏斗显形。
- [工具循环打断分布读面的形状裁决](../tickets/T2167-loop-break-stats-shape.md) — ToolLoopBreakerHook 增量 brokenByTool（256 折叠纪律）+brokenByToolSnapshot 降序典序+brokenTotal+maxRunObserved 水位+resetBrokenForTest 清分布不清 run 状态——Temporal retry-loop detection 思想，「哪个工具在烧配额」定向治理信号显形。
- [延迟作业调度漂移读数的形状裁决](../tickets/T2169-job-drift-shape.md) — DelayedJobQueue 增量 task 包装漂移记录（执行起点 clock.instant()−fireAt 钳 0）+DriftStats(executed/last/max)+resetDriftForTest；过期补跑漂移显形正值=补偿错过窗口量化——Sidekiq queue latency 思想。
- [工具 schema 健康审计的形状裁决](../tickets/T2171-schema-health-shape.md) — ToolSchemaHealthAudit 纯函数（core/exec）：四态分桶（VALID/MISSING/UNPARSEABLE/NOT_OBJECT）与校验器跳过条件严格同口径（三键全缺=裸奔）+bypassRatio 派生+findings 封顶 16——ajv/OpenAPI schema 校验思想。
- [事件时序单调性审计的形状裁决](../tickets/T2173-event-order-audit-shape.md) — EventOrderAudit 纯函数（core/observability）：analyze 单会话事件→inversions 逆序对数+maxInversionMillis 倒退量+firstInversionIndex 定位（-1 哨兵）；等时刻不算逆序、null 时戳跳过——事件溯源不变量思想（与 TurnSequenceAudit turn 序号轴辨义）。
- [评估数据集质量审计的形状裁决](../tickets/T2175-dataset-quality-shape.md) — DatasetQualityAudit 纯函数（core/eval）：退化条目分桶（空 input/expected+短 input<8 字符阈值）+inputLengthP50/P95 秩插值+degenerateRatio 派生（同条目双退化两桶同计可>1）——Cleanlab 数据质量思想（与 847 重复轴/期望格式轴三者辨义）。
- [悬空轮检测器的形状裁决](../tickets/T2177-dangling-turn-shape.md) — DanglingTurnDetector 纯函数（core/message）：按 turnSeq 分组——轮内有 USER 无 ASSISTANT 即悬空（TOOL 链不豁免、仅 TOOL/SYSTEM 轮不算）+样本封顶 8 升序+hasDangling 哨兵——Temporal activity 检测思想，取消/中断残留的「问了没答」形态显形。
- [L 会话阶段对账轮的形状裁决](../tickets/T2181-l-audit-shape.md) — **里程碑对账轮**：LSessionLedgerAuditTest 对账测试（范围自扩展+四面互证：票对公式/impl ±1 窗/README 行/spec 号连续性）——同批修复实证漂移：R31-R39 票号 +2 重编号、R39 effort/spec 错标 1439→1438、impl 1083/1084/1085 置换——J/K 对账轮先例 + SRE Production Readiness Review 思想。
- [Spill 冷热分层访问审计的形状裁决](../tickets/T2183-spill-tiering-shape.md) — SpillTieringAudit 纯函数（spill）：读事件按 uri 聚合对照存量全集——never/single/multi 三桶+hotRatio/coldRatio 派生（空库 -1 哨兵）——MinIO tiering/S3 lifecycle 思想（与 843/815 三者辨义）。
- [门阈值敏感性扫描的形状裁决](../tickets/T2185-gate-sensitivity-shape.md) — GateThresholdSensitivity 纯函数（core/eval）：analyze(scores, threshold, δ)→δ 带计数+tighten/loosen 翻转分向+sensitivityRatio 派生（左闭右开带、-1 哨兵、δ 负值 fail-fast）——scikit-learn validation_curve 思想。
- [Saga 运行静态读数的形状裁决](../tickets/T2187-saga-stats-shape.md) — CompensatingBatch.sagaStats() 静态面（既有类增量）：runs/successes/compensationRuns/compensationFailures 漏斗+conserved 守恒+lastFailedStep 断点步名（currentStep 追踪）——Seata 事务度量思想。
- [评估通过率趋势审计的形状裁决](../tickets/T2189-pass-rate-trend-shape.md) — EvalPassRateTrend 纯函数（core/eval）：Theil–Sen 成对斜率中位数（离群抗噪）+Direction 闭集（死区 ε=0.005/run+INSUFFICIENT 哨兵）——跨 run 改进/退化趋势稳健显形。
- [媒体摄入统计读面的形状裁决](../tickets/T2191-media-intake-stats-shape.md) — MediaIntake 实例面增量：intakes/bytesTotal/readBacks 三计数+per-MIME 直方（数量降序典序、封顶 16 基数纪律）+stats/reset（不影响 store）——OpenAI usage by modality 思想，多模态使用画像与字节配额治理显形。
- [导出清单校验统计读面的形状裁决](../tickets/T2185-manifest-verify-stats-shape.md) — **1439 补位轮**（H R39 补位先例）：ExportManifestVerifyStats 静态面（三受踪包装零侵入）——verifies=ok+failed 守恒+mismatched/missing/unexpected 明细桶+lastEntryKind——TUF 校验遥测思想。
- [语义切片索引覆盖读面的形状裁决](../tickets/T2193-chunk-coverage-shape.md) — SemanticChunkIndex 增量 coverageStats()（indexedUries/totalChunks/maxChunksPerUri/largestUri 切片失衡定位）——Elasticsearch index stats 思想。
- [事件去重聚合读面的形状裁决](../tickets/T2195-event-dedup-stats-shape.md) — **换题轮**（FeedbackExporter 画像面过窄）：EventDeduplicator 增量 passed/deduped 双计数+DeduplicationStats（ringSize/capacity+deduplicationRatio -1 哨兵）+reset 只清计数不清环——SendGrid/Mailgun webhook replay 遥测思想。
