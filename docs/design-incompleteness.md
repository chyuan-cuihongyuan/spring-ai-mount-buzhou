# 设计不完全方面清单（2026-08-17 全仓评审）

> 本文档由 code-review 双轴评审产出：以根提交 `a8704a4` 为定点、main@HEAD 全树为评审面，
> 6 个 Spec 轴子代理（56 份 spec + 顶层文档 ↔ 代码逐面对照）+ 4 个 Standards 轴子代理
> （CLAUDE.md / CONTRIBUTING.md 成文规约 + Fowler 气味基线），高严重度发现经人工独立复核 12 项。
> 用途：把「设计说了但没做 / 做了但没说 / 说做不一致 / 规约未自持」四类不完全面集中入档，
> 作为后续 effort 的开图输入。每条给出证据与建议方向；「硬偏差」= 明确矛盾或缺失，
> 「判断项」= 需设计裁定。

## 一、本次评审已同步修订的文档漂移（30 处，16 文件）

评审中已完成的无歧义同步（文档落后于代码事实类），不再列为待办：

| 文件 | 修订 |
|---|---|
| CLAUDE.md | spec 数「9 份机制详设」→ 01–55 共 55 份；「17 模块」→ 16 模块；issue tracker 指针由 `.scratch/` 改指 `.wayfinder/tickets/`（T1–T248） |
| CONTRIBUTING.md | 「九份机制详设」→ 55 份 |
| README.md | 机制详设范围 01–23 → 01–55；模块结构图与模块表补 `buzhou-resilience`（开关 `buzhou.resilience.enabled`，默认开） |
| CONTEXT.md | 公共面快照「449 类型 × 14 模块」→「466 类型 × 13 模块」（与 snapshot 实测一致） |
| docs/api-surface.md | 去除重复的 starter 空标题；449 → 466；补登漏列的 `BuzhouToolsProperties`（core config） |
| docs/ops-runbook.md | 快照计数 449 → 466 |
| docs/spec/00-overview.md | mermaid 依赖图补 `buzhou-resilience` 节点与边；机制索引补登 10–23、37–55 共 33 档；推演清单计数复核（85 → 89，01=10/02=12/04=9/07=15/08=8） |
| docs/spec/01 | 记忆 Advisor 位序 +200 循环外 → `ToolCallingAdvisor.DEFAULT_ORDER + 400` 循环内（3 处，03 号档已先行改记）；`InjectionViewBuilder` → `InjectionViewProcessor`（4 处）；包根/工具/范围读取归属按实现回写（buzhou-memory / buzhou-spill / core.spi） |
| docs/spec/04 | `db-enabled` 默认关 → 默认开（存在 SkillStore bean 时生效；spec 21 / impl-66 定型） |
| docs/spec/09 | 「16 模块含根」加注：effort#4 新增 resilience 后 reactor = 根 + 16 子模块 |
| docs/spec/15 | 「错误五类」原列六名：删 SERVER（5xx 并入 NETWORK，与 `ErrorCategory` 枚举一致） |
| docs/spec/23 | api-surface「14 模块 404 项」→「13 模块 466 项」 |
| docs/spec/52 | 评估会话 id 格式 `item<id>` → `i<id>`；run 完成事件通道「末项评估会话」→ 独立 `eval-<runId>-done` 会话（实现自记「诚实入档」） |
| redteam/baseline.md | 「4 plugins × 2 strategies」→ 6 plugins（impl-69 已扩 pii:direct / harmful:injury） |
| examples/README.md | 「四簇 demo」补扩充注记（demo/ 实测 31 个测试类） |
| SessionIndexQuery.java | javadoc「status null = 全部」→「null = 非 DELETED」（spec 33 §B，三实现一致） |
| WebhookDeadLetter.java / WebhookEventForwarder.java | 「重放由运维自建（out-of-scope）」注记作废，改指 `replayDeadLetters()`（spec 37 §B）；close() javadoc「限时 5s」→ 可配 close-drain-timeout（spec 44 §A） |

## 二、安全相关缺口（最高优先）

### S1【硬偏差】MCP 危险工具默认模式为空，HITL 挂接链断裂

- **spec 承诺**：spec 14 §F——「`dangerousToolNamePatterns` 客户端侧模式（**默认 delete/drop/write/update/remove/send/exec 类动词**），注册表聚合 `dangerousToolNames()` 供装配侧挂 guard HITL」（User Story 14：恶意 server 不能绕过审批）。
- **代码现实**：默认值为空列表（`McpModule.java:127`、`BuzhouMcpProperties.java:25`、`DefaultMcpClientRegistry.java:131` 注释自认「空 = 不登记」）；`dangerousToolNames()` 在 mcp 模块外**零消费方**（guard/starter 无引用，仅 health 端点读其 size）。
- **影响**：MCP server 提供的 `delete_*` 类工具默认不经任何人工审批即可被执行脊柱调用。
- **建议方向**：按 spec 14 落默认动词模式 + 在 guard 装配侧消费 `dangerousToolNames()` 自动登记 HITL；或裁定改 spec（明确「默认空、由业务显式配置」为定案）并删除 User Story 14 承诺。

### S2【硬偏差】危险工具 opt-in 启用时不自动带入默认守卫

- **spec 承诺**：spec 06 开关矩阵 + spec 07「opt-in 启用时自动带入默认守卫条目」；spec 06:125 另定 http_request 写方法粒度（GET/HEAD 不强制守卫）。
- **代码现实**：`ToolsModule.enabledDangerousToolNames()`（buzhou-tools `ToolsModule.java:94-100`）仅测试/示例消费；`BuzhouGuardAutoConfiguration.java:36` 明言「不自动耦合 tools」；http_request 写方法粒度无实现（整名入清单）。
- **影响**：Boot 用户显式打开 `write_file` 等危险工具后没有任何默认 HITL 拦截，与「safe-by-default」叙事相悖。
- **建议方向**：starter 层做装配期编排（tools 暴露清单 → guard 自动登记默认守卫条目），保持模块不直接依赖；或 spec 改记为「业务显式配置责任」并下调 README 安全叙事。

## 三、spec 承诺但代码未落地（功能缺口）

| # | 缺口 | spec 证据 | 代码证据 | 建议 |
|---|---|---|---|---|
| F1 | 运行期瞬断重试缺失（工具调用 1s/2s/4s 上限 3、IO 白名单、HarnessInternal span） | spec 05:96-102 | `HarnessToolCallingManager` 无重试逻辑（单次 `task.get`，:463） | 补实现或 spec 05 降级为「不重试」定案 |
| F2 | 工具级策略键无消费者（`buzhou.tool-policies.<name>.timeout-seconds/serial-group`） | spec 05:142,146 | 全仓零读取；serialGroups 仅注解通道（ToolsModule.java:156） | 补消费或删键 |
| F3 | Boot 注入通道缺失（starter 声明 `ToolCallingAdvisor.Builder` Bean + ConditionalOnMissingBean 替换） | spec 05:52-55 | main 代码无此 Bean | 补装配或 spec 回写 |
| F4 | spec 05 配置键整体漂移：`buzhou.parallel.*` 全仓零命中；并发上限 8 硬编码无 yml 通道 | spec 05:138-146 | `buzhou.core.tool-timeout`（BuzhouCoreProperties.java:114）；`HarnessAssembler.java:40` 硬编码 | **需裁定**：按实现重写 spec 05 键表，或补 yml 通道 |
| F5 | 精确缓存指标未落：`buzhou.cache.response.hit/miss/evicted`（MeterRegistry 可空，无 registry 时纯计数器可读 API） | spec 53 §E | 全仓零命中；`ResponseCacheStore.java:91-101` 仅内部 AtomicLong，`ResilienceModule.configure`:163-168 无 meter 注册 | 补 meter 注册 + 可读 API |
| F6 | DbPolicyConfigProvider 退避随机源不可注入（spec 要求 0.0/0.5/1.0 三点边界测试） | spec 50 §B Testing Decisions | `DbPolicyConfigProvider.backoffMillis` 直用 ThreadLocalRandom（:112）；且 spec 写 LongSupplier、实现为 DoubleSupplier | 补注入点 + 边界测试；回写 spec 类型 |
| F7 | canary.selected 事件 payload 缺 sessionId | spec 48 §B（钉「sessionId + model」） | `ResilienceAdvisor.java:234-236` payload 仅 {model, primary} | 补字段（事件面新增字段，兼容） |
| F8 | 会话索引业务标签自动装配路径无入口 | spec 30 US3 | `SessionIndexObserver.wiring()` 恒传 `Map.of()`（:44-46），仅公开构造可传 | 补装配入参或 spec 标注「编程面 only」 |
| F9 | `docs/config-reference` 全键表缺失 | spec 21:9（map 形态键「由 docs/config-reference 全键表补全」） | 文件不存在 | 生成该文档或修订 spec 21 承诺 |
| F10 | `AgentSession.resume()` 缺失（spec 07 推演#10 的续跑重放 API） | spec 07:327 | 仅 `SessionInterrupts.resumeWith`（spec 12 面），07 档未回写 | spec 07 回写指向 resumeWith（功能等价、名不同） |
| F11 | manager 聚合前 Spill 终检（「双路径幂等」）未见实现 | spec 05:47 | 仅 ToolResultLimiter（spec 31）；offload 全靠 Hook 层 | 判断项：主流程可覆盖则 spec 回写 |

## 四、代码已做但 spec/文档未入档（需回写或裁定）

1. **webhook 中断判 FATAL 直接死信**（`WebhookEventForwarder.java:213-215`）——spec 24 死信口径仅「4xx 即死 / IOException、5xx 重试」，中断死信未文档化（停机窗口事件只能靠 `replayDeadLetters` 补投）。
2. **损坏未决记录就地隔离为死信 attempts=-1**（`WebhookOutbox.java:173-181`）——spec 24 未规定，spec 37 §B 仅提「损坏死信重放时丢弃」；javadoc 已自记，spec 未回写。
3. **缓存键多采 model**（`ResponseCacheKeys.java:55` `options.getModel()`）——spec 53 §A 明文采样仅类名 + temperature/topP/topK/maxTokens。
4. **裸 `IllegalStateException("SHA-256 不可用")` 残留四处**——`ResponseCacheKeys.java:94`、`ResourcePolicySource.java:75`、`AuditChain.java:240`、`WebhookEventForwarder.java:227`（HMAC）；spec 50 §A 已封口应改 CONFIG_INVALID（同批 ArgumentFingerprint/ReadIntegrity 已合规迁移）。
5. **BuzhouHook 已扩为七切面**（`onModelError`，BuzhouHook.java:36，spec 15 落地）——spec 07「六切面」未回写；「编译 6 链缓存」亦未字面实现（HookChain.java:69 单链全遍历）。
6. **形状偏离（语义等价）**：spec 17 约定 RunCommandTool 构造重载注入 CommandBackend，实现为并列类 `SandboxRunCommandTool` 装配期二选一；spec 05 `SessionToolExecutor` 公共类不存在（per-session ExecutorService + 注册表等价达成，DefaultAgentRuntime.java:366-370）。
7. **未回写 04 档的增量**：`BuzhouMcpProperties.dangerousToolPatterns` / `shutdownBudget(35s)`、skills `SkillSearchTool`（注释指向 spec 21/37 等后续档）。
8. **guard test 依赖 buzhou-memory**（pom 仅 test 边）——与 spec 09「feature 模块严禁互依」字面相抵；建议 09 档追认「test 边豁免」或改夹具。

## 五、Standards 轴：成文规约硬违规（规约未自持）

> 规约源：CLAUDE.md「Java 代码规范」、CONTRIBUTING.md。以下为子代理报告经抽查的代表性证据，非穷尽清单。

1. **api/SPI Javadoc 系统性缺失**（「api 子包与 SPI 必须有 Javadoc」）：core 46 个公开类型无类型级 Javadoc，含规约自称的范式文件本身——`BuzhouHook.java:3`、`HookChain.java:13`、`AgentSession.java:6`、`AgentRuntime.java:3`、`MessageStore.java:8`、`HarnessToolCallingManager.java:31`；memory/spill/resilience 另有 43 个零 Javadoc 公开类型（含 `SpillStore`、`RangeReadEngine`、`MicroCompactor`、`SummaryGenerator` 等 SPI 级）。
2. **`@Bean` 读裸 Environment 约 13 处**（明禁）：`BuzhouCoreAutoConfiguration.java:146/212/242/255`、`BuzhouResilienceAutoConfiguration.java:44/66`、`BuzhouMemoryAutoConfiguration.java:53,80`、三个 Health 装配、`BuzhouObservabilityAutoConfiguration.java:48`、`BuzhouMcpHealthAutoConfiguration.java:25`、`BuzhouSkillsAutoConfiguration.java:46`、`BuzhouMcpAutoConfiguration.java:46`、`BuzhouRedisStoreAutoConfiguration.java:69-71`。
3. **日志未统一 SLF4J + 拼接 + 丢栈**：全仓 16+ 文件用 `System.Logger`（含公开类 `RunawayHook`、`WebhookEventForwarder`）；拼接+丢栈实证 `FeedbackExporter.java:82`、`RetentionSweeper.java:227`、`DefaultAgentRuntime.java:199/243/294`、`FactsExporter.java:61`、`EvidenceRefLedger.java:122`、`JdbcSessionIndexStore.java:135`。**注**：spec 13 §11 要求 SLF4J 基线，仓内 System.Logger 已既成风格——需裁定「spec 追认」或「代码整改」二选一。
4. **一把抓 `catch (Exception/Throwable)`**：core eval（`EvalDatasetStore.java:142/150`、`EvalRunner.java:182/191`，Jackson 应收窄 JsonProcessingException）；`HookedToolCallback.java:86/94` 静默吞异常返回占位；mcp `DefaultMcpClientRegistry.java:362/387` catch Throwable、`:422` 连 `InterruptedException` 一起吞且不恢复中断标志；guard `OnnxPromptGuard.java:19` 公开 SPI `throws Exception` 签名层面迫使调用方一把抓；memory/spill/resilience 约 20 处同型。
5. **`instanceof` 后强转**（明禁，应 pattern matching）：core policy `LayeredPolicy.java:52/58-59`、`ToolPolicyMatcher.java:48-52`；memory `MemoryModule.java:233-447` 约 18 处。
6. **魔法值口径不一**：advisor order 字面量 `ResponseCacheAdvisor.java:44`(+450)、`SemanticCacheAdvisor.java:59`(+460)、`SpillOffloadHook.java:56`(100)、`OnloadHook.java:27`(200)——同模块 `ResilienceAdvisor.java:68` 已抽 `CHAIN_ORDER_OFFSET`；spill 默认值 2048/20/32000 散落四处硬编码（`SpillProperties`、`SpillModule.java:40`、`MediaIntake.java:24`、`DiskSpillStore.java:72`）改默认值需散弹多文件。
7. **构造器执行业务逻辑/启线程**（明禁）：`AsyncObservabilityPipeline.java:59-60` 构造器内 `drainThread.start()` 且 `this::drainLoop` 提前逃逸；`DbToolSetProvider.java:41` 构造器内启动轮询调度。
8. **异常 message 缺上下文**：`DiskSpillStore` 7 处 `new BuzhouException(SPILL_IO_FAILED, "spill 磁盘 IO 失败", e)` 不带 uri/路径。

## 六、设计气味（判断项，摘重）

1. **契约实现漂移（最重）**：`DegradingObservabilityStore` 在 jdbc/redis 各一份且已分叉——jdbc 版（:112-113）有 `buzhou.store.write.failures{policy=degrade}` 指标，redis 版缺失；同名降级策略两库行为不一致。
2. **死代码与双轨规范化**：`AuditChain.java:160` `verifySignature` 无调用方（逻辑已迁 `AuditChainVerifier.SignatureOps:113`，方法体一字不差）；`Jcs.write/writeObject` 与 `writeNode`（52-147）双轨；`ArgumentFingerprint.canonicalJson:42` 与 Jcs 两套「规范化 JSON」——安全哈希材质口径有漂移风险。
3. **望远镜构造器**：`DefaultAgentSession.java:117-205` 七个构造器（10→16 参同前缀叠加）——Data Clumps，宜打包参数 record/Builder（叠加构造器属二进制兼容政策遗产，major 版收拢）。
4. **Divergent Change**：`DefaultAgentRuntime.java`（680 行）兼 spawn/fork/export-import/租约续约/优雅停机/全局监听器；导出导入（:222-310）与续约（:580-612）可拆协作者；另有三处同形状「遍历扩展点→try→catch→WARN 拼接」重复（:194-201/:236-245/:290-296）。
5. **Duplicated Code**：`ResilienceAdvisor.degradeFromCanary`(:326-387) 与 `fallbackOrRethrow`(:394-451) 候选循环整段同构；`ResponseCacheAdvisor.java:73-111` 与 `SemanticCacheAdvisor.java:91-126` 流式聚合装配逐行雷同；`MemoryModule.java:230-450` yml 子树解析样板重复 10+ 次；`ObservabilityAdvisor.java:228-240/:300-313` usage 记账块重复。
6. **Speculative Generality**：`DefaultSkillRegistry.load(appId, agentName, name)` 前两参被完全忽略（调用方只能 `load(null, null, name)`，LoadSkillTool.java:75）；`E2BSandbox.java:34`/`FirecrackerSandbox.java:36` 恒抛 UOE 的公开预留类型。
7. **Data Clumps**：`InjectionViewProcessor` 的 `(factsBlock, catalogBlock, currentTurn, sessionId, summaryTokenBudget)` 五元组穿行三方法。
8. **结构偏离**：memory/spill/resilience 三模块无 `api`/`internal` 分包——与 spec 09 包结构约定冲突，或需 09 档对「单包公开类型」追认。
9. **并发灰色地带**：`DefaultMcpClientRegistry.java:133`、`DbToolSetProvider.java:33` 用 `Executors.newSingleThreadScheduledExecutor`（规约明禁 newFixed/newCached，此项同属无界队列工厂方法，建议显式 `ScheduledThreadPoolExecutor`）。

## 七、文档间残留矛盾（未同步，需裁定口径）

1. **机制计数口径**：README「九大机制」+ 生产级纵深分节 vs CLAUDE.md「十大机制」（韧性层入列）——叙事 framing 差，建议统一为「9 + 韧性 = 10」或 README 升韧性为第十机制。
2. **perf 口径**：spec 51 §C「rateTurn 写入 ≤10ms 量级」 vs 哨兵硬顶 20ms（`PerfEffort10SentinelsTest.java:41`、docs/perf/baseline.md:56）。
3. **promptfoo star 数**：oss-perimeter-hardening.md:15/71/85 称 ~5K★ vs redteam/README.md:3 与 oss-perfect-tier23.md 称 24,206★。
4. **api-surface.md 主清单与 snapshot 数量口径差**：md 为散文清单（实测约 414 条），snapshot 466 条为黄金面——建议 md 头部注明「以 snapshot 为准」。
5. **spec 03 模块表归属残留**：`ObservabilityStore` SPI 实际在 `core.spi`（同 ToolSetSpec/ToolSetProvider），03 档模块表与 04 档未逐行回写（01 档本次已回写归属）。
6. **历史计数不可验**：spec 31「289 用例」等历史测试计数无重跑通道，不判漂移、建议后续档只写「全绿」口径。

## 八、处置建议（优先级排序）

1. **P0 安全**：S1（MCP 危险模式默认空 + HITL 断链）、S2（危险工具无默认守卫）——下一 effort 开图；若裁定「业务显式责任」则需同步下调 README/spec 安全叙事。
2. **P1 承诺兑现**：F5（缓存指标）、F6（jitter 注入）、F7（canary payload sessionId）——小切口、spec 已钉口径，直接补实现。
3. **P1 裁定**：F4（spec 05 键表）、五.3（SLF4J vs System.Logger）、六.1（降级存储两库分叉）——先要设计裁定，再动手。
4. **P2 回写**：第四节 8 项未入档行为 + 第七节文档矛盾，一次 docs effort 收口。
5. **P2 规约整改**：Javadoc 缺失（五.1）与裸 Environment（五.2）量大但机械，可配 formatter/规约检查插件批量做；catch 收窄（五.4）建议随触碰式整改。
6. **P3 重构气味**：第六节判断项随下一次 major 窗口或触碰式重构处理。

## 九、已验证一致面（抽样，审计覆盖佐证）

五 SPI 签名、Hook 三态/order、沙箱/黑名单/SSRF、租约 fence、unit-of-work、模块 imports 与开关矩阵；spec 24 outbox 全链路、33 §C scanByPrefix 三实现下推、25+35+41 熔断退避/半开多探测/Clock 注入、26 引用账本、28+36 导出扩展、29+33 fsck、30 索引 V3 迁移+契约矩阵、31 限幅、32/34/38 黄金 G1–G18 入 CI、35 目录溢出/MediaIntake、37 purgeOlderThan 三实现、39 背压+双 health、40 SpillCipher/TURN_IN_FLIGHT、42 checksum/读降级、43 限额与逐键 fail-fast、44 排空、45 G19–G21 与 perf 哨兵、46 TTFT/TPOT、47 MDC/rateTurn、48 稳定哈希粘选、49 shadow 护栏、50 四新码与 9 处迁移、51 G22–G24、52 评估全链路、54 Redis 固定窗/fail-fast、55 语义缓存位序与默认；配置键抽样约 25 键、runbook 端点与指标名、perf 基线常量、红队硬门 0.95/0 均与代码一致；测试实测 1258 个 @Test 全绿（提交记录：全仓 verify 累计 160 轮）。
