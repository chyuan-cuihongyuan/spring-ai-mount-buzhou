# Spec: 死循环与失控检测（08 — 双窗口数值硬顶 + 软退出通道 + 硬顶携带部分结果）

Status: ready-for-agent
源决策: wayfinder「production-readiness」08 号票（grilling 收口，决策=做；数值硬顶 + 语义重复检测两层分期；软退出通道是差异化亮点）
里程碑: M1 稳定基线（数值硬顶）+ M2 可控治理（确定性重复检测）。路线图「M1：03 → 05 → 06/07/08」；07 背压与多层限流已收口，本机制与其同属「失控防护家族」，收敛统一形态「双窗口计数 + 软信号 Attachment 注入 + 硬顶阻断转人工」（07 spec 开放问题已显式把 M2 收敛留给本机制）。

---

## Problem Statement

不周山的单轮推理（一次用户输入 → 最终回复）由 Spring AI 的 `ToolCallingAdvisor` 驱动 think→tool→think 循环。目前这个循环**对「行为失控」没有任何闸门**：

- **轮内步数无上限**：模型可以无限次「思考—调工具—再思考」。一个会把工具结果误读、或反复调用同一工具「确认」的模型，会把单轮推理烧成几十上百次模型调用，打爆 provider 配额、拖垮轮次时限、拖死实例线程——直到 token 耗尽或下游超时才被动停止。
- **工具调用次数无上限**：单轮可以无限次调工具。模型陷入「调一个工具 → 拿到结果 → 又调同一个工具」的死循环时，框架只是忠实地一次次执行，没有任何「你已经连续做了太多次」的裁决。
- **按工具无单独限额**：昂贵/慢工具（高延迟检索、付费 API）和廉价工具（本地读文件）共用一份隐性的「无限」额度。一个失控模型可以把昂贵工具打满，业务侧连「这类工具单轮最多调 N 次」的旋钮都没有。
- **单轮时长无硬顶**：单轮 wall-clock 完全靠工具超时（60s/把）和模型超时各自为政，没有「这一轮整体不许超过 T 秒」的轮次级裁决。一轮可以靠「每步都不超时但步数无限」地无限拖。
- **跨轮次累计无防线**：即使每轮各自有界，一个会话可以靠「每轮都烧到上限、连续几十轮」把会话生命周期的总步数/总工具调用推到不可接受——慢烧型失控没有任何会话级累计闸门。
- **失控只能被动崩、不能主动收**：到点没有任何「让模型主动收尾」的软通道，也没有「强制终止但保住已做的工作」的受控终态。终止 = 崩溃式超时/异常，已经做完的工具调用结果随轮次失败一起丢，被终止 ≠ 前功尽弃这件事框架不保证。
- **失控不可观测、不可转人工**：SRE 无法从事件流回答「这一轮是不是顶到步数上限被掐了、这个会话是不是在慢烧」；业务侧也无法把失控会话转人工接管。

从用户视角：平台集成者想让单轮推理在**行为失控**面前有数值闸门、能**主动收尾**（软退出）、到点**体面地停**（携带部分结果、有事件留痕、可转人工），而不是**默默地崩**或**无限地烧**；SRE 想在 observability 里看清每次失控处置。**注意与花费失控的分工**：本机制管「行为失控」（步数/调用次数/时长），「花费失控」（token 硬顶、预算）归 11 成本治理，两者正交。

## Solution

不周山落地**死循环与失控检测（失控防护）**，对齐业界最小共识（LangGraph `recursion_limit` 步数硬顶 + `RemainingSteps` 软退出；LangChain `ModelCallLimitMiddleware`/`ToolCallLimitMiddleware` 双窗口 + 按工具粒度；OpenAI Agents SDK `max_turns` + `MaxTurnsExceeded` 携带部分结果），**不新增模块 / 不新增 SPI / 不引外部依赖**——检测器主体挂在既有 Hook 链与 Attachment 注入通道上（core 扩展），硬顶终止复用既有取消传播路径：

- **双窗口数值硬顶（M1）**：① **轮次级**窗口——单轮最大思考步数（模型调用次数）、单轮最大工具调用次数、单轮 wall-clock 超时；② **会话级**窗口——会话生命周期累计步数、累计工具调用次数。两窗口各自可配、各自裁决。**按工具单独限额**（通配匹配工具名，单轮内某工具最多调 N 次）。
- **软退出通道（差异化亮点）**：达**软阈值**（剩余预算占比低于阈值）时，经既有 **Attachment 注入通道**向模型注入「剩余步数预算：N/M，请尽快收尾」的 `<system-reminder>` 信号——与事实闭环（FactCollector→state→Attachment）同构、同通道，让模型**主动收尾**而非被硬切。软阈值只是注入信号，不改变计数、不阻断。
- **硬顶终止携带部分结果**：达**硬顶**到点强制终止，受控终态**携带部分结果**（被终止 ≠ 前功尽弃：已完成的工具调用结果随 unit-of-work 落库，终止原因作为最终回复回注）。硬顶分两种落地：① **步边界硬顶**——在 `beforeModel` 切面检测到超限，返回 `Block(reason)`，reason 成为本轮最终回复（既有 HookAdvisor 已支持的路径）；② **轮次级 wall-clock**——同样在 `beforeModel` 步边界检测（诚实边界：轮次时长上界 = deadline + 单步时长，非中途精确打断）。
- **确定性重复检测（M2，标 `> 【推演】` + 自建评测）**：收敛为确定性规则——连续 N 次**同工具同参数**调用、或状态原地踏步；**不做语义相似度判断**（避免误杀合法的分页翻读/轮询类循环）。
- **失控事件进 observability**：`runaway.*` 事件族（软阈值/硬顶/按工具超额/重复检测）经既有 `SessionEvent` + `emitEvent` 通道发出（与 `backpressure.*`/`guard.*`/`drain.*` 同通道、同命名约定）。
- **可转人工**：硬顶事件携带部分结果指针，业务侧可经既有 HITL 确认事件通道（`guard.confirmation.requested` 先例）把失控会话转人工；与 15 运行时干预的干预平面在其落地后正式收敛（当前只交付事件 + 终止语义）。
- **默认姿态**：机制默认装配，**各阈值默认不限（null），显式配置才生效**——不设可能误伤生产的魔法默认值（对齐 07 背压的 safe-by-default）。

完整闭环：**用户输入 → 轮次启动（beforeTurn 重置轮次计数、启动 wall-clock、校验会话级窗口）→ 每步模型调用（beforeModel 递增步数、校验步硬顶/wall-clock、达软阈值注入剩余步数提醒、达硬顶 Block 终止携带部分结果）→ 每次工具调用（beforeTool 递增工具计数/按工具计数、指纹比对重复检测、超额 Block 或降级）→ 失控处置全量事件留痕 → SRE 在事件流看到全部处置、业务侧可转人工**。

## User Stories

1. 作为平台集成者，我希望配置**单轮最大思考步数**（模型调用次数硬顶），这样一个失控模型不会把单轮推理烧成上百次模型调用打爆 provider 配额。
2. 作为平台集成者，我希望配置**单轮最大工具调用次数**，这样模型陷入「调工具→拿结果→又调工具」循环时有上限。
3. 作为平台集成者，我希望配置**单轮 wall-clock 超时**，这样一轮整体不许超过 T 秒，不能靠「每步都不超时但步数无限」地无限拖。
4. 作为平台集成者，我希望配置**会话生命周期累计步数/工具调用上限**，这样慢烧型失控（每轮各自有界但连续几十轮）也有会话级防线。
5. 作为平台集成者，我希望会话级累计计数**跨崩溃保留**（持久化在 SessionStateStore），这样 AUTO_RESUME 重驱动一个崩溃中的失控会话不会重置预算、重新烧一遍。
6. 作为 Agent 应用开发者，我希望**按工具单独限额**（通配匹配工具名，如 `expensive_search` 单轮最多 3 次），这样昂贵/慢工具和廉价工具可以有不同的护栏。
7. 作为 Agent 应用开发者，我希望达软阈值时模型**收到「剩余步数预算：N/M，请尽快收尾」的注入信号**，从而**主动收尾**而不是被硬切丢掉已完成的工作。
8. 作为 Agent 应用开发者，我希望软阈值**只注入信号、不改变计数、不阻断**，这样软退出是「提示」不是「惩罚」，合法的长任务不会被误伤。
9. 作为 Agent 应用开发者，我希望软阈值**可配比例**（如剩余 < 20% 时注入），这样我能按业务任务的典型长度调软退出的提前量。
10. 作为 Agent 应用开发者，我希望硬顶终止时**已完成的工具调用结果不丢**（携带部分结果），这样被终止 ≠ 前功尽弃，用户至少拿到半成品。
11. 作为 Agent 应用开发者，我希望硬顶终止的**原因作为最终回复**回注给模型/用户（如「已达单轮步数上限 N，强制终止，已保留 N 步的部分结果」），这样终止是可解释的、不是黑屏。
12. 作为 Agent 应用开发者，我希望硬顶终止的回复**携带部分结果指针/摘要**，这样用户/下游可以继续基于半成品工作。
13. 作为 Agent 应用开发者，我希望硬顶终止后**会话不废**（会话仍可接受下一轮输入，只是本轮被掐），这样用户可以换个问法继续，不必重建会话。
14. 作为 Agent 应用开发者，我希望 wall-clock 超时**在步边界生效**并如实告知「上界 = deadline + 单步时长」的边界，这样我对终止时机的预期是准确的。
15. 作为平台集成者，我希望重复检测**用确定性规则**（连续 N 次同工具同参数），这样合法的分页翻读/轮询循环不会被语义相似度误杀。
16. 作为 Agent 应用开发者，我希望重复检测的**连续次数阈值可配**（默认保守、可关），这样我有旋钮也有退出。
17. 作为 SRE，我希望全部失控处置**事件留痕**（软阈值注入、硬顶终止、按工具超额、重复检测，带维度/计数/原因），这样失控处置可审计、可告警。
18. 作为 SRE，我希望硬顶事件**携带部分结果指针**，这样我能从事件追溯到具体的半成品产物。
19. 作为业务/HITL 集成者，我希望硬顶事件**可经既有 HITL 通道转人工**（与 `guard.confirmation.requested` 同形态），这样失控会话可以交人接管而不是默默终止。
20. 作为平台集成者，我希望三维阈值**默认不限、显式配置才生效**，这样引入升级不改变既有行为。
21. 作为平台集成者，我希望一键关闭/调参走 **yml 配置**（`buzhou.runaway.*` 前缀，boxed null=不限），特殊部署可回退。
22. 作为 Harness 集成者，我希望本机制**不新增模块 / 不新增 SPI / 不引外部依赖**（core 扩展 Hook + AttachmentRenderer），17 模块星形依赖不变。
23. 作为平台架构者，我知晓**花费失控（token 硬顶/预算）归 11 成本治理**——本机制管行为失控（步数/次数/时长），两者正交。
24. 作为平台架构者，我知晓**语义相似度重复检测不做**（避免误杀合法循环）、**可组合终止条件代数不做**（AutoGen `&`/`|` 式，过度设计）。
25. 作为平台架构者，我知晓**中途精确打断式 wall-clock 不在本期**（步边界检测为主，诚实边界写清），watchdog-cancel 为潜在增强。
26. 作为 Agent 应用开发者，我希望流式调用（`session.stream`）的失控检测**与同步 `chat` 语义一致**（步数/wall-clock 同样生效），这样流式不被绕过。
27. 作为 Agent 应用开发者，我希望公共 API 变更（配置项、事件类型、可能的异常类型）**全部 additive**，既有集成源码/二进制兼容。
28. 作为 Agent 应用开发者，我希望软退出注入**复用既有 Attachment 注入通道**（与事实闭环同构），这样注入位置/格式/预算扣除与既有事实注入一致，不引入第二条注入路径。
29. 作为 SRE，我希望失控事件**经既有 `SessionEvent`/`emitEvent` 通道**（与 `backpressure.*`/`guard.*`/`drain.*` 同通道、同命名约定），这样不新增事件总线。
30. 作为 Agent 应用开发者，我希望**会话级累计计数与崩溃恢复的幂等去重协同**——重复的同一调用若已被 dedup 闸门折叠，重复检测不重复计数/误报，这样两机制不打架。
31. 作为平台集成者，我希望**软退出提醒每步刷新**（模型每步看到最新的剩余预算），这样软信号是实时有效的。
32. 作为 Agent 应用开发者，我希望硬顶终止**与 06 drain、租约单活跃语义正交**（只终止本轮/标记会话，不做跨会话 interrupt/rollback），这样不与既有生命周期机制冲突。
33. 作为平台架构者，我希望**转人工的完整机制（挂起-回填、干预平面）与 15 运行时干预收敛**，当前只交付事件 + 终止语义 + HITL 通道对接，15 落地后正式收敛。

## Implementation Decisions

### 改动面与定位

- **core 扩展，不新增模块**（08 票与路线图口径一致：「检测器主体在执行脊柱，Hook 链发失控事件」）。检测器实现为**既有 `BuzhouHook`**（`beforeTurn`/`beforeModel`/`beforeTool` 切面）+ 既有 `AttachmentRenderer` 注入槽 + 既有 `session.cancel()` 取消路径。**不新增 Advisor、不新增 SPI、不新增事件通道、不引外部依赖**。
- **检测器形态决策（已核验现有缝合点）**：经核验，`HookAdvisor`（order `ToolCallingAdvisor.DEFAULT_ORDER + 600`）位于 Spring AI `ToolCallingAdvisor` 循环**之内**，其 `adviseCall` → `chain.beforeModel(ctx)` **每次模型调用（每个 think 步）触发一次**，`Block(reason)` 即以 reason 作为该次模型调用的回复文本（既有路径）。工具切面 `beforeTool`/`afterTool` 经 `HookedToolCallback` 包装层**每个工具调用触发一次**，`ctx.toolName()` + `ctx.arguments()` 可用。故所有计数/限额/重复检测/硬顶均挂既有 Hook 切面，**无需新增脊柱 instrumentation**。
- **不新增持久化 SPI、不新增事件通道**：轮次级计数单进程内存；会话级累计计数复用 `SessionStateStore`（通用 KV，参照 `recovery.autoresume.attempts` 先例，键如 `runaway.session.steps`/`runaway.session.tool-calls`）；事件走既有 `SessionEvent` 字符串 type + `emitEvent` 通道。
- **公共 API 变更（全部 additive，PR 须说明兼容性）**：新增 `buzhou.runaway.*` 配置属性 record；新增 `runaway.*` 事件类型字符串；新增一个 `AttachmentRenderer` 实现（软退出提醒）。可选新增 `RunawayTerminatedException`/结果类型用于携带部分结果指针（实现期按「Hook Block reason 已足够 vs 需结构化异常」定案，对齐 07 容量异常形态）。

### 双窗口数值硬顶（M1）

- **轮次级窗口（core，内存计数）**：
  - **步数计数（决策）**：在 `beforeModel` 递增「本轮步数」（每次模型调用 = 一步）；`beforeTurn` 重置。硬顶校验在 `beforeModel`、`nextCall` **之前**——超限返回 `Block(reason)`（reason 含「已达单轮步数上限 N」+ 部分结果摘要），复用 HookAdvisor 既有 Block→reply 路径。
  - **工具调用计数（决策）**：在 `beforeTool` 递增「本轮工具调用数」（每次实际执行的工具调用）；`beforeTurn` 重置。硬顶校验在 `beforeTool`、工具执行**之前**——超限返回 `Block(reason)`（reason 回注为工具结果文本，对齐 `beforeTool` Block 语义），或按配置降级为「工具未执行」结果。
  - **wall-clock（决策，诚实边界）**：`beforeTurn` 记录轮次起始 `Instant`；`beforeModel` 检查 `Duration.between(start, now) > wallClockLimit` → `Block(reason)`。**诚实边界写进文档**：轮次时长上界 = `wallClockLimit + 单步时长`（一次模型调用延迟 + 一次工具超时），**非中途精确打断**；中途 watchdog-cancel 为潜在增强（见 Out of Scope）。
- **会话级窗口（core，SessionStateStore 持久化）**：
  - **累计计数（决策）**：会话生命周期累计步数/工具调用数，持久化在 `SessionStateStore`（键 `runaway.session.steps`/`runaway.session.tool-calls`，参照 `recovery.autoresume.attempts` 读写先例）。每次 `beforeModel`/`beforeTool` 递增后校验会话级硬顶——超限 `Block(reason)`（reason 含「已达会话累计步数上限」）。
  - **跨崩溃保留（决策）**：计数持久化在 store → AUTO_RESUME 重驱动时计数不重置，避免崩溃-恢复循环重烧预算。与 11 幂等去重协同（见下）。
  - **重置语义**：会话级窗口随会话删除而清除（store 生命周期），不提供手工重置 API（实现期评估是否需要）。
- **按工具单独限额（core，内存计数）**：
  - **计数（决策）**：`beforeTool` 维护 `Map<String, Integer>` 按工具名（通配匹配，复用既有 `ToolPolicyMatcher` 形态）计数；`beforeTurn` 重置。超限返回 `Block(reason)` 或降级结果，并发 `runaway.per-tool-exceeded` 事件。
  - **通配匹配**：工具名匹配走既有 glob 手法（exact 优先 → 最长前缀 `*`），与 spill/guard 的工具策略匹配一致。

### 软退出通道（差异化亮点，M1）

- **注入路径（决策，复用既有通道）**：新增 `AttachmentRenderer` 实现（如 `RunawayBudgetRenderer`），经既有 `CompositeAttachmentRenderer` 折进 `InjectionViewProcessor` 的 `<system-reminder>` 注入块（插在近期原文之前，与事实块同位）。**不新增注入槽、不新增注入路径**——与事实闭环（FactCollector→state→Attachment）同构同通道。
- **触发条件（决策）**：renderer 读轮次级步数计数器；当 `remaining / limit < softThresholdRatio`（默认如 0.2，即剩余 < 20%）时渲染「剩余步数预算：N/M，请尽快收尾并给出结论」，否则返回 `Optional.empty()`（不注入）。
- **每步刷新（已核验）**：`BuzhouMemoryAdvisor`（order +400，位于循环内）每次模型调用重建注入视图 → `AttachmentRenderer.render()` 每步触发 → 软退出提醒**每步刷新**。诚实标注切面次序：注入视图在 memory(+400) 构建，步数计数在 hook(+600) `beforeModel` 递增——故本步注入用的是「上一步末」的计数（一步滞后，可接受；模型看到的是「进入本次调用时的预算」）。
- **软阈值语义（决策）**：**只注入信号、不递减计数、不阻断**。软退出是「提示模型主动收尾」，不是惩罚；合法长任务不受影响（remaining 未跌破阈值即不注入）。
- **与事实注入的预算关系**：软退出提醒字符数计入既有 `buzhou.facts.max-inject-chars` 共享总量（与事实块共享，同一份预算不超额），超出截断附指针（对齐事实注入既有规则）。

### 硬顶终止携带部分结果（M1）

- **步边界硬顶（决策，主路径）**：`beforeModel`/`beforeTool` 检测硬顶 → 返回 `Block(reason)`。`reason` 为「受控终态」文本：包含触发维度（steps/tool-calls/wall-clock/session-steps/session-tool-calls/per-tool）+ 上限 + 当前值 + 部分结果摘要/指针。`beforeModel` 的 Block 经 HookAdvisor 既有路径成为本轮最终回复；`beforeTool` 的 Block 经既有 beforeTool Block 语义回注为工具结果文本。
- **部分结果保留（决策，须实现期核验）**：受控终态**携带部分结果**——本轮已完成的工具调用结果随 unit-of-work 落库（参照 drain force-kill 保留缓冲写先例：`GracefulShutdownEndToEndTest.drainForceKillsAndFlushesExitTierBufferedWrites` 断言 messageStore 非空）。**实现期必须核验**：硬顶 Block 路径下，本轮累积的中间工具结果确实随轮次提交落库（非仅 Block reason 文本），否则「携带部分结果」承诺落空。
- **会话存活（决策）**：硬顶只终止本轮（回复终止原因），**不关闭/不废会话**——会话仍可接受下一轮输入（用户可换问法继续）。与 06 drain、租约单活跃语义正交。
- **wall-clock 与既有 model deadline 的关系**：本机制 wall-clock 是**轮次级**，与 10 韧性的 `ResilienceProperties.deadline`（单次模型调用级）正交；两者可共存（轮次 wall-clock 通常 ≥ 单步 deadline）。实现期确认两者不相互误清零 in-flight 注册。

### 确定性重复检测（M2，标 `> 【推演】` + 自建评测）

- **规则（决策，`> 【推演】`）**：在 `beforeTool` 维护指纹环缓冲（指纹 = 规范化 `(toolName, canonicalJson(arguments))` 的稳定哈希，复用 07 HITL 参数指纹手法）；检测**连续 N 次**（默认如 3，可配）同工具同参数调用 → 发 `runaway.repetition` 事件 + `Block`（或按配置仅告警不阻断）。
- **不做语义相似度（决策）**：**不做**语义相似度/embedding 判断——避免误杀合法的分页翻读、轮询、批量处理类循环（这些是「参数变化、目的正当」的合法循环，确定性同参数规则不误伤）。
- **状态原地踏步（`> 【推演】`）**：可选第二规则——连续 N 步工具结果指纹相同（结果未变化）视为原地踏步；留实现期 + 自建评测定案（评测集须含合法分页/轮询正例，防误杀）。
- **与幂等去重的协同（决策，须实现期核验）**：11 崩溃恢复的 dedup 闸门（reserve-then-fill）会折叠重复的同一调用。**核验点**：dedup 命中的调用若在 `beforeTool` 之前短路（不经 HookedToolCallback），重复检测不重复计数——两机制协同而非打架；dedup 关闭（持久化强度=none）时重复检测独立生效。实现期确认 dedup 短路与重复计数的关系，避免漏判/双重计数。

### 失控事件进 observability（M1）

- 新增事件类型（字符串 type，参照 `backpressure.*`/`drain.*` 命名约定，经既有 `SessionEvent` + `emitEvent` 通道）：
  - `runaway.soft-threshold`（sessionId/turn/counter/steps|tool-calls/limit/remaining）——软阈值触发，提醒已注入；
  - `runaway.hard-stop`（sessionId/turn/reason[steps|tool-calls|wall-clock|session-steps|session-tool-calls|repetition]/limit/value/partialResultRef）——硬顶终止，携带部分结果指针；
  - `runaway.per-tool-exceeded`（sessionId/turn/toolName/limit/value）——按工具限额触发；
  - `runaway.repetition`（sessionId/turn/toolName/fingerprint/count）——重复检测触发（M2）。
- **事件落 ObservabilityStore 的路径（须实现期定案）**：现有 `SessionEvent` 通道（达 SessionEventListener + hook `onEvent`）与 `ObservabilityStore`（达 dashboard/OTel/Micrometer）**当前无桥接**。实现期二选一定案：① 检测器在持有 `SpanContextCarrier` 时直接经 `SpanRecorder.emit(span, "runaway.*", payload)` 落 store（免费得 Micrometer 计数 + OTel 导出）；② 新增一个 SessionEventListener→ObservabilityStore 桥接（与既有 `backpressure.*`/`guard.*` 一致地走 `emitEvent`，再统一桥接）。推荐 ①（与可观测 span 同位，免费 metering），但须确认脊柱/Hook 侧 span 上下文可得。

### 转人工（M1 事件 + HITL 通道对接；完整机制留 15 收敛）

- **当前交付（决策）**：硬顶 `runaway.hard-stop` 事件携带部分结果指针，业务侧可经既有 HITL 确认事件通道（`guard.confirmation.requested` 形态）把失控会话转人工接管。即：失控终止 = 受控终态 + 事件，业务/HITL 层订阅事件决定是否接管。
- **与 15 运行时干预的收敛（留待 15 落地）**：15 的干预平面（挂起-回填原语）落地后，硬顶可改为「挂起待人工」而非「终止携带部分结果」。本期只交付事件 + 终止语义 + HITL 通道对接，不做完整挂起-回填。

### 配置范围

- **core 侧** `@ConfigurationProperties(prefix="buzhou.runaway")`（record + compact constructor + boxed null=不限，对齐 `BuzhouBackpressureProperties`/`BuzhouShutdownProperties` 模板）：
  - `enabled`（默认 true，safe-by-default）
  - `per-turn.max-steps`（默认 null=不限）/ `per-turn.max-tool-calls`（默认 null=不限）/ `per-turn.wall-clock`（默认 null=不限）
  - `per-session.max-steps`（默认 null=不限）/ `per-session.max-tool-calls`（默认 null=不限）
  - `per-tool.<glob>.max-calls`（map，默认空=不限；通配匹配）
  - `soft-threshold-ratio`（默认 0.2——剩余 < 20% 注入软退出提醒；仅在有 `max-steps` 时生效）
  - `repetition.consecutive`（默认 null=关；M2，开启后值如 3）/ `repetition.action`（block|flag-only，默认 block）
  - `escalate-policy`（默认 emit-event；未来 hitl/转人工）
- **绑定级/工具级 policy 覆盖不在本期**（同 07 背压 M1 口径：policy 消费管线打通前只到默认 + yml 两层；08 票「阈值走 policy 四层」的绑定级诉求留待管线打通后接入，如实标注）。注意：按工具限额的 `<glob>` 匹配是 yml 层的工具名通配，**不是**四层 policy 的工具级覆盖。

### 工程纪律

- 不新增模块、不新增 SPI、不引外部依赖（计数/指纹全内存；会话级计数复用既有 SessionStateStore）；行为变更带测试（CONTRIBUTING 约定）。
- 阈值默认 null=不限；保留处禁魔法数字，比例/默认值显式命名常量并注释。
- 借鉴（一手链接见 `docs/production-readiness/references.md` 与 08 号票 Answer）：LangGraph `recursion_limit` + `GraphRecursionError`（步数硬顶 + 专用异常 + 按调用覆盖）；LangGraph `RemainingSteps`（软退出通道蓝本）；LangChain v1 `ModelCallLimitMiddleware`/`ToolCallLimitMiddleware`（双窗口 + 按工具粒度）；OpenAI Agents SDK `max_turns` + `MaxTurnsExceeded`（异常携带部分结果）；AutoGen Termination Conditions（**评估后不做**——可组合代数 `&`/`|` 过度设计，记录于此）。

## Testing Decisions

### 什么是好测试

只测**外部行为**——硬顶在正确的步数/调用数/时长触发、软退出提醒在软阈值触发时确实进入模型所见 prompt、部分结果在硬顶后确实落库、事件流正确——不测计数器私有字段、指纹缓冲内部状态。「步数是否到 N 就停、部分结果是否保留、软退出是否注入」一律从外部观察判定（同 `CrashRecoveryEndToEndTest`/`ResilienceEndToEndTest`/`GracefulShutdownEndToEndTest` 的 e2e 哲学）。wall-clock 测试用 `BlockingChatModel`/`BlockingTool` + 短 deadline 保证确定性，**不用** wall-clock sleep（对齐 07 背压测试纪律）。

### 缝合点（1 个主缝合点，复用既有 e2e 形态，不新增测试基础设施）

1. **主缝合点（最高、复用既有 e2e 形态）——端到端失控检测测试**：
   - 复用 `CrashRecoveryEndToEndTest`/`GracefulShutdownEndToEndTest`/`ResilienceEndToEndTest`/`BackpressureEndToEndTest` 装配与手法：`Buzhou.runtime(model, stores, config)` + core test-jar `ScriptedChatModel`（enqueue 多个 tool-call assistant message 后给最终回复）+ `session.addEventListener(events::add)` + latch 阻塞工具（带计数器）+ 必要时 `BlockingChatModel`/`BlockingTool`。
   - 经真实 runtime 驱动，断言：
     ① **步数硬顶**——`per-turn.max-steps=3`，ScriptedChatModel 预排 ≥3 个 tool-call message，断言 `model.seenPrompts.size()==3`（第 4 次模型调用未发起）、`runaway.hard-stop` 事件 reason=steps、最终回复含终止原因、`stores.messageStore().load(sid)` 含本轮部分工具结果（**部分结果保留核心断言**，照搬 `drainForceKillsAndFlushesExitTierBufferedWrites` 形态）；
     ② **工具调用硬顶**——`per-turn.max-tool-calls=N`，工具被调 >N 次，断言 `runaway.hard-stop` reason=tool-calls；
     ③ **按工具限额**——`per-tool.expensive_*.max-calls=2`，第 3 次断言 `runaway.per-tool-exceeded` + 降级/Block；
     ④ **wall-clock**——`per-turn.wall-clock=短值` + `BlockingTool`，断言 `runaway.hard-stop` reason=wall-clock（步边界，对齐 `deadlineTimeoutFiresAndCancelsInFlightCall` 形态）；
     ⑤ **会话级累计**——预置 `sessionStateStore` 的 `runaway.session.steps` 接近上限（照搬 `crashloopHardCapStopsRepeatedAutoResume` 形态），下一轮断言 `runaway.hard-stop` reason=session-steps；并断言跨崩溃（重 spawn）计数不重置；
     ⑥ **软退出注入**——包装 ChatModel 捕获 prompt（照搬 `HookEndToEndTest` observing 包装）或读 `injectionSnapshot(sid, turn)`，断言 remaining 跌破 `soft-threshold-ratio` 时 prompt 含「剩余步数预算」`<system-reminder>` 块、未跌破时不注入；
     ⑦ **重复检测（M2）**——预排连续 N 次同工具同参数 tool_call，断言 `runaway.repetition` 事件 + Block；
     ⑧ **流式对等**——`session.stream` 下的步数/wall-clock 硬顶同样生效（对齐 `drainWaitsForInFlightStreamTurn` 形态）；
     ⑨ **事件流**——`runaway.*` 事件按序、payload（维度/计数/上限/部分结果指针）正确；
     ⑩ **默认姿态**——不配置任何阈值时行为与现状完全一致（回归断言：null=不限，零计数器开销不改变循环）；
     ⑪ **会话存活**——硬顶后同一 session 再 `chat` 仍正常响应（会话不废）。
   - 先验：`CrashRecoveryEndToEndTest`（sessionStateStore 计数预置/跨崩溃/latch 工具）、`GracefulShutdownEndToEndTest`（cancel 传播 + 部分结果保留 + 流式）、`ResilienceEndToEndTest`（ScriptedChatModel 预排 + `seenPrompts` 计数 + BlockingChatModel wall-clock）、`BackpressureEndToEndTest`（事件流断言 + 计数佐证）、`HookEndToEndTest`（observing 包装证注入到达模型）、`HarnessToolCallingManagerTest`（工具计数手法）。

> 备选次缝合点（**不**默认采用）：纯单测缝合点（`HookChainTest` 形态，直接以合成 `ModelCallContext`/`ToolCallContext` 驱动检测器 Hook，断言计数/指纹逻辑）。仅在计数/指纹逻辑复杂到 e2e 反馈太慢时引入；默认只保留上述 1 个 e2e 主缝合点（「越少越好、最高缝合点优先」）。

### 被测模块

- `buzhou-core`（检测器 Hook + 软退出 AttachmentRenderer + 会话级计数 SessionStateStore 读写 + `buzhou.runaway` properties + autoconfig 接线）。
- 不涉及新模块；store 无语义变更（复用既有 KV）；observability 模块不改（事件走既有通道）；memory 模块不改（复用既有 InjectionViewProcessor/AttachmentRenderer 槽）。

## Out of Scope

- **花费失控 / token 硬顶 / 预算**——归 11 成本治理；本机制只管行为失控（步数/调用次数/时长），两者正交。
- **语义相似度重复检测**——08 票明确不做，避免误杀合法分页翻读/轮询/批量处理循环；只做确定性同参数规则。
- **可组合终止条件代数**——AutoGen `&`/`|` 式可组合条件，08 票评估后认定为过度设计，不做。
- **中途精确打断式 wall-clock**——本期 wall-clock 在步边界生效（诚实边界 = deadline + 单步时长）；独立 watchdog 线程到点调 `session.cancel()` 的中途打断为潜在增强，不在本期。
- **绑定级/工具级 policy 四层消费**——同 07 背压 M1 口径，待 policy 消费管线打通；本期的按工具 `<glob>` 匹配是 yml 层通配，非四层 policy 工具级覆盖。
- **分布式跨实例精确累计**——会话级计数虽持久化在 store（共享 store 下跨实例可见），但单步递增/校验是单进程内存语义；不做分布式原子计数（不引 Redis 强依赖，对齐 07 背压口径）。
- **转人工完整机制（挂起-回填、干预平面）**——与 15 运行时干预收敛，留待 15 落地；本期只交付事件 + 终止语义 + HITL 通道对接。
- **会话级计数手工重置 API**——实现期评估，默认随会话删除清除、不提供手工重置。
- **重复检测的状态原地踏步精确算法**——M2 给规则形态（连续同结果指纹），算法细节 + 评测集（含合法分页/轮询正例）留实现期 + 自建评测定案。

## Further Notes

- **管辖 ADR**：wayfinder「production-readiness」08 号票（决策=做；数值硬顶 + 语义重复检测两层分期；软退出通道是差异化亮点；花费失控归 11；可组合代数不做）。路线图落点：`docs/production-readiness/README.md` M1 行「08 死循环与失控检测 | 做 | M1 数值硬顶（轮次×会话双窗口 + 按工具 + wall-clock）+ M2 确定性重复检测；软退出通道 + 硬顶携带部分结果 | core 执行脊柱 + Hook」。
- **与既有机制的接口清单（均已交付，直接消费）**：`BuzhouHook` 六切面 + `HookResult` 密封三态（07，计数/限额/硬顶 Block 的挂接面）；`HookAdvisor` 循环内位 + `Block(reason)`→reply 路径（07，步边界硬顶）；`HookedToolCallback` 的 `beforeTool`/`ctx.toolName()`/`ctx.arguments()`（07，工具计数/按工具限额/重复指纹）；`AttachmentRenderer` + `CompositeAttachmentRenderer` + `InjectionViewProcessor`（07/01，软退出注入槽，每步刷新）；`SessionStateStore` KV + `recovery.autoresume.attempts` 读写先例（11，会话级累计计数跨崩溃保留）；`session.cancel()` + `cancelInFlight` + `ModelCallInFlight`（10/06，中途取消路径，watchdog 增强用）；`ToolPolicyMatcher` glob（07，按工具通配匹配）；`SessionEvent` + `emitEvent` 通道 + `backpressure.*`/`drain.*`/`guard.*` 命名先例（事件族）；drain force-kill 部分结果保留断言（06，携带部分结果测试形态）。
- **实现期须核验的开放项**：
  1. **部分结果保留**：硬顶 `Block` 路径下，本轮累积中间工具结果确实随轮次 unit-of-work 落库（非仅 Block reason 文本）——这是「携带部分结果」承诺的关键，照搬 drain force-kill 测试形态核验。
  2. **会话级计数与 dedup 协同**：11 dedup 闸门短路重复调用时，重复检测/工具计数是否漏判或双重计数——确认 dedup 短路与 `beforeTool` 的先后关系。
  3. **软退出注入的切面次序**：注入视图在 memory(+400) 构建、步数在 hook(+600) `beforeModel` 递增——确认一步滞后可接受、注入用「进入本次调用时」的预算语义正确。
  4. **wall-clock 与 model deadline 的关系**：轮次级 wall-clock 与 10 单步 deadline 正交共存，确认不相互误清零 in-flight 注册；wall-clock 步边界边界（deadline + 单步时长）如实文档化。
  5. **事件落 ObservabilityStore 的路径**：选 SpanRecorder.emit 直发（推荐，需 span 上下文可得）还是 SessionEventListener→store 桥接——实现期定案并保持与 `backpressure.*`/`guard.*` 一致。
  6. **流式对等**：`session.stream` 下步数/wall-clock 硬顶同样生效（流式 Block 的回复序列与同步一致），照搬 drain 流式测试形态核验。
- **未来集成点（设计时预留，不在本期实现）**：15 运行时干预落地后，硬顶可由「终止携带部分结果」升级为「挂起待人工」（挂起-回填原语）；11 成本治理的 token 硬顶与本机制的步数硬顶共享「双窗口计数 + 软信号注入 + 硬顶阻断」形态（失控防护家族统一形态，07 spec 开放问题已把 M2 收敛留给本机制）；watchdog-cancel 中途精确打断 wall-clock；AIMD 式自适应步数限额（潜在增强）。
- **文档交付**：「与花费失控的分工」（行为失控 vs 花费失控，正交）、「wall-clock 步边界诚实边界」（上界 = deadline + 单步时长）、「与幂等去重的协同」写入机制文档；配置项全表 + `runaway.*` 事件类型全表 + 软退出提醒文案模板落 docs/spec 同步（「改机制先改 Spec」，新增 `docs/spec/14-runaway-detection.md` 对齐既有八节模板）。
