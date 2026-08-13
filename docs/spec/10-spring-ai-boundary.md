# 10 与 Spring AI 2.0 原生能力的边界 · Boundary vs Spring AI 2.0

> 本文件面向**评估者与集成者**，诚实标注 Buzhou 九大机制相对 Spring AI 2.0.0 原生能力的关系：**REPLACES**（Spring AI 没有、Buzhou 替代）/ **ADDS**（Spring AI 有、Buzhou 增强）/ **NATIVE**（Spring AI 已原生、Buzhou 仅 wrapper）。事实骨架来自 [T2 research](../../.wayfinder/tickets/T2-spring-ai-native-vs-buzhou.md)（web 核验，2026-08）；术语以仓库根 [CONTEXT.md](../../CONTEXT.md) 为准。
>
> This document is for **evaluators and integrators**. It honestly classifies each of Buzhou's nine mechanisms against Spring AI 2.0.0 native capabilities: **REPLACES** (Spring AI lacks it, Buzhou provides) / **ADDS** (Spring AI has it, Buzhou enhances) / **NATIVE** (Spring AI already provides it natively, Buzhou only wraps). The factual skeleton comes from [T2 research](../../.wayfinder/tickets/T2-spring-ai-native-vs-buzhou.md) (web-verified, 2026-08).

---

## English

Spring AI 2.0 solves "how to wire model + tools + Advisors together". Buzhou is a runtime **harness layered on top of Spring AI** — it does not replace your `ChatClient` / `ChatModel`. The honest question an evaluator must answer is: *relative to Spring AI 2.0, what does Buzhou actually add?* The table below is the answer. Confidence: **High / Med-high / Med**.

### What Spring AI lacks → Buzhou REPLACES

| Mechanism | Spring AI 2.0 native | Buzhou | Confidence |
|---|---|---|---|
| **Spill protection / `read_range`** | None — no large-output offload, no truncation, no byte-range / JSON-path / paged readback | Spill store + `read_range` readback tool; oversized tool outputs land on disk and are read back by byte range / JSON path / pagination | **High** |
| **Parallel tool calls** | `DefaultToolCallingManager` (GA) executes tools **sequentially**; no executor, no per-call timeout, no cancellation | Virtual-thread fan-out, ordered re-injection, per-call timeout & cancellation propagation, crash-recovery for dangling calls | **High** |
| **Skill system (capability prompts)** | Closest is `ToolSearchToolCallingAdvisor` (progressive tool-schema disclosure — *not* a capability prompt) | Skill = capability prompt + tool bundle, mounted as a unit | Med |

> **Strongest survival differentiator:** parallel tool calls. The moat is **implementation depth** (timeouts, cancellation, crash-recovery), not the concept.

### What Spring AI has → Buzhou ADDS

| Mechanism | Spring AI 2.0 native | Buzhou adds | Confidence |
|---|---|---|---|
| **Progressive memory compaction** | Core ships only `MessageWindowChatMemory` (turn-count window); no token budget, no summarization, no compaction. Community `spring-ai-session` adds event-sourced ChatMemory + pluggable compression | Micro-compaction (completed turns as atomic units) + nine-section summary + dynamic budget (debit-then-compute); placeholder + evidence pointer for recall | Med-high |
| **Cognitive observability** | Mature Micrometer/OTel *operational* observability; but prompt/completion, tool args/results are **not captured by default**, no reasoning, no span events | Span+Event cognitive model (evidence / reasoning / span event) — **integrated into** the OTel pipeline rather than a parallel one | **High** |
| **Hook guardrails** | Advisors support pre/post interception & veto; built-in `SafeGuardAdvisor`; no packaged HITL, no long-output guardrail, no state→attachment fact loop | Packed HITL gate, long-output guardrail, **HITL → state → Attachment fact loop** (deterministic fact capture, not relying on the LLM) — **built on** Advisors | **High** |
| **Persistence SPI** | `ChatMemoryRepository` (multi-backend) + `spring-ai-session` JDBC; no Summary / SessionState / SessionLease / Observability store abstractions | Dedicated store SPI for Summary / SessionState / SessionLease / Observability | Med-high |
| **Atomic tools** | `@Tool` can expose any method, but core ships **no curated tool set** | Curated atomic tool set (e.g. `read_range`); `run_command` is safe-by-default (see slice 07) | Med-high |

### What Spring AI already does → Buzhou is NATIVE (wrapper only)

| Mechanism | Spring AI 2.0 native | Buzhou | Confidence |
|---|---|---|---|
| **MCP hot-swap** | **Native** — dynamic tool add/remove, no restart, immediately available | Buzhou only wraps it; **not a differentiator** | **High** |

> **Honest note:** MCP hot-swap is a Spring AI 2.0 native capability. Buzhou's MCP module is a thin wrapper and adds no differentiation here. This is stated plainly so the other claims in this document remain trustworthy. (MCP is out of scope for the current "core deepening" effort — see [SPEC](../../.wayfinder/SPEC.md).)

### Bottom line

Buzhou's real differentiation is **REPLACES** (Spill, parallel tools) and the **ADDS** layer built on Advisors (memory compaction, cognitive observability, hook guardrails, persistence SPI). Where Spring AI is already native (MCP hot-swap), Buzhou honestly does not claim credit.

---

## 中文

Spring AI 2.0 解决了「如何把模型、工具、Advisor 链接到一起」的问题。Buzhou 是**叠加在 Spring AI 之上的运行时中间层（Harness）**——不替代你的 `ChatClient` / `ChatModel`。评估者必须回答的诚实问题是：*相对 Spring AI 2.0，Buzhou 到底补了什么？* 下表就是答案。置信度：**高 / 中高 / 中**。

### Spring AI 没有的 → Buzhou REPLACES

| 机制 | Spring AI 2.0 原生 | Buzhou | 置信 |
|---|---|---|---|
| **Spill 溢出 / `read_range`** | 无——无大产物落盘、无截断、无字节区间 / JSON path / 分页回读 | Spill store + `read_range` 回读工具；大工具返回落盘，按字节区间 / JSON path / 分页回读 | **高** |
| **并行工具调用** | `DefaultToolCallingManager`（GA）**顺序执行**；无 executor、无单工具超时、无取消 | 虚拟线程 fan-out、按序回注、单工具超时与取消传播、悬空调用崩溃续跑 | **高** |
| **Skill 体系（能力 prompt）** | 最近的是 `ToolSearchToolCallingAdvisor`（渐进披露工具 schema——*非* 能力 prompt） | Skill = 能力 prompt + 工具集，作为一个单元挂载 | 中 |

> **最强存活差异化**：并行工具调用。护城河在**实现深度**（超时 / 取消 / 崩溃续跑），不是概念。

### Spring AI 有的 → Buzhou ADDS

| 机制 | Spring AI 2.0 原生 | Buzhou 增强 | 置信 |
|---|---|---|---|
| **渐进式记忆压缩** | 核心仅 `MessageWindowChatMemory`（按条数窗口）；无 token 预算、无摘要、无压缩。社区 `spring-ai-session` 补事件源 ChatMemory + 可插拔压缩 | 微压缩（完结轮次为原子单位）+ 九段式摘要 + 动态预算（先扣后算）；占位符 + 证据指针可回查 | 中高 |
| **认知可观测** | 成熟的 Micrometer/OTel *运维*可观测；但 prompt/completion、工具入参/结果**默认不采集**、无 reasoning、无 span event | Span+Event 认知模型（证据 / 推理 / span event）——**整合进** OTel 管线、而非另起一条 | **高** |
| **Hook 护栏** | Advisor 支持 pre/post 拦截与阻断；内置 `SafeGuardAdvisor`；无打包 HITL、无长产物护栏、无 state→attachment 事实闭环 | 打包 HITL 门禁、长产物护栏、**HITL → state → Attachment 事实闭环**（确定性采集事实、不靠 LLM 自觉）——**建在** Advisor 之上 | **高** |
| **持久化 SPI** | `ChatMemoryRepository`（多后端）+ `spring-ai-session` JDBC；无 Summary / SessionState / SessionLease / Observability store 抽象 | 专属 store SPI：Summary / SessionState / SessionLease / Observability | 中高 |
| **原子工具** | `@Tool` 可暴露任意方法，但核心**无策展工具集** | 策展原子工具集（如 `read_range`）；`run_command` 默认安全（见 impl/07） | 中高 |

### Spring AI 已原生的 → Buzhou NATIVE（仅 wrapper）

| 机制 | Spring AI 2.0 原生 | Buzhou | 置信 |
|---|---|---|---|
| **MCP 热插拔** | **原生**——动态工具增删、免重启、即时可用 | Buzhou 仅 wrapper、**非差异化** | **高** |

> **诚实标注**：MCP 热插拔是 Spring AI 2.0 的原生能力，Buzhou 的 MCP 模块只是薄封装、在此不提供差异化。此条单独说明，以保本文件其余论断可信。（MCP 不在当前「core 做深」effort 范围——见 [SPEC](../../.wayfinder/SPEC.md)。）

### 结论

Buzhou 的真实差异化在 **REPLACES**（Spill、并行工具）与建在 Advisor 之上的 **ADDS** 层（记忆压缩、认知可观测、Hook 护栏、持久化 SPI）。Spring AI 已原生的（MCP 热插拔），Buzhou 诚实不揽功。

---

## 对照参考 / Cross-references

- 逐机制详设：[00 总览](00-overview.md) · [01 记忆压缩](01-memory-compaction.md) · [02 Spill](02-spill.md) · [03 可观测](03-observability.md) · [04 Skill/MCP](04-skill-mcp.md) · [05 并行工具](05-parallel-tools.md) · [06 原子工具](06-atomic-tools.md) · [07 Hook 护栏](07-hooks.md) · [08 会话/配置/持久化](08-session-config-persistence.md)
- 事实来源：[T2 research](../../.wayfinder/tickets/T2-spring-ai-native-vs-buzhou.md)（web 核验）。
- 项目状态：实验性（alpha），公共 API（`api` 子包）尚未冻结——见 [README §项目状态](../../README.md#项目状态)。
