# Wayfinder Map — K 会话 1200 系：全模块测试补全覆盖（effort #1200 总图）

> **K 会话**（2026-09-14 启动）：继 C（300）/ D（400）/ E（500）/ F（600）/ G（700）/ H（800）/ I（900）/ J（1000）之后的第九条自迭代线，主题定位：**测试集补全覆盖**（区别于 I/J 的机制增量线——本线以既有代码的测试缺口为选题源）。
> **号段裁决（号段声明先行）**：K 会话占用 spec **1200–1349**、票 **T1801–T2050**（每轮 2 张：shape + verify）、impl **903–1052**（每轮 1 片）、efforts **#1200–#1349**。本文件即占坑声明，先于 R1 动工提交入 main。
> **并存声明**：I 会话（effort-900.md）/ J 会话（effort-1000.md）与本线**互不触碰对方 map 文件**；每轮开工先 `git fetch` 双查 main，push 被拒即 `pull --rebase` 后重推。
> 用户常设授权（沿 F/G/H/I/J 会话）：**全程 AFK，不问用户**——每轮 = wayfinder（决策票）→ to-spec → to-tickets → implement → git 自动提交推送 GitHub。

## Destination

**全仓 16 模块测试补全覆盖完成**——以各模块 JaCoCo 实测（`target/site/jacoco/jacoco.csv`）为唯一证据源，每轮清一批零覆盖/低覆盖类；R1 目标＝**全部模块零覆盖类清零**（core 16 + guard 1 + spill 1 + resilience 1 + mcp 1，逐类直测），装配期豁免与匿名片段残留诚实入档；后续轮按缺口证据（低覆盖类、契约缝隙）持续轮进；周期性终验全仓 `mvn -B -ntp clean verify` 绿。

## Notes

- **证据口径**：零覆盖 = `LINE_COVERED=0` 且 `LINE_MISSED>=5`；低覆盖 = 覆盖率<50% 且 miss>=10。JaCoCo 报告按模块独立统计——**跨模块测试真实执行不计入本模块报告**（如 Spotlighting 被 guard 测试断言常量、TableContextWindowResolver 被 memory 测试真实执行，core 报告仍为 0），本模块直测仍是回归防线第一层，补测必要性不受影响。
- 每轮固定四步产物：决策票（同轮开+解决，Resolution 注明「用户常设授权 AFK、可推翻」）→ `docs/spec/<1NNN>-<slug>.md`（+README 行，SpecCoverageTest 门）→ impl 切片 → 模块测试。
- 测试纪律：JUnit 5 + AssertJ 静态导入；**无 Mockito**——手写 fake / 匿名类 / lambda stub（仓库惯例，testsupport 包为先例）；测试类与被测类同包（包私有可达）；类级中文 Javadoc 引用 spec/票号；样板 `PropertyInvariantsTest`（纯函数）/ `BuzhouGuardAutoConfigurationTest`（ApplicationContextRunner）/ `H2StoresContractTest`（内存库）/ `McpHealthHintsTest`（健康委托直接 new）。
- 代码规范：无魔法数字（static final 常量）、record/sealed 优先；模块依赖边界不变；测试域新增依赖须有同仓先例（h2/testcontainers 已在 guard/store-jdbc 使用，本线只复用不新引）。
- 排重纪律：每轮开工先 grep 近期提交与本 map 台账，避免与他线（I/J）新落地能力撞车。

## Decisions so far

- [全模块测试缺口审计与 R1 补测形态](../tickets/T1801-test-coverage-r1-shape.md) — JaCoCo 证据驱动：6 模块 20 个零覆盖靶点直测清零；豁免入档（AgentSession 匿名片段 11 行、BuzhouCoreAutoConfiguration SmartLifecycle 匿名类 9 行——装配期样板代码）；mcp 补 h2 test 依赖（同仓 guard 先例，非新引第三方）。
- [guard 健康 indicator 未随条件装配](../tickets/T1803-guard-health-indicator-crash.md) — R1 测试显形：`buzhou.guard.enabled=false`（或仅关审计）+ actuator 在 classpath 时启动崩溃（auditChainHealthIndicator 无条件要求条件装配的 AuditChainHealth）——indicator 内部类补同条件 @ConditionalOnBean，最小一行修复。
- [JdbcToolSetSpecStore CLOB 方言缺陷](../tickets/T1804-mcp-toolset-clob-dialect.md) — postgres:17 容器实证 `type "clob" does not exist`（真实 PG 部署 ensureSchema 必抛）——DDL CLOB→TEXT（PG/MySQL 原生、H2 同义）+ PostgreSqlToolSetSpecStoreTest（Testcontainers）锁方言回归。
- [ToolDenialLog 排序被 Map.copyOf 破坏](../tickets/T1805-tooldeniallog-mapcopyof-order.md) — R1 全量 verify 显形主干既有红（JDK 升级后 MapN 哈希布局变化翻出）：Map.copyOf 不保序打散排序结果——改 Collections.unmodifiableMap；教训：有序快照禁用 Map.copyOf。
- [低覆盖类批次 1 选题与补测形态（PolicyGateHook × RecallSearchTool）](../tickets/T1806-lowcoverage-batch1-shape.md) — R2：低覆盖档（<50% 且 miss≥10）证据驱动选题——guard 策略门四合同面（三态裁决映射/FIDES taint 组装/policy.decided 事件/指标三桶，OPA「input→decision+reason」合同思想）+ memory 召回工具十断言面（四模输出/摘要归一截断/降级与失败文案/轮次窗，ES partial-results 降级显式提示思想）；core 复扫靶点归批次 2。
- [PolicyGateHook 指标注释 tag 值失真](../tickets/T1808-policygate-metric-comment.md) — R2 补测显形：注释称 outcome=allowed|blocked|escalated，代码实际发射 allow|deny|escalate（Action 名小写，spec 13 无背书）——实际合同锁定 + 注释更正（零行为变化）；改 tag 值是部署侧可见行为变更，须独立 spec 决策。
- [skills RedisSkillStore 零覆盖补测形态（契约接入 + R1 审计遗漏修正）](../tickets/T1809-redis-skill-store-contract-shape.md) — R3：R1「全部 16 模块」审计漏扫 skills（结论未逐模块罗列的流程漏洞，诚实入档）——RedisSkillStore（cov=0/26）接 SkillStore 契约基类（Pact consumer-driven contract 思想，三实现同组断言）；Testcontainers redis:7-alpine 门控沿 store-redis 同款（不选手写 fake：60+ 方法 stub 是 Mockito 手工复刻且测不到真实 JSON/网络路径）；skills pom 补 testcontainers-junit-jupiter（test，根 POM 版本管理）；无 Docker 验证口径 = 编译绿 + 收集 + skip（CI 覆盖行为面）。
- [core 零覆盖尾巴清扫与判据收紧（AttachmentRenderer × CommandOutcome）](../tickets/T1811-core-zero-tail-sweep-shape.md) — R4：隔离 worktree 复扫（820 类）低覆盖档清空 → zero 判据收紧 miss≥5 → miss≥1（≥5 门槛藏住 CommandOutcome success() 这类小而行为敏感的谓词）；AttachmentRenderer default 截断合同（java.util 接口 default 测试思想）+ CommandOutcome 谓词矩阵（超时优先于退出码）；core 证据一律走隔离 worktree（主工作区复扫被并行会话构建竞争卡死，e84940f6 同源问题）。
- [ToolTimingAggregatorConcurrencyTest 负载下非确定性卡死](../tickets/T1815-tool-timing-concurrency-hang.md) — R4 验证显形：同 commit 一次 ~8 分钟全绿、一次 forked JVM 109+ CPU 分钟挂死（jstack 栈顶 record CAS 区，RollingMaxCounter 内联归因）——并行流内 yield 风暴恶化 FJ 调度 + 无超时护栏；最小修复 = 移除 yield + @Timeout(120) 护栏（测试侧语义不变，主代码活锁未证实）；并发压测默认带超时护栏先例确立。
- [SnapshotMessage 补测与收紧判据跨模块复核](../tickets/T1813-snapshot-message-and-tightened-sweep-shape.md) — R5：miss≥1 口径再浮出 SnapshotMessage（mis=2，compact 构造 null 防御）——null→空 Map / Map.copyOf 防御拷贝 / spillUri·evidenceId 透传；六小模块（tools/observability/observe-otel/observe-dashboard/spill/resilience）旧判据期报告隔离重扫清单化归 R6+；收敛信号：core 浮出量 R4=2 → R5=1，R6 起该口径并入周期性对账轮。

## R1 台账（spec 1200 / impl 903）

| 模块 | 零覆盖靶点（miss 行数） | 测试落点 |
|---|---|---|
| core | hook.Spotlighting（34） | SpotlightingTest（round-trip/锚点/降频） |
| core | token.TableContextWindowResolver（31） | TableContextWindowResolverTest（override 精确键/前缀/回退） |
| core | config.ConfigMaps（30） | ConfigMapsTest（叶子归一化/嵌套递归/空前缀） |
| core | session.SessionInterrupts（21） | SessionInterruptsTest（pending 推导/resumeWith 幂等） |
| core | spi.ToolSetSpec（19） | ToolSetSpecTest（构造校验/防御拷贝/sameConnection/visibleTo） |
| core | spi.CompositeAttachmentRenderer（17） | CompositeAttachmentRendererTest（拼接/截断/空防御） |
| core | recovery.RecoverySupport（16+6 匿名） | RecoverySupportTest（attach 装配面/onClose→COMPLETED） |
| core | observability.EventType（15） | EventTypeTest（of 恒等/注册幂等/非法入参） |
| core | recovery.RunStateTrackerHook（15） | RunStateTrackerHookTest（快照新建/推进/CONTINUE） |
| core | spi.EmbeddingProvider（12） | EmbeddingProviderTest（cosine 语义/防御归零） |
| core | metrics.CompositeBuzhouMetrics（9） | BuzhouMetricsComposeTest（compose 广播/default 委托） |
| core | hook.SessionStateHandle（5） | SessionStateHandleTest（default CAS 语义） |
| core | hook.OnFail（5） | OnFailTest（值序稳定护栏） |
| core | spi.SessionStateStore default 体（20） | SessionStateStoreDefaultsTest（裸实现驱动 5 个 default） |
| guard | config.BuzhouGuardHealthAutoConfiguration（11） | BuzhouGuardHealthAutoConfigurationTest（runner 三态） |
| spill | config.BuzhouSpillHealthAutoConfiguration（5） | BuzhouSpillHealthAutoConfigurationTest（runner UP/UNKNOWN/DOWN） |
| resilience | config.…ResilienceHealth（9） | BuzhouResilienceHealthAutoConfigurationTest（直接 new + runner） |
| mcp | store.jdbc.JdbcToolSetSpecStore（30） | JdbcToolSetSpecStoreTest（H2 内存库：懒建表/整表替换/round-trip） |

豁免入档（不追）：core.session.AgentSession 残余 11 行（内部匿名片段，主面已高覆盖）；core.config.BuzhouCoreAutoConfiguration SmartLifecycle 匿名类 9 行（装配期样板，装配语义由 starter SpecCoverage 域覆盖）。

## R2 台账（spec 1201 / impl 904 / T1806–T1808）

| 靶点 | 证据（2026-09-14/15 报告） | 测试落点 |
|---|---|---|
| guard policy.PolicyGateHook | cov=6 / mis=11（仅装配触达） | PolicyGateHookTest（9 用例：三态映射/Input 组装/taint 映射/事件字段/指标三桶/常量合同） |
| memory tool.RecallSearchTool | cov=26 / mis=30（无直测文件） | RecallSearchToolTest（12 用例：四模格式/归一截断/三类文案分支/降级与可用/倒序/轮次窗/limit） |

补测显形：T1808 指标注释 tag 值失真（零行为变化修正，单列 commit）。core 低覆盖批次待本轮 core 复扫证据归 R3。

## R3 台账（spec 1202 / impl 905 / T1809–T1810）

| 靶点 | 证据（2026-09-15 报告） | 测试落点 |
|---|---|---|
| skills store.redis.RedisSkillStore | cov=0 / mis=26（无测试文件；R1 审计漏扫 skills） | RedisSkillStoreContractTest（契约四用例 + 重启存活，redis:7-alpine 容器门控） |

验证：skills 134 测试 0 失败；契约 5 用例无 Docker 按设计 skip（收集完整 + 编译绿）；行为面声明限定 Docker 在场（CI）——store-redis/store-jdbc 既有口径。

## Not yet specified

- R2+ 候选（按缺口证据逐轮显形，不预切）：低覆盖类清点（覆盖率<50% 且 miss>=10 的后续批次）；store-jdbc / store-redis 容器测试在无 Docker 环境的降级口径；JaCoCo report-aggregate 聚合报告可行性（跨模块执行归一，能否消除「跨模块执行不计本模块」的统计盲区）；分支覆盖（BRANCH）维度是否纳入证据口径。

## Out of scope

- 不改任何被测主代码行为（若测试显形真实缺陷，修复须单列票与独立 commit，不在补测轮夹带）。
- 不引入 Mockito/新断言框架等新第三方测试依赖（仓库无 mock 惯例维持）。
- 不追 100% 行覆盖指标——豁免类（装配期匿名样板）诚实入档而非硬凑。
- 不触碰 I 会话（specs 90x / T125x-T145x / impl 65x-75x）与 J 会话（specs 100x-114x / T145x-T175x / impl 75x-90x）号段产物。
