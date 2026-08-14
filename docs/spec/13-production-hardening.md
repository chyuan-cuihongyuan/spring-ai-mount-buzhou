# 13 生产级收口：core / memory / spill / guard 运行时库外围防护层

> 本 Spec 由 wayfinder3 图（[MAP](../../.wayfinder3/MAP.md) + [T55–T68](../../.wayfinder3/README.md)）综合而成，事实源 = [docs/research/oss-production-grade.md](../research/oss-production-grade.md)（6 并行子 agent 2026-08-14：2 本地勘察 + 4 外部研究，载荷性结论已复核）。遵守仓库铁律「**改机制先改 Spec**」：本轮以新增防护层为主，凡触及机制语义变更处须同步修订 docs/spec 对应篇（文末「Spec 同步义务」）。领域术语以根目录 [CONTEXT.md](../../CONTEXT.md) 为准。
> **用户常设授权（2026-08-14）**：全程不需询问意见、按研究推荐迭代（可推翻）。

## Problem Statement

四机制经 effort #1/#2 已**功能完备**（Tier-1/2/3 全量落地、576 tests green），但以「生产级运行时库」标准横扫（研究文档 §0），存在七类**系统性外围缺口**，全部为跨进程/跨生命周期视角：

- **停机无兜底**：core AutoConfiguration 的 bean 无销毁回调；Runtime 不追踪 spawn 的会话——进程停机时在途 Turn 无人等待（executor 只 `shutdownNow` 硬中断）、租约无人 release（靠 90s TTL 自愈，多实例短窗口双主）；流式订阅者取消后 afterTurn/span 收尾全跳过；close 路径一个 listener 抛异常会跳过其余清理。
- **Turn 可永久挂死**：工具 join 外层 `get()` 无超时、组锁 `synchronized` 不可响应中断、permit 无界等待——一个不配合的工具挂死整条会话且无恢复手段；模型调用 core 层无超时包裹；租约**无续租**（长 Turn 期间静默过期被 steal → 双主），`LeaseLostException` 定义了但从不抛出。
- **存储只进不出**：MySQL 第二次启动必失败（索引无 IF NOT EXISTS）；无版本化迁移（加列对旧库不生效）；全仓没有 deleteSession——会话结束后 messages/state/spans/events/摘要/租约/spill 全残留；ToolCallLog、RunRegistry、摘要旧版本、EpisodeLedger、审计链全部无界增长；spill 的 TTL 清理有实现但无调度调用方；embedding 每次召回全量重算（成本无界）。
- **事务名存实亡**：JdbcUnitOfWork 注入却无任何 store 使用——「先删后插」两段自动提交、摘要 `MAX(version)+1` 读改写竞态、单条脏 JSON 炸掉整个会话加载；运行期写失败异常原样外溢打断当轮对话；摘要熔断连续失败后**永久**熔断。
- **生产不可诊断**：无异常分类与错误码；错误反馈识别靠中文字符串前缀匹配；全模块几乎零日志；后台任务静默吞异常；线程无名（thread dump 不可归属）；无健康检查、无指标。
- **配置面残缺**：并发 8 / 工具超时 60s / 租约 TTL 90s 全硬编码；无启动期校验（store.type 拼错 → 装配全不命中 → 运行期 NPE）；无配置元数据（IDE 无提示）；默认值偏开发态（spill 落进程 CWD、快照永不过期、hot-tail 预算 0=不限）。
- **guard 运维断层**：审计链纯内存（重启即失）且未进自动装配；签名密钥无版本化轮换、无加载路径；policy 不可热更、未接线；沙箱无内存/输出上限。

## Solution

以 ≥10K★ 开源项目为事实源，为四机制补齐**运行时库外围防护层**（不重做机制）：**core** 得到优雅停机（SmartLifecycle 分 phase + 在途 Turn 排空）、Turn Deadline 对象化贯穿（消灭挂起点）、租约自动续租 + LeaseLost 中止 + fence 写路径检查、有界事件总线（背压 + 丢弃可见性）、线程命名与异常隔离；**stores** 得到版本化 schema 迁移（MySQL 幂等修复 + PG advisory lock + 基线升级路径）、会话级联清理与保留策略族（封闭才计时、低频后台兑现、阈值四件套触发）、事务接线与并发正确性修复、写失败降级语义；**memory/spill** 得到容量配额（事实台账 noeviction、可再生集合可逐出）、spill 生命周期调度与孤儿回收、embedding 缓存（embed once）；**guard** 得到审计链持久化 + 密钥版本化轮换 + 独立校验工具、policy 热加载（快照原子替换 + provenance）、沙箱资源限额；**横切**得到异常分类与错误码、关键路径日志基线、资源泄漏检测、健康检查与指标（micrometer optional）、全参数可配 + JSR-303 启动校验 + 配置元数据 + FailureAnalyzer、故障注入测试基建。

## User Stories

1. 作为运维者，我希望**进程收到 SIGTERM 后在途 Turn 有序收尾**（新 Turn 拒绝、在途按取消模式排空、超时硬截断），这样滚动发布不打断用户对话半成品外泄。
2. 作为运维者，我希望**每个机制模块以 SmartLifecycle 分 phase 停机**且顺序确定（core 先停排空、持久层最后），这样停机期不出现「写入方已死、持久层先撤」的丢写窗口。
3. 作为运维者，我希望**停机排空有超时预算**（默认 30s 可配），这样停机不会被一个挂死工具无限阻塞。
4. 作为 Buzhou 接入方，我希望**流式订阅者取消后收尾照常执行**（span 关闭、turn 记账、afterTurn 钩子），这样取消不泄漏资源与账目。
5. 作为 Buzhou 接入方，我希望**一个不响应中断的工具无法挂死会话**——外层 join 有 deadline 兜底、组锁与许可等待限时化，这样最坏情况是单工具超时回喂而非整会话僵死。
6. 作为 Buzhou 接入方，我希望**Turn 预算是对象化 Deadline 并贯穿嵌套调用**（剩余时间传递而非每层重新计时），这样嵌套工具不会让总时长超过 Turn 预算。
7. 作为 Buzhou 接入方，我希望**模型调用受 loopTimeout/Deadline 兜底**（可配、默认保守不限但显式配置即生效），这样慢模型不无限占用会话。
8. 作为运维者，我希望**长 Turn 期间租约自动续租**（TTL/3 节奏、轮间 + 后台双路径），这样正常工作负载不再出现租约静默过期。
9. 作为运维者，我希望**续租失败（被 steal）立即以 LeaseLostException 中止 Turn**（在飞结果不入历史、Turn 不入 Completed-Turn），这样双主窗口内本地绝不写脏数据。
10. 作为运维者，我希望**过期租约被物理移除**且 TTL/续租间隔可配，这样内存不泄漏、参数可按部署调优。
11. 作为 Buzhou 接入方，我希望**事件监听器异常被隔离**（单个监听器抛异常不影响其余监听器与主链路），这样一个坏的观测插件炸不掉会话。
12. 作为 Buzhou 接入方，我希望**事件分发可选有界异步模式**（容量 + 水位 + 溢出策略，丢弃必须计数可见），这样慢监听器不拖慢 Turn 主链路。
13. 作为贡献者，我希望**全部线程有 `buzhou-<role>` 前缀命名**且带未捕获异常处理器，这样 thread dump 可归属、异常不蒸发。
14. 作为运维者，我希望**JDBC schema 有版本化迁移**（版本表 + 有序脚本 + 基线判定 + 加列路径），这样升级不靠手工 DDL、旧库能平滑演进。
15. 作为运维者，我希望**MySQL 重复启动不再失败**（索引幂等化）且**多实例并发冷启动有锁保护**（PG advisory lock / MySQL 锁），这样集群滚动重启无竞态。
16. 作为运维者，我希望**事件溯源日志与 Run 注册表有 JDBC 装配**（进 store 组合工厂），这样恢复设施在真实部署可用而非手工接线。
17. 作为运维者，我希望**删除会话时全量级联清理**（消息/摘要/状态/租约/观测/快照/工具日志/run/spill 文件/向量缓存一次清），这样存储不留孤儿。
18. 作为运维者，我希望**会话数据有保留策略**（从 closedAt 起算、默认 72h、改短不追溯、可归档），这样磁盘与 DB 用量有上界且审计需求可延长。
19. 作为运维者，我希望**观测流水有独立 TTL**（event/span 默认 7 天、低频批量清理），这样高频遥测不占生产主存。
20. 作为运维者，我希望**后台清理按定量公式触发**（基础阈值 + 比例因子 + 封顶 + 硬性兜底），这样清理既不空转也不风暴。
21. 作为运维者，我希望**摘要旧版本有修剪**（保留最近 K 版），这样压缩越频繁表不越爆。
22. 作为 Buzhou 接入方，我希望**多表写在同一事务**（工具日志、状态、run 注册表的先删后插原子化），这样崩溃窗口不产生半份数据。
23. 作为 Buzhou 接入方，我希望**摘要版本号生成原子化**（无读改写竞态），这样并发压缩不撞唯一索引崩溃。
24. 作为 Buzhou 接入方，我希望**单条脏数据不炸整个会话加载**（跳过 + WARN + 计数），这样一条损坏记录不废掉一个会话。
25. 作为 Buzhou 接入方，我希望**存储写失败策略可配**（默认 FAIL_TURN 保持既有语义；可选 DEGRADE 降级内存继续并告警），这样按场景权衡可用性。
26. 作为 Buzhou 接入方，我希望**摘要熔断有时间恢复与半开试探**（窗口后放行一次、成功清零），这样连续失败不再永久失去摘要能力。
27. 作为 Buzhou 接入方，我希望**InMemory 各 store 有容量上限与会话级移除**（事实台账超额明确拒绝而非 OOM；观测类可再生数据可逐出），这样内存套件也能长跑。
28. 作为 Buzhou 接入方，我希望**spill 目录有调度化的 TTL 清理、容量配额与启动孤儿扫描**，这样磁盘用量有界、崩溃残留可回收。
29. 作为 Buzhou 接入方，我希望**embedding 结果有缓存（embed once）**且三处消费方共用，这样召回成本不随历史线性放大。
30. 作为 Buzhou 接入方，我希望**EpisodeLedger 序号持久化**（重启不归零不覆盖），这样 episodic few-shot 不丢历史。
31. 作为 Buzhou 接入方，我希望**后台整理任务有队列上限、失败退避、会话摘除与关闭排空**，这样整理器自身不成为泄漏源。
32. 作为安全审计者，我希望**审计链持久化**（append-only 存储、失败明示降级）且**随 guard 自动装配**，这样审计能力默认在线而非手工拼装。
33. 作为安全审计者，我希望**签名密钥版本化轮换**（keyVersion 嵌入记录、旧钥只验不签、minVerifyVersion、文件加载路径），这样密钥轮换是运维动作不是代码变更。
34. 作为安全审计者，我希望**有独立于生产进程的链校验工具**（全量重放 + VerificationReport 定位首个断点），这样事后篡改可被证明。
35. 作为安全审计者，我希望**policy 规则可热加载**（etag 条件拉取 + 快照原子替换 + 失败沿用旧版 + provenance 进决策），这样策略变更不重启、决策可溯源到规则版本。
36. 作为 Buzhou 接入方，我希望**沙箱有资源限额**（输出字节上限 + 截断标记 + 终止原因），这样失控子进程不爆内存、截断不静默。
37. 作为 SRE，我希望**四机制有健康检查**（禁用报 UNKNOWN 不拖垮聚合）与**只读运维端点**（状态快照），这样 K8s 探针与排障有标准面。
38. 作为 SRE，我希望**关键行为有 Micrometer 指标**（未装 actuator 时零依赖 no-op、装了自动绑定），这样 Grafana 面板开箱即得。
39. 作为排障工程师，我希望**异常有统一分类**（可重试/不可重试/致命 + 结构化错误码）且**错误反馈识别不靠字符串前缀**，这样告警与策略能按类别自动化。
40. 作为排障工程师，我希望**租约/句柄/资源有泄漏检测**（采样分级 + 出租时长阈值 + 报告钩子），这样泄漏在测试期与生产期都可被发现。
41. 作为 Buzhou 接入方，我希望**并发/超时/TTL 等全部硬编码参数可配**且有启动期范围校验，这样调优不改代码、拼错配置启动即失败并得到人类可读修复指引。
42. 作为 Buzhou 接入方，我希望**IDE 有 `buzhou.*` 配置元数据**（默认值 + 枚举提示），这样接入不用翻源码。
43. 作为贡献者，我希望**有故障注入测试工具**（延迟/失败率/永久挂起/泄漏资源），这样韧性声明有测试背书。
44. 作为贡献者，我希望**自动装配有条件矩阵测试**，这样 optional 依赖的增删不产生装配回归。

## Implementation Decisions

> 不含具体文件路径/代码片段；接口级描述。「采纳」= 研究推荐 shape。Phase 划分供 `/to-tickets` 切片参考。

### 范围与阶段

- **Phase 0 地基**：Deadline 对象 + 挂起点修复（T57）；FaultInjectingToolCallback 测试构件（T67 前半）。
- **Phase 1 致命缺陷**：优雅停机（T56）；schema 版本化迁移 + MySQL 幂等 + 恢复设施装配（T60）。
- **Phase 2 正确性**：事务接线 + 竞态 + 脏数据隔离 + 降级语义 + 熔断半开（T62）；租约续租 + LeaseLost + fence（T58）。
- **Phase 3 治理**：级联清理 + 保留策略族 + 清理执行器（T61）；容量配额 + spill 生命周期 + embedding 缓存 + Episode 序号持久化（T63）。
- **Phase 4 运维闭环**：审计链持久化 + KeyRing + Verifier + policy 热加载 + SandboxLimits（T64）；异常分类 + 日志 + 泄漏检测 + 健康/指标/端点（T66）。
- **Phase 5 配置收口**：全参数可配 + JSR-303 + 元数据 + FailureAnalyzer + 默认值安全化（T65）；装配矩阵测试 + 韧性场景矩阵收口（T67 后半）。

### core（T56/T57/T58/T59 → §core-1..4）

1. **优雅停机与生命周期**（T56；源 Spring Boot 76K★ / Spring Framework 58K★）：每机制 AutoConfiguration 增补 `SmartLifecycle` bean；`BuzhouLifecyclePhases` 常量集中声明——**core phase 最大（最先 stop：拒绝新 Turn → 对在途发 AFTER_CURRENT_TURN 取消 → 排空等待）**，memory/spill/guard phase 较小（后 stop：只关各自后台任务与缓存），持久层语义最后撤离。`stop(Runnable)` 完成后必须回调；bean 容忍「没有 stop 直接 destroy」。排空超时 = `buzhou.lifecycle.timeout-per-shutdown-phase`（默认 30s）。`DefaultAgentRuntime` 追踪 spawn 的活跃会话（注册表，弱引用语义）；executor 关闭走 `shutdown() + awaitTermination(period)`，@Bean 用显式 destroyMethod 防推断双触发。`stream()` 补 doFinally（cancel/timeout 与正常完成同路收尾）。`close()` 与事件分发路径逐 listener try/catch（异常收集，绝不跳过后续清理）。
2. **Turn Deadline 贯穿与挂起修复**（T57；源 gRPC-Java 12.1K★ Deadline 语义 + Kafka delivery.timeout 端到端预算思想）：`TurnDeadline` 值对象（绝对时刻、`remaining()`/`isExpired()`/组合器）；工具派发时限 = `min(perToolTimeout, deadline.remaining())`；嵌套/子调用传递**剩余时间**而非重新计时。修复三个永久阻塞点：外层 join 用 deadline 剩余限时（超时按 TIMEOUT outcome 回喂）、组锁 `synchronized` → `ReentrantLock.tryLock(timeout)`、`acquire()` → `tryAcquire(timeout)`。模型调用在配置了 loopTimeout 或 Deadline 时受剩余时间兜底。与既有 `CancellationToken`（CancelMode 三档）**并列传播、不合并**（保护既有 API）；派发与 join 处统一取 min。`ToolSetSpec` 的 connect/request timeout 在工具执行器消费。
3. **租约续租与 fence**（T58；源 HikariCP 21.2K★ maxLifetime 抖动防同步 + 出租泄漏检测语义）：自动续租双路径——Turn 循环轮间 renew + 后台调度（TTL/3 周期）；renew 失败（租约已被 steal/过期不可再取）→ 抛 `LeaseLostException` → Turn 按「在飞结果丢弃、不入 Completed-Turn」中止（双主窗口本地零写入）；Turn 提交点（history 落库前）校验 fencingToken 仍持有（写路径 fence）。InMemory 租约过期物理移除；TTL/续租间隔全部可配。
4. **事件背压与线程卫生**（T59；源 Netty 35K★ 写水位 + Akka 13.3K★ 死信语义 + Kafka 线程命名）：事件分发**默认保持同步**（兼容）但补监听器异常隔离（逐个 try/catch + ERROR 日志 + 计数）；opt-in `buzhou.core.event-dispatch.mode=buffered`——有界队列（容量可配）+ 溢出策略枚举（DropOldest / Block(pushTimeout)），持久化类监听器建议 Block、遥测类建议 DropOldest；**丢弃必须可见**（计数器 + 低频汇总事件）。`BuzhouThreadFactory`（`buzhou-<role>-<seq>` 命名 + uncaughtExceptionHandler 统一 ERROR 日志）应用于全部线程创建点；`DbPolicyConfigProvider` 轮询异常修复（WARN + 指数退避 + 连续失败告警事件）。

### stores（T60/T61/T62 → §stores-5..7）

5. **Schema 版本化迁移**（T60；Flyway 思想注记、自建零依赖）：`buzhou_schema_version` 表 + classpath 有序迁移脚本（`V<n>__*.sql`，方言分目录）；启动期并发保护（PG advisory lock / MySQL `GET_LOCK`）；**基线判定**——库中已有表而无版本行 → 标记基线不重跑；首个正式迁移演示加列路径（如 `reasoning_signature` 对旧库的 ALTER）。MySQL 全部索引幂等化（IF NOT EXISTS / 存在性判定）。`JdbcToolCallLog` / `JdbcRunRegistry` 接线进 store 组合工厂（`BuzhouStores` 演进为含恢复设施的完整组合，二进制兼容方式：新组合形状 + 旧工厂保留 deprecated）。
6. **级联清理与保留策略族**（T61；五公理：封闭才计时（Temporal 22.3K★）/ 低频兑现（ClickHouse 49.2K★ merge_with_ttl_timeout）/ 阈值四件套（PostgreSQL 21.8K★ autovacuum））：SPI 增 `deleteSession(sessionId)`（default no-op）与 `prune(policy)`——各 store 实现（messages/summaries/state/lease/spans/events/snapshots/tool_call_log/run_registry + spill 文件 + embedding 缓存），core 提供 `SessionCleaner` 协调器一次级联。`RetentionSweeper` 后台执行器（默认 PT1H、可关）：会话保留 `SessionHistoryPolicy`（**锚点 = closedAt**、默认 PT72H、改短不追溯）、观测 TTL（event/span 默认 PT7D、批删限量）、摘要版本修剪（保留最近 K=3）、ToolCallLog 保留窗口（默认 PT7D，窗口外删除——恢复语义只保证窗口内）。触发采用 MaintenanceTrigger 公式（base + scaleFactor × 总量、封顶、hardFloor 兜底）。
7. **事务正确性与降级语义**（T62）：JdbcUnitOfWork 接线——tool_call_log / session_state / run_registry 的多表写与先删后插全部在 UoW 事务内；摘要版本生成改原子 UPSERT（方言分轨 PG `ON CONFLICT` / MySQL `ON DUPLICATE`）；`load` 逐条隔离脏数据（跳过 + WARN + `BuzhouDataCorruptionException` 计数，绝不炸整个会话）。写失败策略 `buzhou.store.write-failure-policy = FAIL_TURN | DEGRADE`（默认 FAIL_TURN = 既有语义；DEGRADE = 降级内存 + ERROR + 指标，适用于观测类写）。Redis UoW 连接池化（可配上限）。摘要熔断加 failureWindow（默认 PT10M）后半开试探、成功清零、计数随会话清理。

### memory+spill（T63 → §growth-8）

8. **增长治理与成本护栏**（T63；配额分可牺牲集合（Redis 76K★ volatile 族语义）+ embed once（pgvector 22.6K★/Milvus 45.6K★））：InMemory 各 store 有界化——`buzhou.store.in-memory.max-sessions`（默认 1,000）/ per-session 消息上限（默认 5,000）；**事实台账（message/summary/state）超额抛 `QuotaExceededException`（noeviction 语义，绝不静默丢）**；可再生集合（observability）容量触发逐出（volatile-lru 语义，采样近似）；会话 close 经 SessionCleaner 移除数据。RunRegistry COMPLETED 保留窗口（默认 PT24H）。spill：`deleteExpired` 由 RetentionSweeper 调度（PT1H）、`maxTotalBytes`/`maxFilesPerSession` 配额（超限拒绝落盘并回喂提示模型走显式分页）、启动孤儿扫描（引用会话不存在的文件，报告 + 清理）。`CachedEmbeddingProvider` 装饰（内容 hash 键、LRU 容量默认 512、进程内；RecallSearch / EpisodeLedger / SemanticChunkIndex 三处共用）。EpisodeLedger 序号从持久状态恢复（重启不归零）。SleepTimeScheduler：pending 队列上限（默认 64/会话，超限丢弃计数）、perSession 会话结束摘除、close 接线进生命周期、失败指数退避（cap 60s）。

### guard（T64 → §guard-9..10）

9. **审计链与密钥运维**（T64；源 Vault Transit 36.1K★ 版本化密钥——版本嵌载体、旧钥只验不签、窗口收窄）：`AuditRecordStore` SPI（JDBC append-only 表 + InMemory 有界环形）持久化审计记录；guard AutoConfiguration 接线（`buzhou.guard.audit.enabled` 默认随 guard 开；签名密钥缺失时降级纯哈希链 + WARN）。`SigningKeyRing`：记录嵌 `keyVersion` 字段，`rotate()` 原子切换 latest、旧公钥永久保留可验、`minVerifyVersion` 拒绝过老签名；`KeyProvider` SPI（PKCS#8 PEM 文件加载，配置指路径）。`AuditChainVerifier` 独立校验（输入导出记录集 + KeyRing → `VerificationReport{verifiedCount, firstBreakIndex, brokenRecordId, keyVersionStats}`）；`sessionHash` 随会话收尾发布，nightly 重放校验入红队同节奏流水线（注记级）。
10. **policy 热加载与沙箱限额**（T64；源 OPA 12.1K★ bundle 原子切换 + @deno/sandbox 资源形状）：`PolicySource`（classpath/file，etag = 内容 hash）+ `PolicyRefresher`（轮询默认 PT30S、可关）——拉取/校验失败**沿用旧快照**绝不部分生效；快照 provenance（revision + activatedAt）写入 `PolicyDecision`；PolicyGateHook 进自动装配。`SandboxLimits`（timeout、maxOutputBytes、可选 memory/netAllowlist）作为沙箱档配置；`CommandResult` 增 `truncated` 与 `killedReason(timeout|memory|output|manual)`；输出超限截断显式标记。DenoSandbox 的 `deno --version` 探测结果缓存（TTL 重探）。

### 横切（T65/T66/T67 → §cross-11..13）

11. **可诊断性**（T66；源 Spring Boot 健康协议 + Micrometer 命名约定 + Netty ResourceLeakDetector 四级采样 + HikariCP 出租阈值）：`BuzhouException` 统一基类（sealed 子类按 RetryCategory{RETRYABLE, NON_RETRYABLE, FATAL} 分类）+ `ErrorCode` 结构化枚举；工具错误反馈识别从字符串前缀改为结构化标记（消息词汇不变，识别走元数据）。日志基线：停机超时/续租失败/背压丢弃/清理失败/审计降级/沙箱拒绝/数据损坏 = WARN 或 ERROR（SLF4J 占位符风格）。`ResourceLeakDetector`（DISABLED/SIMPLE(默认)/ADVANCED/PARANOID + 1/128 采样 + 出租时长阈值 + LeakListener 钩子）挂在会话资源注册表、spill 句柄、租约三处。每机制 `HealthIndicator`（禁用/未启用报 UNKNOWN；DOWN 仅当机制无法履行核心职能）+ `@Endpoint(id="buzhou")` 只读快照 + 每机制 `MeterBinder`（`@ConditionalOnClass(MeterRegistry)`，未装时内部 no-op recorder）；指标命名 `buzhou.<mech>.<测量>`（小写点分、tag 值有界枚举、严禁 sessionId 进 tag）。
12. **配置校验与默认值安全化**（T65；源 Spring Boot 配置体系）：硬编码项全部入 properties（maxConcurrencyPerTurn、toolTimeout、leaseTtl+renewInterval、loopTimeout、eventDispatch{mode,capacity,overflow}、inMemory 上限、retention 族、spill 配额、sandbox 限额、policy 刷新间隔）。引入 jakarta.validation（@Validated + @Min/@Max/@NotNull；`store.type` 封闭枚举 fail-fast）。全量 `additional-spring-configuration-metadata.json`（默认值 + 枚举 hints）。`FailureAnalyzer`（store 装配失败、配置约束冲突 → description + action）。默认值修正（带迁移注记）：spill root-dir 默认改独立临时目录、redis snapshot-ttl 默认 PT168H、hot-tail maxInlineChars 默认 65536（0 仍表示不限）、jdbc dialect 缺省时按 DatabaseMetaData 自动探测（显式配置覆盖）。**兼容性原则线：新增默认只影响「此前即不安全」的路径，机制行为默认不变。**
13. **故障注入与韧性测试基建**（T67；源 Kafka Trogdor 进程内确定性故障思想）：`FaultInjectingToolCallback` 测试构件（delay / failRate / hangForever / leakResource / cancelMidFlight，装饰 ToolCallback）随 core test-jar 发布。韧性场景矩阵（主接缝 = examples 端到端 FakeChatModel 驱动）：挂起工具→deadline 兜底、慢监听→背压丢弃可见、坏监听→隔离、续租被 steal→LeaseLost 中止、写失败→双策略、脏 JSON→隔离恢复、停机→排空语义、熔断→半开恢复、MySQL 二启幂等（Testcontainers）、基线升级迁移、审计链篡改→verify 断点、密钥轮换→旧验新签、配额超限→明确拒绝、装配条件矩阵（ApplicationContextRunner：有/无 micrometer × enabled/disabled × 属性组合）。

## Testing Decisions

- **好测试只测外部行为**；主接缝不变：examples 端到端 agent session（FakeChatModel/record-replay 驱动）。韧性矩阵断言外部可观测行为：Turn 不挂死（有限时间内收尾并有 TIMEOUT 反馈）、停机后在途 Turn 的归宿、LeaseLost 后会话状态、配额拒绝的错误类型、链校验报告内容。
- **次接缝**：模块单测扩展（执行管理器、租约、事件总线、清理执行器、KeyRing、SandboxLimits）+ store 契约测试扩展（`AbstractBuzhouStoresContractTest` 范式增 deleteSession/retention 契约）+ Testcontainers（MySQL 二启、PG advisory lock、迁移基线升级）+ ApplicationContextRunner 装配矩阵。
- **故障注入**：FaultInjectingToolCallback 是韧性测试的唯一故障源（进程内确定性）；Toxiproxy 网络级注入不引入（注记）。
- **既有回归**：576 tests 全绿是门槛；既有用例语义不变（默认值修正如有行为面影响，须在对应测试同步并注明 spec 依据）。

## Out of Scope

- 多实例分布式接管（分布式锁/心跳/跨实例枚举）；PG advisory lock 仅作并发冷启动防炸。
- buzhou-observability / otel / dashboard / mcp / skills / tools 模块做深；观测模块零改动（指标经 MeterBinder 自动绑定，不动总线订阅面）。
- 独立 JMH 基准模块（JMH 2,663★ 不达标）；性能护栏由 examples 集成测试承载。
- 发布 Maven Central、SLSA provenance、Trivy CI 门禁、Toxiproxy（均注记）。
- Firecracker / E2B 沙箱档完整实现（沿用 effort #2；SandboxLimits 形状先行）。
- FIDES 二期、sub-agent / multi-agent、跨 agent 共享记忆（沿用 effort #2 边界）。
- embedding 缓存的持久化落库（本轮进程内 LRU；跨重启缓存重建成本可接受，持久化留 fog）。

## Further Notes

- **事实来源**：`docs/research/oss-production-grade.md`（star 数 2026-08-14 GitHub API 实测；五项载荷性本地结论已由主 agent 复核证实）。
- **决策票据**：wayfinder3 [T55–T68](../../.wayfinder3/MAP.md) 随本 Spec 批准而闭合（用户常设授权 ratify、可推翻）；执行切片 = `/to-tickets` → `.wayfinder3/impl/`。
- **Spec 同步义务**：优雅停机/Deadline/租约 → 修订 `05-parallel-tools.md`（或恢复链篇）；保留/清理/配额/迁移 → `09-modules-engineering.md` 增「存储运维」节 + `01-memory-compaction.md`（熔断半开）；审计/密钥/policy/沙箱限额 → `07-hooks.md`；配置/健康/指标 → `00-overview.md` 附录级提及 + 各机制篇配置节。
- **反模式（勿踩）**：停机只 shutdownNow 不排空；deadline 每层重新计时；续租失败静默继续写；逐出碰事实台账；清理挂写路径；迁移靠手工 DDL；字符串前缀当协议；泄漏只在生产发现；默认值变更破坏既有契约；丢弃不可见。
- **语言与许可**：文档与注释主语言中文；坐标 `io.github.chyuan-cuihongyuan:buzhou-*`，Apache-2.0。
