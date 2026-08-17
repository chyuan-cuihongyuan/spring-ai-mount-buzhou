# 生产级（production-grade）两轮研究：本地缺口勘察 + ≥10K★ 开源运维实践

> effort #3（[map](../../.wayfinder/maps/effort-03.md)）事实源。2026-08-14 由 **6 个并行子 agent** 完成：2 个本地勘察（buzhou-core；memory/spill/guard/store-jdbc/store-redis）+ 4 个外部研究（Spring 系生产实践；数据基础设施运维；运行时可靠性；安全运维 + LLM serving）。star 数为当日 GitHub API 实测；载荷性本地结论已由主 agent 复核（MySQL 索引/JdbcToolCallLog 未装配/renew 无调用方/LeaseLostException 零抛出/AuditChain 未接线，五项全证实）。
> 遵守 effort #2 的 **10K+ stars 政策**：采纳事实源只认 ≥10K★ OSS；不达标者（Reactor 5,234★、JMH 2,663★、SLSA 全家、cosign/Scorecard 等）仅注记。

## 0. 执行摘要

四机制（core/memory/spill/guard）经 effort #1/#2 后**功能完备**（Tier-2/3 全量），但以「生产级运行时库」标准横扫，存在**七类系统性缺口**：

| # | 缺口类 | 严重度 | 关键证据 |
|---|--------|--------|---------|
| 1 | **停机与生命周期**：AutoConfig bean 无销毁回调、Runtime 不追踪会话、executor 只 `shutdownNow`、`stream()` 无 doOnCancel | 致命 | `BuzhouCoreAutoConfiguration`（grep SmartLifecycle/@PreDestroy 零命中）；`DefaultAgentRuntime:65` |
| 2 | **挂起与预算**：外层 `futures.get(i).get()` 无超时 + `synchronized(groupLock)` 不可中断 → 一个工具可挂死整条会话；租约无续租、`LeaseLostException` 从不抛 | 致命 | `HarnessToolCallingManager:224,302-305`；全 core 无 renew 调用 |
| 3 | **存储增长治理**：MySQL 第二次启动必失败（索引无 IF NOT EXISTS）；无版本化迁移；无 deleteSession 级联清理；ToolCallLog/RunRegistry/摘要版本/EpisodeLedger/审计链全部只进不出；spill `deleteExpired` 有实现无调度 | 致命 | `schema-mysql.sql:15,25,58,68,78`；grep deleteSession 零命中；`AuditChain` 纯内存 |
| 4 | **事务与并发正确性**：JdbcUnitOfWork 注入却无人用（先删后插两段自动提交、summary `MAX(version)+1` 竞态）；单条脏 JSON 炸整个会话 load | 高 | `JdbcToolCallLog:38-46`；`JdbcSummaryStore:32-41`；`RedisMessageStore.load` |
| 5 | **可诊断性**：无异常分类/错误码；`isErrorFeedback` 靠中文字符串前缀匹配；全模块几乎零日志；后台任务静默吞异常 | 高 | `HarnessToolCallingManager:267-270`；`DbPolicyConfigProvider:69` |
| 6 | **配置与默认值**：并发 8 / toolTimeout 60s / LEASE_TTL 90s 全硬编码；无 JSR-303 校验（store.type 拼错不 fail-fast）；spill 默认落 CWD、hot-tail 预算 0=不限、guard 防御默认关 | 高 | `HarnessAssembler:75`；`DefaultAgentRuntime:21`；`SpillProperties:35-37` |
| 7 | **guard 运维断层**：AuditChain/PolicyGateHook 未进自动装配；密钥无版本化轮换；policy 不可热更；sandbox 无内存/输出上限 | 高 | `BuzhouGuardAutoConfiguration` grep AuditChain 零命中；`AuditChain:52-60` |

总体判断（勘察 agent 原话）：单会话内「错误即反馈 + 有界 Turn + 取消三档」设计细致，但**跨进程/跨生命周期**的生产要件（停机、续租、背压、容量、可观测性）系统性缺失——「功能完整的 harness 原型，尚缺运行时库应有的外围防护层」。

## 1. 本地勘察详录

### 1.1 buzhou-core（8 维度）

1. **生命周期【部分】**：会话级 `SessionResourceRegistry` 逆序 closeAll 有 CAS 防重入；但 core AutoConfig 两 bean 无销毁回调；`DefaultAgentRuntime` 不追踪 spawn 会话（停机时在途 Turn 无人等待、租约无人 release，靠 90s TTL 自愈）；`shutdownNow` 硬中断；`stream()` 缺 doOnCancel/doFinally（订阅者取消后 afterTurn/span 收尾全跳过）；`close()` 内 dispatchEvent 抛异常会跳过后续 `listeners.clear()`。
2. **线程卫生【部分】**：仅 2 个池创建点。`DefaultAgentRuntime:63-65` 虚拟线程无名无 handler；`DbPolicyConfigProvider:25-26` 单线程 scheduled 无名无 handler，`pollSafely` `catch (RuntimeException ignored)` 零日志吞异常，且 core 内无人保证 close。
3. **超时预算【部分】**：工具级 `task.get(toolTimeout)` + Turn 级 maxToolRounds/retryBudget 已有；**缺口**：外层 join 无超时 + 组锁不可中断；模型调用 `chatClient...call()` 裸调无 core 超时；loopTimeout 默认 null 不限且只轮间检查；**值全硬编码**（并发 8、60s、90s），`BuzhouCoreProperties` 仅 2 项；`ToolSetSpec` 定义了 connect/request timeout 但 core 无消费点。
4. **资源泄漏【部分】**：租约有 TTL 但**无续租**（长 Turn 期间被 steal → 双主）；`LeaseLostException` 零抛出；fencingToken 只在 release 校验、写路径无 fence；InMemory 租约过期不物理移除；`InMemoryUnitOfWork.locksBySession` 锁对象按会话累积；`DefaultFactStore` 序列化失败静默退化。
5. **错误分类【缺失】**：无统一异常基类/可重试分类/错误码；错误识别靠中文字符串前缀；`closeAll` 只抛第一个异常；全模块仅 2 处 logger。
6. **配置校验【缺失】**：无 @Validated、无 jakarta.validation 依赖；store.type 拼错 → 三条件装配全不命中 → 运行期 NPE；编程式 API 负数不校验。
7. **InMemory 设施【缺失】**：RunRegistry/ToolCallLog/Message/Summary/Observability 全无界无清理；CoWL 写放大；三设施不在 `Buzhou.inMemoryStores()` 默认组合。
8. **背压【缺失】**：core 无 EventBus；事件分发同步内联 forEach——慢订阅者阻塞 Turn 主链路、订阅者抛异常向 chat()/close() 传播；唯一正向背压是 turnPermits 信号量。

### 1.2 memory/spill/guard/store-jdbc/store-redis（8 维度）

1. **存储增长**：spill `deleteExpired`/`deleteBySession` 存在但**无调度调用方**（仅测试引用）；`linked=true` 文件永不清理；崩溃残留孤儿。ToolCallLog append-only 无 TTL 且 JDBC 实现未装配。审计链纯内存 ArrayList。EpisodeLedger 无上限 + **sequence 内存 int 重启归零 → key 碰撞覆盖**。RecallSearch 每次检索对全部历史消息重新 embed（成本无界）。摘要每压缩 +1 版本、旧版本永久堆积。
2. **Schema 演进**：启动期 `ScriptUtils` 幂等建表；**MySQL 5 条索引无 IF NOT EXISTS → 第二次启动 ScriptUtils fail-fast 应用起不来**；无版本表/迁移路径（加列对旧库不生效）；PG 并发 `CREATE TABLE IF NOT EXISTS` 有 unique violation 竞态、无 advisory lock。
3. **后台任务**：SleepTimeScheduler 失败降级 WARNING 无指标无退避；`close()` shutdownNow 不 drain 且 **close 永不被调**（scheduler 在 MemoryModule 内联创建、非 bean）；pending 队列无上限、perSession map 会话结束不摘除；EpisodeLedger.record `catch (Exception ignored)` 完全静默。
4. **guard 运维**：密钥仅 `generateKeyPair()` 临时对、无 keyId/轮换/keystore 加载；AuditChain/PolicyGateHook 均未进自动装配；policy 规则构造期不可变无热更；DenoSandbox 每次执行跑 `deno --version` 探测；**无内存/输出上限/并发限制**。
5. **存储健壮性**：两 store 模块零 retry/backoff/降级——运行期写失败异常外溢打断当轮对话；Redis UoW 每事务新连接无池；事务边界名存实亡（见缺口 4）；摘要熔断连续 3 次失败**永久熔断**（无时间恢复/半开）且计数 map 不清理。
6. **配额隔离**：spill 无单会话文件数/字节配额；episode/fact 无条数上限；向量化无配额无缓存。
7. **级联清理**：全仓 grep `deleteSession` **零命中**——session 删除后 messages/state/spans/events/snapshots/summaries/lease 全残留；Redis 侧注释自认未做。
8. **默认值**：spill root-dir 默认进程 CWD；redis snapshot-ttl 默认 0=永不过期；jdbc dialect 默认 H2 兜底；hot-tail maxInlineChars 默认 0=不限；guard 注入防御默认全关；facts max-inject-chars ≤0=不截断。

## 2. 外部实践（≥10K★ 事实源）

### 2.1 Spring Boot 76K★ / Spring Framework 58K★（库工程实践）

- **SmartLifecycle 分 phase**：启动最小 phase 先启、停止反序；`stop(Runnable)` 完成后必须 `callback.run()`；DefaultLifecycleProcessor 每 phase 30s 超时；bean 必须容忍「没有 stop 直接 destroy」。
- **Executor 优雅关闭**：Boot `applicationTaskExecutor` 默认快死；排空是显式 opt-in（`await-termination` + period）。@Bean destroyMethod 默认推断 close/shutdown——须防与自定义排空双重触发。
- **健康协议**：UP/DOWN/OUT_OF_SERVICE/UNKNOWN + StatusAggregator；禁用机制报 UNKNOWN 而非 DOWN；bean 名去 HealthIndicator 后缀即组名；慢指标 10s 告警。`@Endpoint(id=...)` + @ReadOperation 只读端点。
- **指标**：Micrometer 小写点分命名、单位/类型不进名字、tag 值必须有界（严禁 sessionId 进 tag）；`MeterBinder` bean 自动绑定（`@ConditionalOnClass(MeterRegistry)` 可选探测）——库不强制依赖 actuator 的标准做法。
- **配置**：record @ConfigurationProperties + JSR-303（校验在绑定时执行=启动期 fail-fast）+ `additional-spring-configuration-metadata.json`（补 hints/默认值/**弃用条目**：warning→error→大版本移除）；配置键独占命名空间。
- **FailureAnalyzer**：AbstractFailureAnalyzer 拦截启动异常翻译成「description + action + cause」；spring.factories 注册；ApplicationContextRunner 测试矩阵。
- **兼容性**：弃用 API 下一个大版本移除、至少 12 个月重叠；`@since`/`@Deprecated(since)` 纪律；每 minor 版配 Upgrading 页。

### 2.2 数据基础设施（Elasticsearch 77.9K★ / etcd 52.1K★ / ClickHouse 49.2K★ / Temporal 22.3K★ / PostgreSQL 21.8K★ / pgvector 22.6K★ / Milvus 45.6K★ / MinIO 61.4K★ / Redis 76K★）

**五条可提炼的治理公理**：

1. **「封闭才计时」**（ES ILM min_age 从 rollover 起算 / Temporal 闭式执行才计保留期）：保留期一律从数据段/会话**封闭**时刻起算，活动数据永不进清理路径；改短保留期不追溯已封闭数据。
2. **「声明式策略 + 低频后台兑现 + 允许读到陈旧」**（ClickHouse merge_with_ttl_timeout 默认 4h / MinIO scanner / ES poll_interval 默认 10m）：策略是数据的属性、执行器轮询且最终一致，绝不挂写路径。
3. **「基础阈值 + 比例因子 + 封顶 + 硬性兜底」**（PG autovacuum `50 + 0.2×N`、PG18 max_threshold、freeze_max_age 兜底）：触发清理的定量形状四件套。
4. **「压缩 ≠ 回收，回收有代价」**（etcd compact/defrag 分离、pgvector 先 REINDEX CONCURRENTLY 再 VACUUM）：历史截断与空间归还是两个动作，后者须限流并发化。
5. **「配额内分可牺牲/不可牺牲集合」**（Redis volatile vs allkeys 族）：逐出只发生在可再生集合，事实台账默认 noeviction。

具体形状：`SessionHistoryPolicy{defaultRetention=PT72H, 计时锚点=closedAt, archiveOnExpiry}`（Temporal）；`ObservabilityTtl{eventTtl=PT7D, spanTtl=PT7D, sweepTimeout=PT4H, sweepBatch}`（ClickHouse）；`MaintenanceTrigger{base=50, scale=0.2, max, hardFloor}`（PG）；`VectorMaintenance{reindexThreshold, 惰性索引, handoff}`（Milvus/pgvector——**embed 一次即持久化向量，禁止 recall 时逐条重 embed**）；`SpillLifecycle{expire, transition, maxTotalBytes}`（MinIO ILM）；`SessionQuota{NOEVICTION 默认 / VOLATILE_LRU 只逐可再生 / sample=5}`（Redis maxmemory 族）。

### 2.3 运行时可靠性（Netty 35K★ / Kafka 33.5K★ / HikariCP 21.2K★ / Akka 13.3K★ / Toxiproxy 12.2K★ / gRPC-Java 12.1K★）

- **Netty ResourceLeakDetector**：DISABLED/SIMPLE(默认)/ADVANCED/PARANOID 四级；1/128 采样（DEFAULT_SAMPLING_INTERVAL=128）；记录获取点栈（targetRecords 默认 4）；GC 时未 release → `LEAK:` ERROR 日志 + 可插拔 LeakListener。
- **背压**：Netty 写水位 32K/64K 纯信号不丢数据、带回差防抖动；Akka 有界 mailbox 满时**显式死信**（丢弃要可见）。→ `BoundedEventBus{capacity, 水位, overflow: DropOldest|Block(pushTimeout)|DeadLetter}` + 丢弃计数指标。
- **HikariCP 池数学**：小池饱和优于大池排队（`core*2+spindle`）；`maxLifetime` 须比基础设施断连短几秒 + **±10% 负向抖动防同步换血**；`leakDetectionThreshold`（0=关，最小 2000ms，出池超时 WARN+栈）。
- **Kafka producer**：retries=MAX_VALUE 不封顶、预算交给 `delivery.timeout.ms` 端到端 deadline（`≥ request.timeout + linger`）；幂等默认开 + in-flight ≤5 保序；退避指数增长带上限。→ 重试次数不设硬上限、由 Turn deadline 截断；仅对非幂等工具强制 maxAttempts=1。
- **gRPC deadline**：绝对时刻 Deadline 对象、传输时换算剩余时间防时钟偏斜；取消级联到所有后代 context；服务端周期检查 isCancelled。→ `min(perToolTimeout, turnDeadline)` 派发、嵌套用剩余时间而非重新计时。
- **故障注入**：Kafka Trogdor（进程内确定性故障）+ Toxiproxy（进程外 TCP 代理）；JMH 2,663★ 未达标仅注记。→ `FaultInjectingToolCallback{delay, failRate, hangForever, leakResource}`。
- **线程命名**：Kafka `kafka-producer-network-thread | clientId`、Netty 池名+全局池号+线程号。→ `buzhou-<role>-<短哈希>`，拒绝无名虚拟线程。

### 2.4 安全运维 + LLM serving（Vault 36.1K★ / Keycloak 36.2K★ / OPA 12.1K★ / Ollama 178.5K★ / vLLM 89K★ / SGLang 31.8K★ / Deno 108.3K★ / gVisor 19.1K★ / Trivy 37.4K★ / Gradle 18.8K★）

- **Vault Transit 版本化密钥**：rotate 产生新版本、旧版本永久保留可验签；**版本前缀嵌在载体里**（`vault:v3:...`），verify 从载体路由密钥而非信任方配置；min_decryption_version 收窄窗口。→ `SigningKeyRing{latestVersion, keyOf(v), minVerifyVersion}`，签名记录带 keyVersion 字段。
- **OPA bundle 热加载**：etag 条件拉取 + **下载-校验-激活原子切换**；失败沿用旧快照绝不部分生效；激活状态上报。→ `PolicySource.fetch(etag)` + 不可变快照原子替换 + provenance（bundleName+revision）写进决策。
- **Keycloak**：export/import 版本化 JSON、**机密不落导出文件**；内部 Liquibase changelog 管理 schema（注记：非公开接口）。
- **审计验证**：**Vault audit device 无条目间哈希链**（HMAC 是脱敏）；≥10K★ 项目中「审计链验证 CLI」**无现成先例**——buzhou ECDSA 链在此维度领先，验证工具需自定义形状：`AuditChainVerifier`（独立于生产进程）+ `VerificationReport{verifiedCount, firstBreakIndex, brokenRecordId}`；sessionHash 对外发布 + nightly 重放校验。
- **vLLM/SGLang/Ollama**：抢占分 recompute/swap（被逐出可降级重生成，非静默丢失）；KV 块 LRU **只回收引用计数为 0 的完整块**（in-use 不回收）；RadixAttention 前缀复用（召回排序给前缀连续性加分）；Ollama keep_alive（闲置卸载热缓存）。→ 逐出标注 `evicted=true+reason`；记忆闲置 N 分钟卸载热缓存。
- **Deno/gVisor 沙箱**：`@deno/sandbox` 支持 `timeout/memory(768MiB-4GiB)/allowNet(域名 CIDR 白名单)/KillController`；**输出上限截断**是共同教训。→ `SandboxLimits{timeout, memory, maxOutputBytes, netAllowlist}` + `CommandResult{truncated, killedReason(timeout|memory|output|manual)}`。
- **供应链**：SLSA 全家 <10K★ 注记；达标替代 = Gradle dependency verification（Java 同栈）+ Trivy 扫描（CI 门禁）。

## 3. 采纳映射（缺口 × 源 × 形状）

| 缺口 | 借鉴源 | 落地形状（详见 spec 13） |
|------|--------|------------------------|
| 停机无兜底 | Spring SmartLifecycle | 每机制 SmartLifecycle + phase 常量 + stop(callback) drain + timeout-per-shutdown-phase |
| Turn 挂死 | gRPC Deadline | Deadline 对象贯穿 + `min(perTool, turnDeadline)` + 外层 join 超时 |
| 双主风险 | HikariCP maxLifetime | 租约自动续租（宽限期内 renew）+ LeaseLost 抛出 + fence 写路径检查 |
| 事件拖垮主链 | Netty 水位 + Akka 死信 | BoundedEventBus + 丢弃可见性 |
| MySQL 二启炸 | Flyway 思想（注记）+ PG advisory lock | 版本化 schema 管理器（自建轻量版本表）+ 方言幂等修复 |
| 只进不出 | ES/etcd/ClickHouse/Temporal/PG 五公理 | SessionHistoryPolicy + ObservabilityTtl + MaintenanceTrigger + SessionQuota |
| 事务名存实亡 | —（正确性修复） | UoW 接线 + 竞态修复 + 脏数据隔离 |
| 不可诊断 | Spring FailureAnalyzer + Netty LeakDetector | BuzhouException 分类 + 错误码 + 泄漏检测 + 关键路径日志 |
| 配置不可调 | Spring 配置体系 | 全参数可配 + JSR-303 + 配置元数据 + FailureAnalyzer |
| guard 断层 | Vault/OPA/Deno | KeyRing 轮换 + 策略热加载 + SandboxLimits + 装配接线 |
| embedding 成本 | Milvus/pgvector | 向量落盘缓存（embed once）+ 惰性索引 |
| 韧性无测试 | Kafka Trogdor/Toxiproxy | FaultInjectingToolCallback + 故障场景矩阵 |

## 4. 出界清单（本轮不做）

- **多实例分布式**（lease 升级分布式锁/心跳、跨实例枚举）——沿用 effort #2 边界，单实例语义先行；PG advisory lock 仅作并发冷启动防炸的健壮性修复，不是分布式协调。
- **buzhou-observability/otel/dashboard 模块做深**——Health/MeterBinder 放四机制模块自身（optional 探测），不动观测模块。
- **独立 JMH 基准模块**（JMH 2,663★ 不达标）——性能护栏以现有 examples 集成测试承载，注记。
- **发布 Maven Central / SLSA provenance**——沿用 effort #1/#2 边界；Trivy CI 扫描可作注记建议不强制。
- **Firecracker/E2B 沙箱档完整实现**——沿用 effort #2 边界（SandboxLimits 形状先行）。
- **Reactor 背压/Rebuff/SLSA/cosign** 等不达标依赖——注记不引入。

## 5. 纠正与未查证

- **纠正**：Vault audit device 非哈希链（HMAC 是脱敏）；JMH 实测 2,663★ 不达标；Reactor 5,234★ 不达标；SLSA 全家均 <10K★；kafka 历史 soak 目录已删除。
- **未查证**：Temporal 动态默认保留期确切 config key；Milvus 自动 compaction 触发阈值；etcd quota-backend-bytes 默认值；kafka retry.backoff.ms=100ms（文档口径）；vLLM priority 调度稳定 flag 名；Boot 4.1 shutdown 默认值标注（30s 来自 Framework DefaultLifecycleProcessor）。
