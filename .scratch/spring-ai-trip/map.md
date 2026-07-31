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
- [参照系与留白推演素材调研](issues/02-research-reference-implementations.md) — Claude Code 三层压缩与九段 compact prompt 坐实蓝本借鉴；AgentScope Java 的 eviction/compaction 分层与蓝文同构，可作推演主参照；LangChain4j 止于滑窗，正是定位空间。详见 `research/reference-implementations.md`。
- [HITL 与 Hook 机制行业调研](issues/22-research-hitl-hooks-landscape.md) — Spring AI 2.0 无原生 HITL/暂停恢复，需自建（DECO 式「事件+state+续跑重放」落地成本最低）；ADK/LangGraph/Claude Code 范式已归档。详见 `research/hitl-hooks-landscape.md`。

## Not yet specified

<!-- 在范围内但还不能精确成票的雾；前沿推进后毕业 -->

- **性能基准与压测方案** — 压缩延迟、并发吞吐、token 节省率的验收标准；需等机制设计成形后才能精确提问。
- **九段式摘要的 prompt 调优与评测方法** — 模板定型后才谈得上调优与评测指标。
- **可视化后台的具体 UI 交互与线框** — 依赖 Span/Event 数据模型定型；前台只做到形态设计，细节待毕业。
- **Maven Central 实际发布流程** — 依赖工程化 ticket 定下 groupId 与账号后才能精确。
- **示例模块的场景设计** — 用哪个业务 demo 串起所有机制（文章用排障会话）；需等模块划分后细化。
- **HITL 确认交互的前端协议细节** — 确认框事件结构、多选项/带输入控件的渲染契约；依赖 25 定下暂停/恢复语义后才精确。
- **危险工具清单的行业默认模板** — 开源版默认把哪些操作标为危险（发布/删除/写库……）；依赖 25 的配置模型定型后细化。

## Out of scope

<!-- 已判定在目的地之外的工作；关闭、永不毕业 -->

- 长期记忆、Agentic Loop（Plan Mode / Sub Agent）、提示词治理、系统化自愈 —— 文章 Roadmap 项，本期不做。
- Graph 编排、多 Agent 协作 —— 属于 Spring AI Alibaba / AgentScope 的编排层，非单 Agent 运行时。
- 可视化后台的生产级前端实现 —— Spec 只到 API 与形态设计。
- DECO 文章中的业务专属 Hook（血缘 offload、文件树事件、发布条目收集、环境变量捕获等）—— 仅作机制范例，业务逻辑不进框架。
