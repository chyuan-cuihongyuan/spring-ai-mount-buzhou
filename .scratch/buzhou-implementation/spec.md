# Spec: Buzhou Harness 全机制实现

Status: ready-for-agent
Labels: ready-for-agent

设计依据：`docs/spec/` 全集（00-overview + 九份机制详设）与 `.scratch/spring-ai-trip/` 的 30 张决策票。本文是实现期 PRD；机制细节与推演标注以 `docs/spec/` 为准，本文不重述，只收拢实现决策。

## Problem Statement

Java 业务团队在 Spring AI 上构建单 Agent 应用时，生产化所需的运行时能力全部要自研：多轮对话上下文撑爆窗口（无渐进压缩）、大工具返回挤占上下文（无溢出保护）、模型推理过程不可解释（无认知可观测）、能力供给僵化（Skill/MCP 不能热更）、危险操作无人工门禁、中断续接历史残缺。每个团队重复造这层「马具」，且大多止于滑窗截断这种断崖式方案。

## Solution

Buzhou Harness 作为 Spring AI 之上的运行时中间层（叠加而非替代），以独立 Maven 模块按需引入的方式提供九大机制：渐进式记忆压缩、Spill 溢出保护、Span+Event 认知可观测（含开发者控制台）、Skill 体系 + MCP 热插拔、并行工具调用、内置原子工具、Hook 护栏体系（读写护栏/HITL/闭环）、会话与配置主干、持久化五 SPI。每个机制模块独立可用、可跨项目作为架构支持能力单独引入；聚合 starter 一键全量。

## User Stories

### 会话主干

1. As a 业务开发者, I want `AgentRuntime.spawn(appId, agentName)` 一行拿到 AgentSession, so that 装配好的 ChatClient + 记忆 + Hook 链开箱即用
2. As a 高级用户, I want `Buzhou.enhance(ChatClient.Builder)` 渐进采用, so that 自有装配也能获得 Harness 能力
3. As a 业务开发者, I want chat/stream 体感对齐 ChatClient, so that 迁移零学习成本
4. As a 业务开发者, I want 传入已有 sessionId 即续接会话（历史+摘要+state 自动加载）, so that 进程重启/跨实例部署不丢上下文
5. As a 平台运维, I want 同一会话同时只允许一个活跃实例（可选 steal 夺权）, so that 多实例部署不会并发写坏会话
6. As a 业务开发者, I want close/cancel/idle 超时触发会话资源成套清理, so that 不留 spill 文件、连接等泄漏
7. As a 前端开发者, I want 会话级事件监听器透出 HITL 确认请求与护栏通知, so that 可用自有 SSE/WS 桥接给用户

### 记忆压缩

8. As a 业务开发者, I want 上下文按「先扣后算」动态预算管理（窗口-输出预留-安全缓冲-系统提示-工具 Schema-当前输入）, so that 历史预算永远真实可用
9. As a 业务开发者, I want 旧工具返回被微压缩为带 evidence-id 的占位符, so that 长会话不被工具结果撑爆
10. As a 排障者, I want 凭 evidence-id 回查原始工具返回, so that 压缩后仍能追溯现场
11. As a 业务开发者, I want 九段式结构化摘要按 P0–P3 优先级降级, so that 关键信息（意图/现场/下一步）永不丢失
12. As a 业务开发者, I want 摘要增量合并且失败熔断, so that 摘要质量不随轮次退化、摘要故障不拖死会话
13. As a 业务开发者, I want token 估算默认字符启发式、可插 TokenEstimator SPI（jtokkit 可选）, so that 零依赖起步、精确度可升级
14. As a 框架使用者, I want 加载历史时悬空调用自动修复（完全悬空剔除/部分悬空合成中断结果）, so that 中断续接后模型 API 不报错
15. As a 框架使用者, I want 幂等工具中断后自动重放一次、失败再进悬空修复, so that 能恢复的执行不丢、不能恢复的安全降级
16. As a 排障者, I want 修复与重放动作都进可观测层并因果串联, so that 能看清「重放了几次、为什么放弃」

### Spill 溢出保护

17. As a 业务开发者, I want 超阈值（默认 32000 字符）工具返回自动落盘、上下文只留预览（默认 2048）+ 路径 + 回读指引, so that 大结果不挤爆上下文
18. As a 模型运行时, I want `read_range(path, mode, …)` 支持字节区间/JSON path/分页三种回读, so that 模型按需取回精确片段
19. As a 业务开发者, I want 回读结果超阈值递归走 spill, so that 回读本身不会二次膨胀
20. As a 平台运维, I want SpillStore 磁盘默认 + JDBC 跨实例实现, so that 单机零依赖、分布式开箱可用
21. As a 业务开发者, I want spill 产物随会话清理、被 evidence 引用的保留、TTL 兜底, so that 既不泄漏也不误杀证据

### 可观测

22. As a 排障者, I want 会话 ⊃ 轮次 ⊃ 模型/工具调用的 Span 树与 Thinking/FinalReply/ToolInput/ToolOutput/Error 事件, so that 看清每轮推理依据
23. As a 排障者, I want 各厂商思维链统一采集（reasoningContent/thinking/thinking_content/Anthropic 块/Google thoughts）, so that 换模型不丢推理可见性
24. As a 排障者, I want 后台能按轮次还原「模型当时实际看到的注入视图」, so that 压缩/spill 效果可解释
25. As a 运维, I want token/耗时同时进 Span 属性与 Micrometer 指标, so that 现有 Prometheus 栈直接可用
26. As a 运维, I want Span 可导出 OTel, so that 接入既有观测平台
27. As a 开发者, I want 引入 buzhou-observe-dashboard 即得内嵌可视化后台（会话回放/Span 树/快照/Skill 管理）, so that 零部署调试
28. As a 框架使用者, I want 采集异步批量落库不采样, so that 排障证据完整且主链路不受影响
29. As a 排障者, I want 压缩/spill/Hook 等框架内部动作也产 Span, so that 框架自身耗时与行为可见

### Skill 与 MCP

30. As a 业务开发者, I want classpath 放 `META-INF/skills/*/SKILL.md` 即注册 Skill, so that 能力随包分发
31. As a 平台运营, I want DB 动态 Skill 管理 API + 后台管理页、同名覆盖内置, so that 不发版即可上架/更新能力
32. As a 业务开发者, I want 上下文只放 Skill 清单、模型用 load_skill 按需取正文, so that 能力规模不占预算
33. As a 平台运维, I want MCP server 清单配置驱动、运行时差量热更新, so that 工具集变更不重启
34. As a 平台运维, I want 在途调用引用计数 + grace 延迟关闭 + 强杀兜底, so that 热更新不打断进行中的调用也不泄漏连接

### 工具

35. As a 业务开发者, I want 同轮多个独立 tool_call 虚拟线程并行执行、按原序回注, so that 多工具场景延迟最低
36. As a 业务开发者, I want 单工具超时不拖整轮、失败转文本回注, so that 一个 hang 工具不死会话
37. As a 业务开发者, I want 工具可声明串行组, so that 同一资源写操作不并发冲突
38. As a 业务开发者, I want 内置 read/write_file、run_command、http_request、todo、copy_file、str_replace 等原子工具, so that 常见操作开箱可用
39. As a 安全负责人, I want 危险工具默认 opt-in、沙箱/黑名单/SSRF 防护默认全开, so that 默认配置即安全
40. As a 业务开发者, I want todo 清单入会话 state 跨实例续接, so that 长任务断点不丢进度

### Hook 护栏

41. As a 业务开发者, I want 六切面 Hook（before/afterTool、before/afterModel、before/afterTurn、onEvent）以 Spring Bean 注册、order 编排, so that 自定义护栏与框架机制同构
42. As a 业务开发者, I want Hook 返回 CONTINUE/BLOCK/REPLACE 密封三态, so that 短路语义编译期可检查
43. As a 业务开发者, I want 工具参数声明 @LongContentParam 后自动支持「路径参数传文件全文」（写侧 Onload）, so that LLM 长产物不被截断略写
44. As a 业务开发者, I want 编辑类工具默认强制「先 copy_file 副本再 str_replace」, so that 原始材料不被改坏
45. As a 安全负责人, I want 危险工具未获真实用户授权时框架层物理走不通（阻断+确认事件+state 放行）, so that 不可逆操作有人工门禁
46. As a 前端开发者, I want 确认事件 schema 支持 yes/no/多选/单输入/hint 嵌 diff 文本, so that 确认交互开箱可渲染
47. As a 业务开发者, I want 授权默认一次性、可配会话内长效, so that 安全与批量场景体验兼顾
48. As a 平台运维, I want 授权 state 持久化, so that 续跑请求打到任意实例都正确放行
49. As a 业务开发者, I want FactCollector 三要素脚手架（判定/渲染/ttl）实现 Hook→state→Attachment 补失忆, so that 关键事实不靠 LLM 自觉记住
50. As a 框架使用者, I want 失败语义非对称（读侧降级透传、写侧阻断）, so that 可用性与产物完整性各得其所

### 配置与持久化

51. As a 平台运维, I want 四层配置覆盖（默认<yml<绑定级<工具级）, so that 平台默认与业务特调共存
52. As a 平台运维, I want PolicyConfigProvider SPI 内置 properties/DB、Nacos/Apollo 可扩展, so that 配置中心不被绑死
53. As a 业务开发者, I want 内存实现默认、JDBC 生产主推、Redis 轻量可选, so that demo 零依赖、生产有正解
54. As a 业务开发者, I want 「一轮消息+state+摘要」unit-of-work 原子提交, so that 中断不留半写状态
55. As a 业务开发者, I want ChatMemory 适配器挂进官方 Advisor 链, so that 与 Spring AI 生态组件兼容

### 工程化

56. As a 使用方架构师, I want 每个机制独立模块按需引入（如只要 buzhou-memory）, so that 依赖面最小
57. As a 使用方架构师, I want 聚合 starter + BOM, so that 全量引入一个版本号搞定
58. As a 社区贡献者, I want CI（JDK21 mvn verify）+ issue/PR 模板齐备, so that 贡献路径清晰
59. As a 维护者, I want Central Portal 发布通道与签名就绪, so that 可发布 Maven Central
60. As a 学习者, I want examples 排障会话 demo（单场景多脚本串全部机制）, so that 有端到端参照

## Implementation Decisions

- **模块构成（16）**：根父 POM、buzhou-bom、buzhou-core、buzhou-memory、buzhou-spill、buzhou-observability、buzhou-observe-otel、buzhou-observe-dashboard、buzhou-skills、buzhou-mcp、buzhou-guard、buzhou-tools、buzhou-store-jdbc、buzhou-store-redis、buzhou-spring-boot-starter、examples。星形依赖无环（core 事件总线解环）；`io.github.chyuan-cuihongyuan:buzhou-*`；包结构 `…buzhou.<mech>` 分 api/internal。（docs/spec/09）
- **挂接点**：工具调用循环介入点 = 自定义 `HarnessToolCallingManager implements ToolCallingManager`（经 ToolCallingAdvisor.Builder / Boot Bean 注入）；可观测 = 循环内 advisor(+400) + ToolCallback 包装；记忆 = 自定义 ChatMemory 适配器（get 返回压缩视图）+ memory advisor。均为 Spring AI 2.0 公开扩展点，不替换框架内核。（01 调研）
- **注入视图单一管线**：加载历史 → 悬空修复 → 微压缩 → 动态预算 → 摘要降级 → Attachment 渲染 → 注入快照落库，全部在视图构建时统一触发；持久层消息只追加，一切加工在视图层。（01/08 spec）
- **Hook 链即机制载体**：Spill offload、写侧 Onload、副本分离拦截、HITL 守卫、FactCollector、可观测采集均为内置 Hook（order 0–999 预留，业务 1000 起，yml 可禁用）；切面映射 ToolCallback 包装层与 advisor 两处。（07 spec）
- **持久化五 SPI**：MessageStore / SummaryStore / SessionStateStore / SessionLeaseStore / ObservabilityStore；内存（core 默认）/ JDBC（生产主推，纯 spring-jdbc，MySQL+PostgreSQL）/ Redis（Lua/MULTI）；unit-of-work 事务边界；观测写入排除在事务外（异步管道）。DDL 与 Redis 布局见 08 spec。
- **并发模型**：JDK 21 虚拟线程；会话级执行器（入资源注册表）+ 每轮并发上限 8（信号量）；超时 `Future.get(timeout)`+中断，不采用 StructuredTaskScope（预览特性）；Span 上下文显式传递，不用 ThreadLocal。
- **配置体系**：四层覆盖（默认<yml<绑定级<工具级），绑定级入持久层；PolicyConfigProvider SPI（内置 properties/DB，Nacos/Apollo 留社区扩展）；工具策略通配消歧「精确 > 最长前缀 > *」；安全项全开、依赖项优雅降级。
- **跨实例语义**：会话租约（fencing token 防脑裂）既是一切「悬空 vs 执行中」的判据，也是 HITL 授权/spill 回读/todo 续接的前提；全部跨实例状态入五 SPI，实例无本地状态。
- **测试接缝（按用户决策）**：每个大机制模块以其公共 API 为独立接缝，可跨项目作为架构支持能力单独测试与复用——Hook 链（core）、压缩引擎（memory）、SpillStore+read_range（spill）、SpanRecorder+ObservabilityStore（observability）、HITL 守卫（guard）、MCP 注册表（mcp）、原子工具逐个（tools）；集成接缝 = AgentSession 端到端（内存 SPI + mock ChatModel）。机制接缝优先于集成接缝。
- **蓝本忠实度**：85 处推演已在 `docs/spec/` 各档以 `> 【推演】` 标注并在 00-overview 汇总；实现遇冲突回写对应文档而非绕开。
- **开源工程**：Central Portal + GPG 签名；CI = GitHub Actions（JDK 21，`mvn verify`）；README/CONTRIBUTING/COC/issue 模板已就位；发布人工 checklist 见 ticket 20。

## Testing Decisions

- **好测试的标准**：只测外部行为（公共 API 的可观察输出——回复、占位符、事件、Span 树、落库内容），不断言内部实现（私有方法、内部状态、调用次序细节）；每个机制接缝的测试只依赖其公共 API + 内存 SPI，不依赖其他机制模块，验证「可跨项目独立作为架构支持」。
- **按模块**：
  - buzhou-core：Hook 链编排（order/三态短路/yml 禁用）、会话生命周期与租约互斥（含 steal）、四层配置合并与通配消歧、unit-of-work 原子性——内存实现直测。
  - buzhou-memory：动态预算计算、微压缩完结判定与占位符、九段摘要合并/熔断/降级、悬空修复两态、重放白名单——mock ChatModel 驱动多轮脚本。
  - buzhou-spill：阈值落盘、占位符文案、read_range 三模式与递归 spill、生命周期清理（引用保留/TTL）——临时目录 + 内存/JDBC H2 实现。
  - buzhou-observability：Span 树归属（含并发不串味）、思维链厂商适配表、异步批量落库 flush、注入快照还原——mock 模型 + 内存 store。
  - buzhou-guard：HITL 阻断→事件→授权→放行全链、一次性/长效授权、闭环 FactCollector 注入与 ttl——内存 state。
  - buzhou-mcp：差量刷新、引用计数延迟关闭与强杀兜底——fake ToolSetProvider + stub client。
  - buzhou-tools：每个原子工具的安全边界（沙箱逃逸、黑名单、SSRF）与默认开关矩阵。
  - buzhou-store-jdbc / buzhou-store-redis：同一 SPI 契约测试套件跑在 Testcontainers（MySQL/PostgreSQL/Redis）上，保证三实现语义一致。
  - 集成：AgentSession 端到端——排障 demo 脚本（长会话压缩链、HITL 往返、跨实例续接）+ 28 号票的评测四指标（P0 保留率/续接成功率/事实召回/压缩率）作为回归门禁。
- **Prior art**：仓库尚无测试代码（绿地）；测试风格对齐 Spring AI 上游（JUnit 5 + AssertJ + MockWebServer/Testcontainers），评测脚本入 examples 独立目录（ticket 30）。

## Out of Scope

- 长期记忆、Agentic Loop（Plan Mode / Sub Agent）、提示词治理、系统化自愈（蓝本 Roadmap 项，本期不做）
- Graph 编排、多 Agent 协作（属编排层，非单 Agent 运行时）
- 可视化后台的生产级前端（告警、多实例聚合监控——生产监控走 OTel 桥）
- DECO 文章业务专属 Hook（血缘 offload、文件树事件、发布条目收集等）——仅作机制范例
- Nacos/Apollo 配置中心适配、jtokkit 精确分词、S3 SpillStore——社区扩展位，非首发
- 性能压测方案（吞吐/延迟验收标准）——地图留雾，实现期机制成形后专项

## Further Notes

- 领域术语以 `CONTEXT.md` 为准；机制细节、接口签名、DDL、时序、推演标注以 `docs/spec/` 十份文档为准——实现冲突回写文档。
- 全部 30 张决策票在 `.scratch/spring-ai-trip/issues/`，每张含定案与影响面；地图 `map.md` 的 Decisions-so-far 是速查索引。
- **蓝本对账已通过**：两篇蓝本（携程 Spring-Ai-Trip 主体机制文、腾讯 DECO Hook 护栏文）已重新逐条核对，功能与思想 100% 有落点、零遗漏；6 处有意演进（toolCallId 命名、evidence 引用保留、MCP 强杀兜底、悬空先重试后修复、确认模型简化、授权指纹化）均已在 `docs/spec/` 以 `> 【推演】` 标注，实现时按 spec 为准而非按蓝本原文。
- examples 主场景 = 运维排障 Agent（忠于蓝本），单场景多脚本；评测脚本独立目录复用同一 mock 设施。
- 危险工具默认清单 = write_file / run_command / http_request 写方法 + copy_file / str_replace；MCP 工具默认不标危险，业务通配自配。
