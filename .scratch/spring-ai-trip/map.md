# Map: Spring-Ai-Trip 开源库设计 Spec

Labels: wayfinder:map

## Destination

一份完整的设计 Spec：中文 Markdown 文档集，落在本仓库 `docs/spec/`（总览 + 每机制一份详设，含 API、配置、存储 Schema、时序）。覆盖 Spring-Ai-Trip 开源库全部机制——渐进式记忆压缩、Spill 溢出保护、Span+Event 认知可观测（含可视化后台）、Skill 体系 + MCP 热插拔、并行工具调用、内置原子工具、Hook 护栏体系（长产物读写护栏、HITL 危险操作人工审核、Hook→state→Attachment 联动闭环）；每个机制独立 Maven 模块，可跨项目单独引入。走到终点 = 实现所需的决策全部做出，无遗留问题，可按 Spec 直接开工。

## Notes

- 领域：Java Agent 运行时 Harness，叠加在 Spring AI 之上（叠加而非替代）。术语以根目录 `CONTEXT.md` 为准。
- 原型文章（设计蓝本）：https://mp.weixin.qq.com/s/o0n-fkQe6Q8dlu7HJAjH3Q （携程技术公众号，Spring-Ai-Trip 主体机制）。
- 蓝本二（Hook 护栏体系）：https://mp.weixin.qq.com/s/ISwjIw5lj7JlcQJV7BOx5g （腾讯 DECO：Hook 链、读写两侧 offload/onload、HITL 门禁、Hook→state→Attachment 闭环；全文已存档 `research/hooks-article.md`）。
- 忠实度原则：蓝本明确描述的机制严格遵循；留白处自主推演，并参考携程/腾讯/字节/阿里等大厂文章与真实开源项目（Spring AI、AgentScope Java、LangChain4j、Claude Code、ADK、LangGraph），推演处在 Spec 中标注。
- 技术基线：JDK 21+（虚拟线程）、Spring Boot 4.x、Spring AI 2.0.0（2026-06 GA，当前最新稳定版；2.0 起工具调用循环移入 Advisor 链，利好全部挂接点设计）。
- 项目身份：GitHub 仓库名 `spring-ai-mount-buzhou`，Apache-2.0。groupId 待定（预期 io.github.\<用户名\>）。
- 本图为纯规划：ticket 只产决策，不写产品代码。
- 每个 session 应 consult 的 skills：`/grilling`、`/domain-modeling`、`/research`。
- 开源工程化目标：可发布 Maven Central；含测试与示例模块。

## Decisions so far

<!-- the index — one line per closed ticket: enough to judge relevance, then zoom the link for the detail the ticket holds -->

- [Spring AI 最新版挂接点调研](issues/01-research-spring-ai-surface.md) — Spring AI 2.0.0（Boot 4.x）：工具调用循环已在 Advisor 链内、ToolCallingManager 可整体替换；reasoning 无统一抽象、MCP 运行时增删无公开 API，需 Harness 自建。详见 `research/spring-ai-surface.md`。
- [Maven 多模块划分与依赖方向](issues/03-module-structure.md) — 12 模块细切（后增补存储扩展 ×2 → 14）；core 收厚（会话/持久化SPI/策略/估算/Hook链/事件总线/并行脊柱）；core 事件总线解环，星形依赖无环；buzhou-* 短前缀；模块自装配 + 聚合 starter；写侧 Onload 归 spill。
- [会话入口 API 形态](issues/04-session-api.md) — 双层 API（spawn 门面 + Builder 增强）；AgentSession 显式生命周期 + 会话作用域资源注册表成套清理；chat/stream 对齐 ChatClient + 事件监听器透出 HITL；sessionId 传入即续接、会话租约互斥（可 steal）。
- [配置体系与策略模型](issues/05-configuration-model.md) — 四层覆盖（默认<yml<绑定级<工具级），绑定级入持久层；PolicyConfigProvider 动态配置 SPI（内置 properties/DB，Nacos/Apollo 留可选扩展）；工具策略=工具声明默认+配置通配覆盖；安全项全开、依赖项优雅降级。
- [持久化 SPI 与默认实现选型](issues/06-persistence-spi.md) — 四 SPI（Message/Summary/SessionState/SessionLease）；首发三实现：内存（core 默认）+ buzhou-store-jdbc + buzhou-store-redis（模块 12→14）；自研全保真消息模型 + ChatMemory 适配器；完整事务（unit-of-work，JDBC 本地事务/Redis Lua）。
- [动态预算算法与 token 估算器](issues/07-token-budget.md) — 蓝本公式定稿（先扣后算、摘要计入、0.90 阈值可配）；窗口=内置模型表+配置覆盖+未知默认32K；估算=字符启发式默认+TokenEstimator SPI（jtokkit 可选扩展）；Schema 按工具集哈希缓存、其余每轮现算，注入视图构建时统一触发。
- [微压缩策略模型](issues/08-micro-compaction.md) — 完结=结论落地（tool_calls 全回应+assistant 文本收尾）；注入前总先微压缩再算预算；策略=neverCompress/maxAgeTurns(3)/minSizeChars(200)+protectRecentTurns(1)；evidence-id=消息 id，统一证据回查工具（范围读取实现升 core 共享）。
- [九段式摘要模板与增量合并](issues/09-summary-template.md) — CC 九段映射定稿（P0=意图/现场/下一步，P3=用户消息清单）；增量合并+analysis 草稿；主模型默认可配独立、失败熔断；system-reminder 包裹插在近期原文前；段落超限降级为 gist+指针不整段删。
- [参照系与留白推演素材调研](issues/02-research-reference-implementations.md) — Claude Code 三层压缩与九段 compact prompt 坐实蓝本借鉴；AgentScope Java 的 eviction/compaction 分层与蓝文同构，可作推演主参照；LangChain4j 止于滑窗，正是定位空间。详见 `research/reference-implementations.md`。
- [HITL 与 Hook 机制行业调研](issues/22-research-hitl-hooks-landscape.md) — Spring AI 2.0 无原生 HITL/暂停恢复，需自建（DECO 式「事件+state+续跑重放」落地成本最低）；ADK/LangGraph/Claude Code 范式已归档。详见 `research/hitl-hooks-landscape.md`。
- [悬空调用修复规则](issues/10-dangling-call-repair.md) — 租约即判据；先重试后修复（重试策略另立 29）；视图层现做不破坏只追加：完全悬空剔除（正文降级保留）、部分悬空补合成中断结果；修复记 Event 审计。
- [Spill 存储抽象与生命周期](issues/11-spill-storage.md) — SpillStore SPI 首发磁盘+JDBC（跨实例靠 JDBC，S3 留扩展）；命名改 `spill://agentName/sessionId/toolCallId`；注册表成套清理+evidence 引用保留+TTL 7 天兜底；阈值 32000/预览 2048 字符并入 05 四层策略。
- [Spill 回读工具设计](issues/12-spill-readback-tool.md) — 单工具 `read_range(path, mode, offset|jsonPath|cursor, limit)` 复用 core 范围读取；回读结果递归走 spill 防二次膨胀；占位符自含回读指引+系统提示词兜底；JSON List 预览=前 20 项+totalCount+truncated；默认注册可关。
- [示例模块的场景设计](issues/30-example-scenarios.md) — 排障会话忠于蓝本，单场景多脚本（压缩链/可观测回放/护栏 HITL/Skill+MCP 四簇），mock DB+HTTP；28 评测脚本独立目录复用 mock。
- [工具中断重试与重放策略](issues/29-tool-retry-replay.md) — 幂等声明白名单才自动重放，其余直接判悬空；续接重放仅 1 次，运行期瞬断重试独立可配（指数退避上限 3）；危险工具重放复用 25 重新授权；重试记 span 与修复 Event 因果串联。
- [摘要质量评测方案](issues/28-summary-evaluation.md) — 四指标（P0 保留率/续接成功率/事实召回/压缩率，judge+人工抽检）；脚本数据集入 examples、Spec 只写方法论；两级联动端到端用例入 01-memory-compaction 评测专节。
- [内置通用 Hook 清单](issues/27-builtin-hooks.md) — 六核心 Hook（offload/onload/副本拦截/HITL/FactCollector/可观测）默认开可禁用；取消响应可选；Rerank 截断/响应格式化降示例；持久化不 Hook 化属内核；危险清单=三件套默认、MCP 自配、启发式示例进文档。
- [Hook→state→Attachment 闭环](issues/26-state-attachment-loop.md) — state 通用 KV 事实模型（key/value/producer/createdTurn/ttl）命名空间约定；注入=system-reminder 块插近期原文前、ttl 表达一次性/累积；业务三要素脚手架（判定/渲染/ttl）；事实进摘要 Current State 段、token 算系统侧固定扣除。
- [HITL 危险操作守卫设计](issues/25-hitl-guard.md) — DECO 式阻断+state+续跑重放（22 最低成本路径）；确认事件 schema 经会话监听器透出、不绑 Web 框架；通用确认模型（yes/no+多选+单输入+hint 嵌 diff 文本）不建富控件；授权=工具名+参数指纹、一次性默认可配长效、state 持久化跨实例放行。
- [长产物读写护栏](issues/24-long-content-guard.md) — 读侧 offload=Spill Hook 化不分层；写侧 @LongContentParam+xxxPath 互补参数协议泛化，beforeTool Hook 加载覆盖；副本分离默认拦截（直改只读源被拦，19 增补 copy_file/str_replace）；失败非对称=读 CONTINUE 降级/写 BLOCK 阻断。
- [Hook 链框架设计](issues/23-hook-chain-framework.md) — 六切面（before/afterTool、before/afterModel、before/afterTurn、onEvent）映射 ToolCallback 包装+advisor；Bean 自动收集+order 编排（内置 0-999 预留，业务 1000 起，yml 可禁用）；密封三态 CONTINUE/BLOCK/REPLACE 短路；Spill/Onload/压缩/可观测/HITL 全实现为内置 Hook 吃狗粮。
- [Spec 文档集结构与写作模板](issues/21-spec-doc-structure.md) — 00-overview+九份机制编号档；固定八节模板（目标/术语/API/配置/Schema/时序/推演/开放问题）；推演 >【推演】就地标注+总览推演清单汇总；中文+Mermaid。
- [开源工程化](issues/20-open-source-engineering.md) — groupId=io.github.chyuan-cuihongyuan；Central Portal 通道；README/CONTRIBUTING/COC/CI/issue 模板已产出；人工清单=Portal 命名空间注册+GPG key+仓库 Secrets+LICENSE。
- [内置原子工具清单与安全边界](issues/19-builtin-atomic-tools.md) — 首发 read/write_file+run_command+todo+http_request+机制衍生 read_range/load_skill/evidence 回查；无害默认开、危险 opt-in 且挂 HITL 守卫；沙箱+命令黑名单+SSRF 防护默认开；todo 入会话 state 持久化；瘦 Schema+Skill 承载深度说明。
- [并行工具调用设计](issues/18-parallel-tool-calls.md) — HarnessToolCallingManager 虚拟线程 fan-out+按序回注（Spill 同点接管）；会话级执行器+每轮上限 8；单工具超时 60s 失败转文本+取消传播；默认可并行+声明式串行例外。
- [MCP 热插拔设计](issues/17-mcp-hot-swap.md) — ToolSetProvider SPI 复用 05 配置体系、不绑配置中心；starter 之上加注册表层；差量刷新只动变化项；引用计数+grace 30s 延迟关闭+5min 强杀兜底；热更事件进可观测层。
- [Skill 体系设计](issues/16-skill-system.md) — META-INF/skills/*/SKILL.md+frontmatter 对齐 CC 生态；DB 覆盖内置、管理 API+dashboard 管理页（后台升为开发者控制台）；清单入系统提示词+load_skill 原子工具按需取正文；绑定关系并入 05 动态配置体系不新增 SPI。
- [可视化后台设计](issues/15-observability-dashboard.md) — buzhou-observe-dashboard 内嵌 Web 模块（模块 15→16）+前端单页打进 jar；注入视图快照落库可按轮还原「模型实际所见」；查询复用 ObservabilityStore（增快照表）；定位开发调试，生产监控走 OTel 桥。
- [可观测采集的挂接方式](issues/14-observability-attach.md) — Advisor(+400)开 Turn/ModelCall span + 包装 ToolCallback 开 ToolCall span；显式上下文传递抗虚拟线程串味；异步批量落库无采样、关闭强制 flush；压缩/spill/Hook 等内部动作全产 Span（13 增补 HarnessInternal 类）。
- [Span + Event 数据模型与思维链捕获](issues/13-span-event-model.md) — 自建认知模型+buzhou-observe-otel 导出桥（模块 14→15）；Span 四类/Event 核心五类开放枚举；平铺 parent_id+新增 ObservabilityStore SPI（06 四→五）；思维链厂商适配表+优雅降级；token/耗时 Span 属性+Micrometer 双写。

## Not yet specified

<!-- 在范围内但还不能精确成票的雾；前沿推进后毕业 -->

- **性能压测方案** — 并发吞吐与延迟的验收标准（压缩质量验收已由 28 定案）；纯性能项待机制实现期成形后提问。

## Out of scope

<!-- 已判定在目的地之外的工作；关闭、永不毕业 -->

- 长期记忆、Agentic Loop（Plan Mode / Sub Agent）、提示词治理、系统化自愈 —— 文章 Roadmap 项，本期不做。
- Graph 编排、多 Agent 协作 —— 属于 Spring AI Alibaba / AgentScope 的编排层，非单 Agent 运行时。
- 可视化后台的生产级前端实现 —— Spec 只到 API 与形态设计。
- DECO 文章中的业务专属 Hook（血缘 offload、文件树事件、发布条目收集、环境变量捕获等）—— 仅作机制范例，业务逻辑不进框架。
