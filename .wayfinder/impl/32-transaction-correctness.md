# 32 — stores · 事务接线 + 并发正确性 + 降级语义

**What to build:** 崩溃窗口不产生半份数据：工具日志/状态/run 注册表的多表写与先删后插入进同一事务；摘要版本号原子生成（无读改写竞态）；单条脏 JSON 只跳过并告警、不炸会话加载；存储写失败策略可配（FAIL_TURN 默认 / DEGRADE 降级）；摘要熔断可恢复（窗口 + 半开）。

**Blocked by:** 31（同 schema/store 面，先立迁移基线）

**Status:** done

- [x] JdbcUnitOfWork 接线：tool_call_log / session_state / run_registry 写操作同事务
- [x] 摘要版本原子 UPSERT（PG ON CONFLICT / MySQL ON DUPLICATE 方言分轨）※实现形态等价偏移，见补全说明 §2
- [x] load 逐条隔离脏数据：跳过 + WARN + BuzhouDataCorruptionException 计数（29 号片类型）
- [x] buzhou.store.write-failure-policy = FAIL_TURN|DEGRADE（默认 FAIL_TURN=既有语义）
- [x] Redis UoW 连接池化（上限可配）
- [x] SummaryCircuitBreaker：failureWindow（默认 PT10M）半开试探、成功清零、计数随会话清理
- [x] 契约测试扩展：并发压缩版本唯一、脏数据恢复加载、双写失败策略行为

## 补全说明（2026-08-14 审计收口）

前一实现 agent 中途死亡后由本轮**逐项审计收口：7 项验收全部已落地且有测试证明，未发现缺口，本轮零代码改动**（主 agent 已先行验证三模块编译 + 既有测试全绿）。

### 1. 审计结论表

| # | 审计项 | 结论 | 证明文件（buzhou-store-jdbc / buzhou-store-redis / buzhou-memory） |
|---|--------|------|-------------------------------------------------------------------|
| 1 | UoW 接线（复用外层事务 / 未开自开短事务） | 已有 ✓ | `JdbcTransactions.inCurrentOrNew`（有事务复用、无则 TransactionTemplate 短事务、null 模板兼容旧自动提交）；`JdbcToolCallLog.append` / `JdbcSessionStateStore.put` / `JdbcRunRegistry.save` 三处多语句写全部包裹；恶意 UoW 回滚证明 = `JdbcRecoveryStoresTest.shouldRollbackAllRecoveryWrites_whenUnitOfWorkFailsMidway`（tool_call_log + run_registry + session_state 三写整体回滚、既有已提交事实不殃及）+ `shouldCommitAllRecoveryWrites_whenUnitOfWorkSucceeds`；`H2StoresContractTest` 两条 UoW 原子性契约 |
| 2 | 摘要版本原子化（无读改写竞态） | 已有 ✓ | `JdbcSummaryStore.save`：H2 乐观轨（无锁 max+1 + 撞唯一索引重试，上限 32 次）+ MySQL/PG 悲观轨（`SELECT ... LIMIT 1 FOR UPDATE` 序列化同会话写者）方言分轨；双线程各压 25 次 = `H2SummaryConcurrencyTest`（全成功、50 版本无重复、latest=50）；真实 DB 背书 = `MySqlStoresContractTest` / `PostgreSqlStoresContractTest` 各自的并发版本唯一用例（Testcontainers，无 Docker 自动跳过） |
| 3 | 脏数据隔离（跳过 + WARN + 计数） | 已有 ✓ | `JdbcMessageStore` 两阶段映射（raw 行 → 逐条解析，坏 JSON/坏枚举单条隔离）+ `BuzhouDataCorruptionException`（core.error，29 号片类型）+ `corruptionCount()`；`RedisMessageStore.load/findById` 同语义（LIST 逐条 / byId 直读）；测试 = `H2DirtyDataIsolationTest`（坏 tool_calls JSON / 坏 role / findById 命中坏行三例）+ `RedisDirtyDataIsolationTest`（jedis-mock，LIST 塞坏 JSON + findById 坏值） |
| 4 | 写失败策略（FAIL_TURN / DEGRADE 边界） | 已有 ✓ | 两模块 `WriteFailurePolicy` + `WriteFailurePolicyProperties`（`buzhou.store.write-failure-policy`，默认 FAIL_TURN）+ `DegradingObservabilityStore`（等价声明、零差异）；**DEGRADE 只装饰观测槽**（saveSpans/saveEvents/saveInjectionSnapshot，WARN + `degradedWriteCount()` 计数），事实类写（message/summary/state/lease）不经过装饰器任何策略下照常抛——两工厂（`JdbcBuzhouStores.createWithRecovery` / `RedisBuzhouStores.assemble`）均仅包装 observability 槽；测试 = `WriteFailurePolicyTest`（jdbc）+ `RedisWriteFailurePolicyTest`（redis）+ 两侧 `*AutoConfigurationTest.observabilityStoreWrappedOnlyWhenDegradePolicyConfigured`（默认裸 / DEGRADE 装配断言） |
| 5 | Redis 池化可配 | 已有 ✓ | `RedisStoreProperties.poolMaxSize`（默认 8，`RedisBuzhouStores.DEFAULT_POOL_MAX_SIZE` 同源）→ `BuzhouRedisStoreAutoConfiguration` 池 bean（`destroyMethod="close"`）→ `RedisUnitOfWork` 借还复用（`ConnectionPoolSupport` + testOnReturn 坏连接淘汰）；测试 = `BuzhouRedisStoreAutoConfigurationTest.transactionConnectionPoolAssemblesWithConfigurableMaxSize`（pool-max-size=3 断言 maxTotal）+ `RedisPooledUnitOfWorkTest`（池上限 1：串行借还复用 / 8 并发事务全提交 / 回滚后连接不污染后续事务） |
| 6 | 熔断半开 | 已有 ✓ | `SummaryCircuitBreaker`：`DEFAULT_FAILURE_THRESHOLD=3` / `DEFAULT_FAILURE_WINDOW=PT10M` 抽常量，构造 + `memory.summary-circuit-breaker.{failure-threshold,failure-window}`（MemoryModule 解析，ISO-8601 或秒数）双路可配；`Clock` 可注入；`SummaryCircuitBreakerTest` 7 例：达阈值开闸 / 窗口内保持关 / 窗口后半开放行一次（CAS 单探针）/ 探针成功清零回闭合 / 探针失败重计重新关窗 / `removeSession` 会话清理 / 闲置条目惰性淘汰 |
| 7 | 半成品坑扫描 | 无坑 ✓ | 三模块 grep TODO/FIXME 零命中；无空方法/桩实现；全部新增类（`JdbcTransactions`、两侧 `DegradingObservabilityStore`/`WriteFailurePolicy`/`WriteFailurePolicyProperties`、`SummaryCircuitBreaker`）均被生产或装配路径引用 |

### 2. 与任务书的实现形态偏移（语义等价，已注记在源码 javadoc）

- **UPSERT 分轨**：任务书建议的 PG `ON CONFLICT` / MySQL `ON DUPLICATE` 经实测不可行——H2 2.4.240 不支持两者（MySQL 兼容模式专属），且 upsert 形状无法可靠取回「本事务实际插入的版本号」（save 契约要求返回版本）。落地形态 = **H2 乐观轨（撞 `idx_summary_session_version` 唯一索引重试）+ MySQL/PG `FOR UPDATE` 悲观轨**，语义等价（版本唯一、消灭 `MAX(version)+1` 读改写竞态），H2/MySQL/PG 三方言并发测试背书。
- **H2 不走 FOR UPDATE 的原因**：H2 MVStore 锁定读在锁等待结束后仍返回等待前旧快照（实测），制造乒乓活锁；乐观 + 重试收敛有保障。

### 3. 已知边界（不阻塞本切片）

- `SummaryCircuitBreaker.removeSession` 清理入口已就位并测试；生产挂接点（SessionCleaner 级联清理）属 T61/切片 35，落地前由**窗口过期惰性淘汰**兜底防计数 map 无界增长（源码 javadoc 已注记）。
- H2 乐观轨在外层 UoW 事务内撞唯一键时：外层事务被标记 rollback-only，重试后最终以 `UnexpectedRollbackException` 整体回滚——**原子、无半份数据**（FAIL_TURN 语义），且 core 现无 UoW 生产调用方包裹摘要写、MySQL/PG 走悲观轨该窗口近乎不存在；理论残余，留注记不改（修复需 REQUIRES_NEW 传播，超出本切片面且无测试运行窗口验证）。
- spec 同步义务（`01-memory-compaction.md` 熔断半开 / `09-modules-engineering.md` 存储运维节）未随本切片执行——超出允许改动面（本切片仅限三 store/memory 模块），待 spec 同步批次。

### 4. 测试资产清单（本切片新增/扩展，全部现有通过）

`buzhou-store-jdbc`：`H2SummaryConcurrencyTest`、`H2DirtyDataIsolationTest`、`WriteFailurePolicyTest`、`JdbcRecoveryStoresTest`（+2 条 UoW 事务契约）、`H2StoresContractTest`（+2 条）、`MySqlStoresContractTest` / `PostgreSqlStoresContractTest`（各 +并发版本唯一）、`config/BuzhouJdbcStoreAutoConfigurationTest`（+DEGRADE 装配断言）
`buzhou-store-redis`：`RedisPooledUnitOfWorkTest`、`RedisDirtyDataIsolationTest`、`RedisWriteFailurePolicyTest`、`RedisStoresContractTest` / `RedisStoresTestcontainersTest`（切到 `createPooled` 装配）、`config/BuzhouRedisStoreAutoConfigurationTest`（+池上限可配、DEGRADE 装配）
`buzhou-memory`：`SummaryCircuitBreakerTest`（7 例半开恢复契约）
