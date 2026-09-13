# 1200 — 全模块测试补全覆盖（R1）

> 来源：K 会话第 1 轮 = effort #1200（[T1801](../../.wayfinder/tickets/T1801-test-coverage-r1-shape.md) / [T1802](../../.wayfinder/tickets/T1802-test-coverage-r1-verify.md) / impl 903）。方法论：覆盖率缺口驱动测试（coverage-guided test completion，JaCoCo 实测为唯一证据源）——「测试集是否覆盖了细枝末节」不靠感觉，靠逐类行覆盖报告显形。

## Problem Statement

仓库质量门只卡 LINE ≥ 70%（模块级聚合），聚合线之下可以藏整类零覆盖：2026-09-13 JaCoCo 报告显示 6 个模块共 20 个类行覆盖为零（core 16 + guard/spill/resilience/mcp 各 1），合计约 330 行从未被任何本模块测试执行。其中包括行为敏感的面：`TableContextWindowResolver`（token 预算的窗口来源）、`SessionStateStore` 的 5 个 default 方法（被 InMemory 覆写遮蔽成死路径）、`RecoverySupport` 装配面（崩溃恢复的挂载点）、`JdbcToolSetSpecStore`（MCP ToolSet 清单持久化）。另有跨模块统计盲区：`Spotlighting`/`TableContextWindowResolver` 被 guard/memory 测试真实执行，但 JaCoCo 按模块独立统计，core 报告仍记 0——本模块直测缺失意味着他模块测试重构时这些类失去第一道回归防线。

## 目标

- **零覆盖清零**：20 个靶点逐类补直测（18 个测试文件），断言行为语义而非凑行数：
  - core（14 文件）：Spotlighting（wrap/unwrap round-trip、降频归一）、TableContextWindowResolver（override 精确键不走前缀、前缀大小写不敏感、未知回退 32K）、ConfigMaps（叶子归一化 Boolean/Long/Double、嵌套递归、空前缀空 Map）、SessionInterrupts（pending 推导、resumeWith 幂等注入、null→""）、ToolSetSpec（紧凑构造校验、防御拷贝、sameConnection、visibleTo）、CompositeAttachmentRenderer（拼接顺序、maxChars 截断、空防御）、RecoverySupport（attach 装配面、onClose→COMPLETED、null 防御）、EventType（of 恒等、自定义注册幂等、非法入参）、RunStateTrackerHook（快照新建/同快照推进、CONTINUE）、EmbeddingProvider（cosine 语义、防御归零）、CompositeBuzhouMetrics（compose 广播、default 委托）、SessionStateHandle（default CAS）、OnFail（值序稳定护栏）、SessionStateStore default 体（裸实现驱动 scanByKeyRange 边界/countByPrefix 前缀边界/compareAndSwap/scanByPrefix）；
  - guard：BuzhouGuardHealthAutoConfigurationTest（ApplicationContextRunner 三态 UP/UNKNOWN/DOWN + indicator 透传）；
  - spill：BuzhouSpillHealthAutoConfigurationTest（默认 UP、开关 UNKNOWN、rootDir 冲突 DOWN）；
  - resilience：BuzhouResilienceHealthAutoConfigurationTest（禁用 UNKNOWN/启用 UP+details 透传/null stats/runner 装配面）；
  - mcp：JdbcToolSetSpecStoreTest（H2 内存库：懒建表幂等、replaceAll 整表替换 + round-trip 保真）。
- **豁免诚实入档**：AgentSession 残余匿名片段（11 行）、BuzhouCoreAutoConfiguration SmartLifecycle 匿名类（9 行）——装配期样板，不硬凑，台账留痕。
- **证据复扫**：补测后重生成 JaCoCo 报告，确认靶点清零且豁免与台账一致。

## 实现决策

- 证据口径：零覆盖 = `LINE_COVERED=0 && LINE_MISSED>=5`；审计覆盖全部 16 模块（memory/tools/observability/observe-otel/observe-dashboard/store-jdbc/store-redis 无零覆盖类，结论入 map 台账）。
- 测试栈沿仓库惯例：JUnit 5 + AssertJ 静态导入；**无 Mockito**——手写 fake / 匿名类 / lambda stub；测试类与被测类同包（包私有可达，如 Spotlighting.datamark、ResilienceHealth 构造器）。
- mcp 模块补 `h2` test scope 依赖（同仓 guard pom 先例，版本走仓库既有依赖管理——非新引第三方）；JdbcToolSetSpecStore 测试用 `jdbc:h2:mem:<uuid>;DB_CLOSE_DELAY=-1` 零 Docker 秒级跑（H2StoresContractTest 同型）。
- **测试显形缺陷（单列票独立修复，不在补测轮夹带语义变更）**：
  - [T1803](../../.wayfinder/tickets/T1803-guard-health-indicator-crash.md)：guard 健康 indicator 未随条件装配——`buzhou.guard.enabled=false` + actuator 启动崩溃；indicator 内部类补同条件 `@ConditionalOnBean`；
  - [T1804](../../.wayfinder/tickets/T1804-mcp-toolset-clob-dialect.md)：JdbcToolSetSpecStore DDL `CLOB` 在 PostgreSQL 不存在（postgres:17 容器实证）——CLOB→TEXT + PG Testcontainers 方言回归；
  - [T1805](../../.wayfinder/tickets/T1805-tooldeniallog-mapcopyof-order.md)：ToolDenialLog `topDenials()` 排序被 `Map.copyOf` 打散（主干既有红，JDK 升级翻出）——改 `Collections.unmodifiableMap`。
- 纯测试增量：不改任何被测主代码行为，零配置默认行为逐位不变。

## 测试决策

- **好测试标准**：只测外部行为（公共 API 的输入→输出/状态变化），不测实现细节；每个断言对应一条可陈述的行为语义（如「override 是精确键匹配、不走前缀」），删掉实现后测试应仍成立。
- 测试 seam 全部取既有最高 seam：纯函数类直接静态调用；SPI 用 lambda/匿名类 stub；装配类用 `ApplicationContextRunner`（BuzhouGuardAutoConfigurationTest 先例）；JDBC 用真内存库（H2StoresContractTest 先例）；健康委托直接 new（McpHealthHintsTest 先例）。
- 验收门：各模块定向测试绿 + core JaCoCo 复扫零覆盖清零（豁免 2 项除外）。

## 兼容性

纯测试增量 + mcp test 依赖补充：主代码零变化、公共 API 面零变化（API 快照不再生）、既有测试零改动。

## Out of Scope

- 不追 100% 行覆盖；装配期匿名样板豁免入档。
- 不引入 Mockito 等新测试框架；不做分支覆盖/变异测试维度（R2+ fog：report-aggregate 聚合报告、低覆盖类批次、容器测试无 Docker 降级口径）。
- 缺陷修复仅限测试显形的最小一行级修复（T1803/T1804/T1805，各自单列票），不做相邻重构。
- 不触碰 I/J 会话号段产物与在跑主题。

## Further Notes

- 跨模块执行不计入本模块 JaCoCo 报告是 JaCoCo 按模块独立统计的固有口径，不是代码缺陷；本模块直测的独立性价值在于：他模块测试重构/删除时，本类仍有第一道回归防线。
- SessionStateStore default 体补测采用「裸实现驱动」：测试内只实现 5 个抽象方法、不覆写任何 default（SessionStateStoreContractTest 匿名走样写法），使 default 体从死路径复活为被测路径。
