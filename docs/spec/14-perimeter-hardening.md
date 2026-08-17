# Spec 14 — 外围模块生产级收口（perimeter hardening）

> effort #4 建造 Spec（/to-spec 产出，ready-for-agent）。
> 决策依据：effort #4（[map](../../.wayfinder/maps/effort-04.md)）T69–T79 决议 + [docs/research/oss-perimeter-hardening.md](../research/oss-perimeter-hardening.md)。
> 前置：spec 13（core/memory/spill/guard 收口）已全部落地（impl 28–43）。

## Problem Statement

core/memory/spill/guard 四机制模块已达生产级标准（生命周期、错误分类、日志、泄漏检测、健康指标、配置校验与元数据、韧性矩阵），但仓库其余面呈现「文档先行、实物滞后」状态：

- **buzhou-resilience 只存在于未合并分支**——main 上模型调用（重试/超时/限流）零兜底，而 CLAUDE.md/spec 11 已引用该模块；
- **观测三模块**停在功能完备层：observability 管线非 Spring bean（context 刷新即泄漏）、全模块零日志、流取消泄漏 RUNNING span、指标与 core 双轨；otel 桥 openSpans 无界增长、exporter 模式静默回退；dashboard **零鉴权绑 0.0.0.0** 且 Skill 写端点裸奔、500 回显内部异常；
- **mcp/skills/tools** 停在功能正确层：MCP 连接工厂零真实协议测试、DB 清单源不接装配；skills 无持久化 store、扫描静默吞错；tools 的 run_command 取消路径泄漏孤儿进程、子进程继承全部环境变量、读入无上限可 OOM；
- **装配基建失效**：全仓没有 spring-boot-configuration-processor，impl/43 手写的 additional-metadata 是死文件；JSR-303 只覆盖 core；
- **redteam 门测不到护栏**：target 返回硬编码文案，promptfoo 无法区分拦截与绕过；基线文件缺失、nightly 承诺的审计重放 job 不存在；
- **CI 无覆盖率无静态分析**，文档模块口径三方矛盾。

生产环境接入方在上述任一面上都会遇到不可诊断、不可运维或不安全的实际故障。

## Solution

把 effort #3 为 core 四模块建立的生产级标准**平移到全部剩余面**，并补齐三条真增量机制（模型韧性、失控检测、会话容量闸）。具体：

1. **resilience 增量移植**：buzhou-resilience 模块（重试/退避/错误分类/超时取消/onModelError/流式 + RPM/TPM 限流）、core/runaway 失控检测（步骤/工具/墙钟/会话累计四层硬顶 + 确定性重复检测 + 软退出通道）、core/backpressure SpawnGate（会话并发容量闸 + OverloadPolicy 两档）从 `Future-needs-to-be-supplemented` 分支移植到 main 现架构，全部对齐 core 生产级标准（日志/健康/指标/配置校验元数据/BOM/starter 接线）。
2. **观测三模块收口**：管线 bean 化与排空、日志基线、span 终态完整、有界化、指标单口径（收敛进 core MeterBinder）、dashboard 对齐 Actuator 安全模型（默认 loopback、非 loopback 强制鉴权、错误不回显、输入有界）。
3. **mcp/skills/tools 收口**：真实协议层集成测试、装配接线 DB 源、持久化 SkillStore（JDBC/Redis）、失败可见化、取消穿透到子进程、环境白名单、读入上限、客户端侧 MCP 危险工具登记。
4. **基建生效**：configuration-processor 全模块生效并加构建断言、JSR-303 全量、FailureAnalyzer 扩面、BOM 补全、starter 正反向用例。
5. **门禁真实化**：redteam target 透传真实回复与拦截信号（promptfoo guardrails 契约）、基线落档、nightly 审计重放、CI 覆盖率/静态分析观测段上线。

## User Stories

1. 作为接入方，我希望引入 `buzhou-spring-boot-starter` 后模型调用自动获得重试/超时/限流兜底， so that 上游模型抖动或限流不会直接打断我的 agent 会话。
2. 作为接入方，我希望模型韧性行为可按 provider 配置（重试类别、退避上限、deadline、RPM/TPM）， so that 我能对不同供应商设置不同的容忍度。
3. 作为运维，我希望单轮/单会话有步骤数、工具调用数、墙钟、累计 token 四层硬顶与软退出， so that 失控循环不会被算力账单终结。
4. 作为运维，我希望会话并发有容量闸（满载拒绝新 spawn 并给出结构化异常）， so that 突发流量不会拖垮整个运行时。
5. 作为运维，我希望观测管线随 Spring context 关闭而排空关闭， so that devtools/测试上下文反复刷新不会累积泄漏线程。
6. 作为运维，我希望观测/桥接层的吞异常点都有 WARN 日志与失败计数， so that 观测系统自身故障在生产可发现。
7. 作为运维，我希望指标只有一个预注册家族（core MeterBinder）， so that Grafana 面板不会被双轨指标搞乱。
8. 作为运维，我希望流被取消时 span 落 CANCELLED 终态、otel 桥的未终态 span 有界驱逐， so that 长跑进程内存稳定、trace 无空洞。
9. 作为安全负责人，我希望 dashboard 默认只绑 127.0.0.1， so that 引入依赖不会默默多出一个公网管理端口。
10. 作为安全负责人，我希望 dashboard 绑定非 loopback 时必须配置 token 否则拒绝启动、写端点校验 Bearer， so that Skill 正文（=模型行为）不可被任意网络客户端篡改。
11. 作为安全负责人，我希望 dashboard 500 响应不回显内部异常、请求体与分页有上限， so that 管理端口被扫描时不泄露内部信息也不可被 DoS。
12. 作为接入方，我希望 MCP 连接工厂有真实协议层集成测试覆盖， so that 升级 MCP SDK 时能靠 CI 发现握手/工具发现回归。
13. 作为接入方，我希望定义 `ToolSetSpecStore`/`SkillStore` bean 后自动装配即启用 DB 清单/技能源， so that 不必手工写装配代码。
14. 作为安全负责人，我希望外部 MCP server 的工具按客户端侧危险动词模式自动进 guard HITL 名单， so that 恶意 server 不能靠自报元数据绕过审批。
15. 作为运维，我希望 classpath 里 SKILL.md 写坏时启动日志有失败名单与计数， so that 技能不会静默消失。
16. 作为接入方，我希望 skills 有 JDBC/Redis 持久化 store 且通过统一契约测试， so that 生产动态技能重启不丢。
17. 作为安全负责人，我希望 run_command 在 turn 被取消时把整棵进程树杀掉， so that 取消不留下孤儿进程继续消耗资源。
18. 作为安全负责人，我希望子进程只拿到白名单环境变量， so that 数据库密码/API key 不会漏给模型驱动的 shell。
19. 作为运维，我希望 read_file/http_request/write_file 有读入/写入上限，超限返回可读错误， so that 一个大文件/大响应不会 OOM 整个运行时。
20. 作为接入方，我希望所有 `buzhou.*` 配置键在 IDE 有补全与校验（元数据 + JSR-303）， so that 拼错键名在开发期就暴露。
21. 作为接入方，我希望典型非法配置组合有 FailureAnalyzer 翻译， so that 启动失败时能直接读出修法。
22. 作为外部使用者，我希望 import buzhou-bom 后能引 core test-jar（FakeChatModel 等）， so that 我可以按同样范式写集成测试。
23. 作为安全负责人，我希望红队评测的评分对象是真实 agent 回复与真实护栏决策， so that 门禁数字反映实际攻防结果。
24. 作为维护者，我希望 nightly 跑审计链重放校验， so that README 的承诺有 CI 背书。
25. 作为维护者，我希望 CI 有覆盖率报告、spotbugs 与 CodeQL 观测段， so that 质量趋势可见且不阻塞日常迭代。
26. 作为维护者，我希望文档模块口径、配置表、文档索引与实际一致， so that 新人按 README 能走通。
27. 作为新人，我希望 examples 里有一个基于 starter + application.yml 的可启动 Boot 工程， so that 十分钟内看到端到端效果。

## Implementation Decisions

### A. resilience 增量移植（T70）

- **不合分支**。`buzhou-resilience` 模块整块移植（自包含：仅依赖 buzhou-core + spring-ai-client-chat）；`core/runaway`、`core/backpressure`（SpawnGate/OverloadPolicy）移植时适配 main 当前 hook 链、`BuzhouCoreProperties`、session 生命周期 API。
- 分支的 crash-recovery、graceful-shutdown 与 `.scratch/`、`docs/production-readiness/` **不移植**（与 main impl-30/34 平行或已覆盖）；分支 spec 有效内容并入 `docs/spec/15-model-resilience.md`（机制详设）与本 spec 对应节。
- 模块语义：ResilienceAdvisor（adviseCall/adviseStream 双路、指数退避 clamp、deadline 专用 executor、ModelCallInFlight 取消在飞调用）、ErrorCategory 五类 + ProviderErrorClassifier SPI、RateLimitAdvisor（RPM/TPM 双桶 + queueTimeout + OverloadPolicy 两档）、onModelError 钩子、runaway 四层硬顶（步骤/每工具每轮/墙钟/会话累计双窗口）+ 确定性重复检测 + 软退出（RunawayBudgetRenderer 注入剩余预算文案）、SpawnGate（refuse-new + SessionCapacityExceededException）。
- advisor order：RateLimitAdvisor 在 ResilienceAdvisor 之前（先闸后重试），两者均晚于观测 advisor。
- 对齐项：日志基线（重试 WARN 含分类与下次退避、限流拒绝 INFO、runaway 软退出 WARN）、健康（最近错误分类/重试耗尽数/限流拒绝数/失控触发数）、指标并入 core MeterBinder 家族、`ResilienceProperties`/`BuzhouRunawayProperties`/`BuzhouBackpressureProperties` JSR-303 + 元数据、BOM/reactor/starter 聚合接线。
- 熔断（circuit breaker）与重试预算：**不做**，注记开放问题。

### B. observability（T71）

- AsyncObservabilityPipeline 经 `BuzhouObservabilityAutoConfiguration` 暴露为 bean（destroyMethod=close）；JVM shutdown hook 仅保留在非装配（编程式）路径。
- SynchronousObservabilityPipeline.doEnqueue 失败语义与 async 对齐：吞 + 计数 + WARN，不上抛主链路。
- adviseStream 补 doOnCancel/doFinally：取消时 MODEL_CALL span 终态 CANCELLED。
- ObservabilityConfig → @ConfigurationProperties + JSR-303（batchSize≥1、flushInterval/flushTimeout 非空非负、maxChildren≥0）。
- **指标家族收敛**：MicrometerDualWriter 平行家族删除；queue.wait/persist.errors 语义并入 core BuzhouMetricsBinder 预注册（新增 timer/counter 定义）；duration 记录改走 core 家族。
- 注入快照：ToolResponseMessage 文本提取 evidence-id 模式最小实现；死代码删除；OpenAI 判定加 provider 显式配置键。

### C. otel（T72）

- openSpans 上限（默认 10_000）+ 最旧驱逐（end=UNSET + buzhou.evicted=true）+ 驱逐计数；sessionTrace 会话结束清理 + 同上限。
- 三处静默 catch → WARN 限频日志。
- exporter-mode=tracer 且容器无 Tracer bean → 启动失败（BuzhouConfigurationException）；exporterMode 枚举校验。
- OTLP headers/timeout 可配（headers 平铺键支持 ${ENV:} 占位）。
- javadoc 前缀修正；pom 冗余 test 依赖删除。

### D. dashboard（T73）

- `bind-address` 默认 `127.0.0.1`；**绑定非 loopback 且未设 auth-token → 启动失败**（BuzhouConfigurationException + FailureAnalyzer）；非 loopback + token → 启动 WARN。
- Bearer token 鉴权：设置后全部 API 与静态页要求 Authorization 头，缺失/错误 401 JSON。
- 500 → `{"error":"internal_error"}` + 服务端 ERROR 日志；请求体 1MB 上限（413）；分页 size clamp [1,200]。
- executor 在 stop() 中关闭；esc() 补 `&#39;`；pathPrefix 禁止与 /api 冲突；启动 INFO/WARN 与路由错误日志基线。
- 测试移除跨模块 import core.internal。

### E. tools（T76）

- run_command：InterruptedException 与超时统一 finally 走 killProcessTree；环境变量白名单（默认 PATH/HOME/LANG/LC_ALL/TZ/TERM + envAllowlist 追加）。
- 读入上限：read_file 按 Files.size 预检（默认 8MB）；http_request Content-Length 预检 + 流式截断兜底；write_file content 上限 8MB + temp+ATOMIC_MOVE。
- http_request：timeoutSeconds 上限校验（≤300s 默认）；连接级头黑名单（Host/Content-Length/Transfer-Encoding/Connection）。
- SsrfGuard 补 fe80::/10。
- core DEFAULT_TOOL_TIMEOUT 60s → `buzhou.core.tool-timeout` 可配（默认 60s）。

### F. mcp（T74）

- 进程内真实协议集成测试：MCP Java SDK StreamableHttp server（含 InMemory transport 形态）覆盖握手/listTools/工具调用/断连；STDIO 用 SDK stdio server 进程验证。
- AutoConfiguration 接线 `ObjectProvider<ToolSetSpecStore>`。
- 配置 fail-fast（transport 枚举/endpoint 非空/timeout 正数）+ properties 化 + JSR-303 + 元数据。
- 健康（ACTIVE/DRAINING/CLOSED 计数 + 最近建连失败）+ 指标（mcp.connections.* 族并入 core 家族）+ 日志基线（refresh 差量 INFO、建连失败 WARN、forceClose WARN）。
- close() 总预算可配（默认 35s），超时放弃等待仅强杀。
- `dangerousToolNamePatterns` 客户端侧模式（默认 delete/drop/write/update/remove/send/exec 类动词），注册表聚合 `dangerousToolNames()` 供装配侧挂 guard HITL（与 tools 模块同挂点）；HTTP env 注入的机密值日志脱敏。
- 快照重发现不做（注记）；拒绝文案英文化并对齐 ToolErrorFeedback。

### G. skills（T75）

- store-jdbc/store-redis 新增 SkillStore 实现（沿既有 schema 迁移体系；Redis hash+索引集合）+ ToolSetSpecStore JDBC 实现；契约测试新增 SkillStore 契约基类（沿 AbstractBuzhouStoresContractTest 范式）。
- AutoConfiguration 接线 `ObjectProvider<SkillStore>`。
- 扫描失败可见化：IOException/解析失败 WARN + 启动汇总（成功/失败计数 + 失败名单 DEBUG）。
- frontmatter 支持 `|`/`>` 多行 description 与列表式 allowed-tools。
- 资源单文件上限（默认 1MB，超限跳过+WARN）；二进制（非 UTF-8）跳过+WARN（文本模型注记）。
- resolve 缓存按 (appId,agentName)，SkillAdminApi 写路径统一失效。
- load_skill 正文上限（默认 512KB）；setBinding 校验 skillName 存在；version 冲突文案对齐 ToolErrorFeedback。
- SkillsProperties JSR-303 + 元数据。

### H. 装配与配置基建（T77）

- 根 pom pluginManagement：maven-compiler-plugin `annotationProcessorPaths` = spring-boot-configuration-processor；凡 @ConfigurationProperties 模块全量生效；构建断言：测试抽查各 jar spring-configuration-metadata.json 存在且含本模块键。
- impl/43 的 4 份 additional-metadata 随 processor 合并生效（核验无冲突）。
- JSR-303 扩面至全部 properties record。
- FailureAnalyzer 新增：dashboard 安全配置、resilience 矛盾组合（deadline < maxBackoff 等）。
- BOM 补 buzhou-core test-jar 与 buzhou-resilience。
- starter：聚合 resilience；补反向用例（memory.enabled=false、store.type=redis 缺 Redis 启动失败）与 @SpringBootTest 全上下文冒烟。

### I. redteam（T78）

- 目标端点：响应体 = 真实 agent 回复（chat() 产出或拦截文案）；guard/HITL 拦截时响应头 `x-buzhou-guard-blocked: true`。
- promptfooconfig：provider transformResponse 派生 `guardrails.flagged`；注入类用例 `type: guardrails` 断言（拦截=pass）。
- `redteam/baseline.md` 首版基线（指标快照 + F1 口径注记）。
- nightly：audit-chain 重放 job（复用 examples AuditChain 集成测试）；升硬门标准写 README（连续 2 次 nightly 全绿 → 移除 || true）。
- 请求解析 JSON 化（解析失败 400）；README 类名修正。

### J. CI（T79）

- jaCoCo 全模块（prepare-agent + report，CI 上传，不设线）。
- spotbugs（单独 CI job，观测不卡门）+ CodeQL workflow（java）。
- ci.yml：concurrency cancel-in-progress、timeout-minutes=30、surefire 报告上传；全部 action 版本统一。
- maven-wrapper（mvnw）；checkstyle/pmd/spotless/多 JDK/failsafe 不做（注记）。

### K. 文档与终验（T80）

- CLAUDE.md/README/spec 文档索引口径统一（模块数、新键入配置表、索引补 spec 11–15、RELEASING 兼容矩阵落 README）。
- examples 新增 starter-boot-demo（starter + application.yml + 内置 Stub 模型可启动工程）。
- 全仓 `mvn verify` 终验绿；spec 14/15 与实现一致性复核；MAP 收口。

## Testing Decisions

- **好测试只测外部行为**：不 assert 内部字段/调用次数；超时类用例用真实时钟边界或注入 Clock，不 sleep 挖时间。
- **主接缝沿用，不新增**：
  - 端到端 = examples 集成测试（FakeChatModel/ScriptedChatModel 驱动）——runaway/SpawnGate/resilience 的用户故事全部在此验证；
  - 自动装配 = 各模块 ApplicationContextRunner（+ 本次补 @SpringBootTest 全上下文冒烟落 starter）；
  - store = 契约测试基类范式（SkillStore 新基类，JDBC/Redis 实现复用）；
  - dashboard = 真实 HTTP 客户端随机端口（沿用现状）；
  - mcp 真实协议 = SDK 进程内 server（新增，属既有模块测试域）；
  - redteam = RedteamTargetSmokeTest 驻留模式（沿用现状）。
- **优先艺术（prior art）**：ResilienceEndToEndTest（分支移植）、GracefulShutdownEndToEndTest/CancelModeEndToEndTest（取消穿透参照）、AbstractBuzhouStoresContractTest（store 契约）、DashboardHttpServerTest（HTTP 面）、BackpressureEndToEndTest（事件背压参照）。
- 取消杀进程树用例必须跨进程验证（spawn 长 sleep → 取消 → join 断言死亡），不能只 mock。
- 元数据断言在构建层（解 jar 查 JSON），不在单测里重造 classpath。

## Out of Scope

- 模型层熔断/重试预算、MCP 工具快照自动重发现、guard CommandSandbox 与 run_command 完整合流（注记开放问题）。
- dashboard 前端工程化、可插拔鉴权 SPI、观测第二存储。
- 多实例分布式接管、Firecracker/E2E 沙箱、FIDES 二期、sub-agent（沿既有边界）。
- 分支 crash-recovery/graceful-shutdown 的移植（main 已覆盖）。
- 覆盖率阈值卡线、checkstyle/pmd/spotless、多 JDK 矩阵、failsafe（注记）。

## Further Notes

- 10K★ 政策执行记录见研究文档；micrometer/MCP SDK/promptfoo 本体 star 不足，经 Spring Boot（75K+）/LangChain（100K+）/官方文档间接采纳。
- impl 切片序：resilience 移植 → runaway/SpawnGate → observability → otel → dashboard → tools → mcp → skills → 装配基建 → redteam → CI → 文档终验（依赖顺序见 `.wayfinder/impl/`）。
- 全部决议可推翻（用户常设授权按推荐迭代）。
