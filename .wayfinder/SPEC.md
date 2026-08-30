# Spec — Buzhou core 做深做透

> 本 spec 是 **effort #1**（[map](maps/effort-01.md) + T1–T9 决策票）的**总纲合成**，由 `/to-spec` 从 wayfinder 图与仓库现状综合而成。逐项交付物仍由对应 ticket 承载；本文件规定**问题、方案、验收与边界**，并固化测试缝决策。effort #2 起总纲改落 `docs/spec/12+`。
>
> tracker 约定见 [`.wayfinder/README.md`](README.md)；术语以仓库根 [CONTEXT.md](../CONTEXT.md) 为准；机制详设见 [docs/spec/](../docs/spec/)。

---

## Problem Statement

Buzhou 的四个核心机制——`buzhou-core`（执行脊柱 / Hook 链 / 持久化 SPI）、`buzhou-memory`（渐进式压缩）、`buzhou-spill`（溢出保护）、`buzhou-guard`（Hook 护栏 / HITL / 事实闭环）——已经按 `docs/spec/` 落地、本地 `mvn clean verify` 十五模块全绿。但从**项目维护者与外部评估者**的视角，这个「绿」**还不可信、也不足以宣称 core 做深做透**，因为存在七处自相矛盾的可信度缺口：

1. **CI 红而本地绿**（[T1](tickets/T1-ci-red-remotely-green-locally.md)）：GitHub badge 显示 `failing`，本地同命令却全绿。「core 是鲁棒的」这个结论，必须建立在「干净 runner 上也能稳定复现绿」之上，否则任何后续深化都是空中楼阁。
2. **只有测试、没有可运行 demo**（[T4](tickets/T4-runnable-main-demo.md)）：`examples/` 全是 JUnit 断言（`*DemoTest` / `SummaryEvaluationTest` / `AtomicToolsIntegrationTest`），缺一个能直接 `run` 的 `src/main` 入口——评估者无法在 30 秒内眼见 core 端到端跑起来。
3. **缺真实 LLM 行为的集成测试**（[T5](tickets/T5-real-llm-integration-test.md)）：现有测试多为单元 / 脚本化，没有一条贴近真实模型行为（多轮 + 工具 + 压缩 / 崩溃续跑 / 并行 fan-out）的端到端链。绿测试不等于 core 真的能和模型一起工作。
4. **README 宣称「生产」却自标 alpha**（[T8](tickets/T8-downgrade-production-wording.md)）：项目状态段诚实写着「早期开发 alpha」，正文却反复「跑在生产里 / 生产所需的稳定性」——自相矛盾，直接削弱整份文档的可信度。
5. **run_command 无安全默认**（[T6](tickets/T6-run-command-safety-default.md)）：`buzhou-tools` 的命令执行原子工具没有 safe-by-default 策略，与「面向生产场景设计」的定位不符。
6. **`.scratch/` 内部草稿泄漏进开源仓**（[T7](tickets/T7-remove-scratch-from-git.md)）：52 份内部 issue/spec 草稿被 git 跟踪，开源仓不应外泄排障草稿。
7. **无「与 Spring AI 原生能力边界」文档**（[T9](tickets/T9-spring-ai-boundary-doc.md)）：评估者无法判断 Buzhou 到底在 Spring AI 2.0 之上**补了什么、替了什么、只是包了一层**——尤其 MCP 热插拔已是 Spring AI 2.0 原生，必须诚实标注。

**问题（用户视角一句话）**：我不能信任 core 是「真实鲁棒」的，任何评估这个项目的人也同样不能——因为 CI 不可复现、没有能跑给人看的 demo、没有贴近真实模型的测试、文档自相矛盾、工具缺安全默认、内部草稿外泄、且没人说清它相对 Spring AI 到底多了什么。

## Solution

把 `buzhou-core` / `buzhou-memory` / `buzhou-spill` / `buzhou-guard` **四个核心机制做到真实鲁棒**，并把用户列出的七项「信誉项」作为**深化过程的副产品**一并收口——不另起 effort。具体地：

- **CI 在干净 runner 上稳定绿**（T1 修复）：根因是环境性 maven 缓存（`spring-ai 2.0.0` / `spring-boot 4.1.0` 均已 GA、在 Maven Central，pom 无 `<repositories>` 是对的），修复方向是清缓存 / `-U` 强制更新，**绝不加 milestone / snapshot 仓**。
- **锚定「做深做透」的量化验收基线**（[T3](tickets/T3-depth-definition-of-done.md) DoD）：在「绿测试」之上，逐模块定义深度判据——属性测试 / 不变式、故障注入、预算压力下压缩正确性、read_range 三种回读边界、HITL→state→Attachment 事实闭环、SPI 契约稳定到可冻结 `api` 子包。基线定下后，per-module 深度 ticket 才从 MAP「Not yet specified」graduate。
- **提供一个真正可运行的 `src/main` demo**（T4）：最小覆盖多轮 + 工具 + 压缩，可选追加 Spill 回读 / HITL；无 key 也能跑（占位 / 可替换 stub）。demo 同时是 core 端到端可用性的证明——它失败即暴露 core 缺口。
- **加入至少一条真实 LLM 行为的集成测试**（T5）：凭据门控，CI 默认跳过、本地带 key 跑；可叠加 Mock 模拟真实行为进 CI。
- **run_command 安全默认**（T6）：默认关闭或默认开但沙箱，并与机制⑧ Hook 护栏的 HITL 联动。
- **`.scratch/` 移出 git 跟踪**（T7）：扫描敏感 → `git rm --cached` + `.gitignore`，保留历史（除非发现敏感信息才考虑重写）。
- **README 措辞降级**（T8）：统一为「面向生产场景设计的实验性框架」，中英同步，与 alpha 状态对齐。
- **撰写「与 Spring AI 原生能力边界」文档**（T9）：基于 T2 的逐机制 `NATIVE`/`ADDS`/`REPLACES` 表，诚实标注 MCP 热插拔 = NATIVE（Buzhou 仅 wrapper）。

**目的地是「core 先做深」，不是「打满九机制」**：其余五个机制模块（observe-otel / observe-dashboard / mcp / skills / tools 除 run_command）维持现状。

## User Stories

> actor：维护者（maintainer）、贡献者（contributor）、评估者（evaluator，正在判断是否采用 Buzhou 的人）、下游集成者（integrator，在 Buzhou 之上构建业务 Agent 的人）。

1. 作为**维护者**，我希望 CI 在干净的 GitHub runner 上稳定转绿，这样「core 鲁棒」的结论才建立在可复现的证据之上、而非仅我的本地机器。
2. 作为**维护者**，我希望 CI 失败的根因被如实记录（环境性缓存而非缺仓），这样未来遇到同类红屏时不必再走「加 milestone 仓」的弯路。
3. 作为**贡献者**，我希望 PR 只要 `mvn verify` 绿即可合入且 CI 与本地一致，这样我不会因为「本地绿、远程红」而反复返工。
4. 作为**评估者**，我希望 clone 下来就能用一条命令（`spring-boot:run` 或 `java -jar`）眼见 core 跑起来——多轮对话、工具调用、记忆压缩都在一个 demo 里发生，这样我在 30 秒内就能判断这个 Harness 值不值得深入。
5. 作为**评估者**，我希望无 API key 时 demo 也能跑（占位 / 可替换 stub），这样我不必先申请凭据就能体验核心机制。
6. 作为**集成者**，我希望 demo 覆盖「多轮 + 工具 + 压缩」这条最小链，这样我能照着它理解微压缩（完结轮次为原子单位）与动态预算（先扣后算）在生产里如何协同。
7. 作为**集成者**，我希望 demo 可选地展示 Spill 回读（字节区间 / JSON path / 分页）与 HITL 门禁，这样我看到大工具返回落盘、危险操作被拦截的真实行为。
8. 作为**维护者**，我希望有一条贴近真实模型行为的端到端集成测试（多轮 + 工具 + 压缩 / 崩溃续跑 / 并行 fan-out），这样我能捕捉单元测试发现不了的链路缺陷。
9. 作为**贡献者**，我希望真实 LLM 集成测试在 CI 默认跳过、本地带 key 可跑，这样我不会因为缺凭据或网络而让 CI 变红。
10. 作为**贡献者**，我希望真实 LLM 测试用 Mock 模拟真实行为进 CI、真实 API 仅本地，这样我在两套环境都能验证同一行为契约。
11. 作为**维护者**，我希望对 core/memory/spill/guard 各有一份「做深做透」的量化验收基线（属性测试 / 故障注入 / 预算压力 / 回读边界 / 事实闭环 / SPI 冻结），这样「深」不是一个形容词，而是一组可勾掉的判据。
12. 作为**维护者**，我希望 memory 的深度判据覆盖「预算压力下压缩信息不断崖丢失」，这样微压缩 + 九段式摘要 + 动态预算的正确性被属性测试守住。
13. 作为**维护者**，我希望 spill 的深度判据覆盖 read_range 三种回读（字节区间 / JSON path / 分页）的正确性边界与失败语义非对称（读侧 offload 失败降级透传、写侧 onload 失败阻断），这样大产物落盘与回读在边界条件下也不出错。
14. 作为**维护者**，我希望 guard 的深度判据覆盖 HITL → state → Attachment 事实闭环（Hook 确定性采集事实写 state、下一轮渲染为 Attachment 进 prompt），这样补失忆范式不靠 LLM 自觉。
15. 作为**维护者**，我希望 core 的深度判据覆盖并行工具调用的 fan-out / 按序回注 / 超时与取消传播，以及崩溃续跑（悬空调用修复）与去重 / 幂等，这样执行脊柱在故障下仍正确。
16. 作为**维护者**，我希望 `api` 子包 SPI 契约稳定到可冻结（破坏性签名变更需主版本），这样下游集成者能放心依赖。
17. 作为**评估者**，我希望有一份文档明确「Spring AI 没有的（Buzhou REPLACES）/ 增强的（ADDS）/ 只是包了一层（NATIVE）」，这样我能判断 Buzhou 的真实差异化、而不是被营销话术误导。
18. 作为**评估者**，我希望该文档诚实标注 MCP 热插拔 = Spring AI 2.0 原生（Buzhou 仅 wrapper），这样我信任这份文档其它部分的论断。
19. 作为**评估者**，我希望该文档把记忆压缩对照社区 `spring-ai-session`、把认知可观测对照原生 Micrometer/OTel，这样我知道 Buzhou 的增量到底在「认知维度」而非另起管线。
20. 作为**评估者**，我希望 README 措辞与 alpha 状态一致（「面向生产场景设计的实验性框架」），这样我不会误以为它已可上生产而盲目依赖。
21. 作为**集成者**，我希望 run_command 原子工具默认是安全的（默认关，或默认开但沙箱），这样我引入 Buzhou 时不会意外把一个无限制的命令执行口子暴露给模型。
22. 作为**集成者**，我希望危险命令模式能与机制⑧ Hook 护栏的 HITL 联动（不可逆操作在框架层物理走不通、需真实用户授权），这样安全模型是一致的、而非 tools 与 guard 各搞一套。
23. 作为**维护者**，我希望 `.scratch/` 的内部排障草稿不再被 git 跟踪并加入 `.gitignore`，这样开源仓只对外暴露该暴露的内容。
24. 作为**维护者**，我希望在 untrack `.scratch/` 前先扫描敏感信息、仅在发现时才考虑重写历史，这样我用最小代价完成仓库卫生。
25. 作为**贡献者**，我希望深度测试并入各模块既有测试套件而非另开测试工程，这样 `mvn -pl <module> -am test` 仍是单模块验证的统一入口。
26. 作为**评估者**，我希望 demo 的行为同时被 JUnit 断言（CI 兜回归），这样「人能跑给人看」与「机器能守住」是同一份 demo、不会随时间漂移。

## Implementation Decisions

### 模块与边界
- **目的地只含四个核心机制模块**：`buzhou-core` / `buzhou-memory` / `buzhou-spill` / `buzhou-guard`。`buzhou-observe-otel` / `buzhou-observe-dashboard` / `buzhou-mcp` / `buzhou-skills` / `buzhou-tools`（run_command 安全除外）维持现状，本次不深化。
- 严守 [09-modules-engineering](../docs/spec/09-modules-engineering.md) 的星形依赖硬约束：feature 模块互不依赖、跨机制协作走 core 事件总线 / core SPI；store 实现只依赖 core SPI。深化不新增模块间直接依赖。
- **`api` 子包 SPI 在本 effort 内冻结**（破坏性签名变更需主版本）；深度测试的一个重要产出是「证明 SPI 已足够稳定到可冻结」。

### tracker / triage 约定（本仓无 GitHub Issues / label）
- **本仓没有 GitHub Issues、没有 triage label 体系**——issue tracker 是**本地 markdown**：本 effort 图在 `.wayfinder/`（MAP + ticket + 本 SPEC），实现期历史 ticket 在 `.scratch/`（正被 T7 移出 git 跟踪）。详见 [`docs/agents/issue-tracker.md`](../docs/agents/issue-tracker.md) 与 [`.wayfinder/README.md`](README.md)。
- 故 to-spec / to-tickets / triage 等「发布到 issue tracker + 打 triage label」类 skill 的动作，在本仓的等价映射是：**新建 / 更新 `.wayfinder/` 或 `.scratch/` 下的 markdown 文件**；「`ready-for-agent` label」**无对应物**，等价语义 = **frontier**（`status:open` + `assignee:""` + 无未闭合 `blocked-by`）。
- 本环境无 `gh` CLI、未鉴权 → 凡需读写 GitHub Issues / Actions 日志的步骤均不可直接执行，须改走本地文件或请用户代取。

### CI 修复方向（T1，根因已被 T2 research 纠正）
- `spring-ai 2.0.0`（2026-06-12 GA）与 `spring-boot 4.1.0`（2026-06-10 GA）**均已 GA、在 Maven Central** → pom 无 `<repositories>` **是对的**。
- CI 失败是**环境性**：疑为 GitHub Actions `setup-java` 的 `cache: maven` 缓存了 GA 前的否定解析标记（`.lastUpdated`），干净 runner 无法重新解析。
- 修复手段（择一 / 组合，须以真实 CI 日志确认后再定）：清掉 setup-java 的 maven cache / 加 `-U` 强制更新 / 临时移除 `cache: maven` → push 验证 badge 转绿。**禁止加 milestone / snapshot 仓。**

### 逐机制 NATIVE / ADDS / REPLACES（事实骨架，来自 T2 research；喂 T3 基线与 T9 边界文档）
> 置信栏：高 / 中高 / 中。加粗为本次「做深」范围内。

| # | Buzhou 机制 | Spring AI 2.0 原生 | 关系 | 置信 |
|---|---|---|---|---|
| 1 | **渐进式记忆压缩** | 核心仅 `MessageWindowChatMemory`（按条数窗口）；无 token 预算 / 摘要 / 压缩。社区 `spring-ai-session` 补事件源 ChatMemory + 可插拔压缩 | **ADDS** | 中高 |
| 2 | **Spill 溢出 / read_range** | 无（无落盘、无大输出截断） | **REPLACES** | 高 |
| 3 | Span+Event 认知可观测 | 成熟 Micrometer/OTel 运维可观测；但 prompt/completion、工具入参/结果默认不采集、无 reasoning、无 span event | ADDS（应整合而非另起 OTel 管线） | 高 |
| 4 | Skill 体系 | 无 prompt-skill；最近的是 `ToolSearchToolCallingAdvisor`（渐进披露工具 schema，非能力 prompt） | REPLACES（非本次深化范围） | 中 |
| 5 | MCP 热插拔 | **原生**：动态工具增删、免重启、即时可用 | **NATIVE（Buzhou 仅 wrapper）——须诚实标注** | 高 |
| 6 | **并行工具调用** | `DefaultToolCallingManager` GA 顺序执行；无 executor / 超时 / 取消；官方 workaround = 自定义 Manager + 虚拟线程 | **REPLACES（最强存活差异化，护城河在实现深度）** | 高 |
| 7 | 原子工具 | `@Tool` 可暴露任意方法，但核心无策展工具集 | ADDS（非本次深化范围，run_command 安全除外） | 中高 |
| 8 | **Hook 护栏** | Advisor 支持 pre/post 拦截与阻断；内置 `SafeGuardAdvisor`；无打包 HITL / 长产物护栏 / state-attachment 闭环 | **ADDS（建在 Advisor 之上）** | 高 |
| 9 | 持久化 SPI | `ChatMemoryRepository`（多后端）+ `spring-ai-session` JDBC；无 Summary / SessionState / SessionLease / Observability 专属 store 抽象 | **ADDS** | 中高 |

**最大意外（须在深度基线与边界文档里正面回应）**：机制⑤ MCP 热插拔 = NATIVE，Buzhou 非差异化（但 MCP 本就 out-of-scope，仅须标注）；机制⑥ 并行工具 = REPLACES 且高置信，是 Buzhou 最强存活差异化，但护城河在**实现深度**而非概念。

### demo 形态（T4，blocked-by T1）
- 形态、模型接入方式（真实 key vs 占位 / stub）、最小可跑路径（`main` 类 / `spring-boot:run` / `java -jar`）须用户在 prototype ticket 拍板；README「方式三 纯编程式」那段代码是否真跑得通须一并验证。
- demo 同时被 JUnit 断言（CI 兜回归）——「人能跑」与「机器守得住」是同一份 demo。

### 真实 LLM 集成测试策略（T5，blocked-by T1）
- 优先「Mock 模拟真实行为进 CI + 真实 API 仅本地（凭据门控）」双轨；覆盖链择一最有价值：多轮 + 工具 + 压缩 / crash-recovery 续跑 / 并行 fan-out。
- 防脆性：倾向录制回放 / 契约测试 / 黄金样本，避免裸网络依赖。

### run_command 安全默认（T6）
- 默认关闭（`buzhou.tools.run-command.enabled=false`，显式开启才注册）vs 默认开但沙箱（白名单 / 容器 / 受限执行器）须用户在 grilling ticket 拍板；与机制⑧ Hook 护栏的 HITL 联动须一并定义（危险命令模式强制人工确认）。
- 跨平台一致性（Linux/macOS/Windows）是沙箱方案的真实成本。

### 仓库卫生与文档（T7 / T8 / T9，均 AFK 可做）
- `.scratch/`：先扫敏 → `git rm -r --cached` + `.gitignore`，保留历史（默认走这条）；仅当发现敏感信息才考虑 `git filter-repo` 重写。
- README：定位所有「生产就绪 / 跑在生产」措辞 → 统一降级为「面向生产场景设计的实验性框架」→ 中英同步 → 提交。
- 边界文档：照 T2 表组织成一篇面向用户的文档（位置 = `docs/spec/` 新增一篇或 README 单列一节，须定），中英同步，链接进 README / spec 索引。

## Testing Decisions

### 什么是好测试
- **只测外部行为，不测实现细节**：断言「给定输入 / 故障，可观测的对外行为是什么」，不断言私有内部状态或调用顺序——这样重构不伤测试。
- **优先复用既有缝，不新建缝**。本 effort 的理想缝数 = 2（见下）。

### 测试缝（已与用户确认）
1. **主缝：可运行 `src/main` demo（T4）**——多轮 + 工具 + 压缩端到端跑通即证明 core 可用；其行为**同时被 JUnit 断言**，由默认 `mvn verify` 兜回归。这是单一最高缝。
2. **辅缝：凭据门控的真实 LLM 集成测试 profile（T5）**——CI 默认跳过、本地带 key 跑。结构性脱离主缝（成本 / 凭据 / 稳定性迫使），不可避免。
- **深度测试并入各模块既有测试套件**（属性 / 故障注入 / 预算压力 / 回读边界 / 事实闭环），不另开测试工程——`mvn -pl <module> -am test` 仍是单模块统一入口。

### 深度测试类目（T3 DoD 定后逐项 graduate 为 per-module ticket）
- **memory**：属性测试 / 不变式；预算压力下压缩信息不断崖丢失（微压缩占位符 + 证据指针可回查；九段式摘要 P0 死保 / P3 先砍的优先级正确）。
- **spill**：read_range 字节区间 / JSON path / 分页三种回读的正确性边界；失败语义非对称（读侧 offload 失败降级透传不阻断、写侧 onload 失败阻断调用）。
- **guard**：HITL → state → Attachment 事实闭环（Hook 采集事实写 state、下一轮注入前渲染为 Attachment）；HITL 门禁下不可逆操作在框架层物理走不通、授权以 state 标记放行。
- **core**：并行工具 fan-out / 按序回注 / 超时与取消传播；崩溃续跑（悬空调用修复：完全悬空剔除 / 部分悬空合成中断结果）；去重 / 幂等。
- **SPI 冻结**：`api` 子包契约稳定到可冻结的证明。

### 既有的先验（Prior art，复用而非另造）
- **持久化 SPI 契约测试模式**：`buzhou-core` 发 test-jar，内含 `AbstractBuzhouStoresContractTest`（`buzhou-core/src/test/.../contract/`）；store 实现模块依赖该 test-jar 并继承——SPI 稳定性证明复用此模式。
- **demo 断言先验**：`examples/` 现有 `*DemoTest` / `SummaryEvaluationTest` / `AtomicToolsIntegrationTest` 的 JUnit 断言风格——T4 的「demo 行为被断言」延续此风格。
- **CI 守门命令**：`mvn -B verify`（`.github/workflows/ci.yml`）——T1 修复后，所有深度测试须在此命令下默认绿（真实 LLM 除外）。

## Out of Scope

- **其余五个机制模块的「做深」**：`buzhou-observe-otel` / `buzhou-observe-dashboard` / `buzhou-mcp` / `buzhou-skills` / `buzhou-tools`（run_command 安全除外）维持现状。MCP 热插拔经 T2 判定为 NATIVE，本 effort 仅在边界文档诚实标注，不为 MCP 再开 ticket。
- **发布到 Maven Central**：已有 `RELEASING.md` 流程，属后续独立 effort。
- **`examples/` 扩展**：除「一个可运行 `src/main` demo + 一条真实行为集成测试」外不扩。
- **从 git 历史抹除 `.scratch/`**：默认仅 untrack（`git rm --cached` + `.gitignore`）；仅当 T7 扫描发现敏感信息才考虑 `git filter-repo` 重写。
- **per-module 深度 ticket 的具体条目**：等 T3 验收基线定下后才从 MAP「Not yet specified」graduate，不在本 spec 内预先穷举。

## Further Notes

- **tracker 是本地 markdown**：详见上方 Implementation Decisions 的「tracker / triage 约定」段（本仓无 GitHub Issues / label，to-spec 类 skill 的 label 动作映射为 frontier 语义）。
- **当前 frontier**：T1、T3、T6、T7、T8、T9（T2 已闭合；T4 / T5 blocked-by T1）。**每会话最多解决一张 ticket**（research 例外）。
- **依赖链**：T4 / T5 blocked-by T1（须依赖可解析）；T3 / T9 blocked-by T2（已闭合 → 已解锁）。
- **已知事实（2026-08-13 核验）**：本地 `mvn -B -ntp clean verify` 十五模块全绿；非 clean 的 `mvn verify` 曾报 15 个 `NoSuchMethodError`，那是 `target/test-classes` 里已删除且从未提交的旧测试残留 `.class` 造成的幽灵错误，`mvn clean` 即消、非真实缺陷。本地 `spring-ai-bom-2.0.0.pom` 时间戳 = GA 当天 → 本地绿可信。
- **忠实度**：逐机制对照表的事实来自 T2 research（web 核验），引用源见 [T2 Assets](tickets/T2-spring-ai-native-vs-buzhou.md)。
