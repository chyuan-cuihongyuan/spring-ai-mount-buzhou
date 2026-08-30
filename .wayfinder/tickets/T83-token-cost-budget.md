---
Type: task
Status: closed
assignee: zcode
blocked-by:
---
## Question

Token/成本计量与预算闸怎么做？现状：usage 只到 per-call/per-turn span（ObservabilityAdvisor 写 span+Micrometer），无会话累计、无货币成本、RunawayHook 闸门只有步数/工具数。借鉴：OpenAI Agents SDK /cost、LangSmith cost per trace、Claude Code 会话成本显示。决策点：会话级累计的存放（SessionStateStore vs ObservabilityStore vs 新 SPI）、成本价目配置形态（per-model 价格表，默认无价=不计金额）、token 硬顶进 RunawayHook 四层硬顶之外的第五层、事件/指标/dashboard 可见性。产出 spec 16 + impl 58。

## Resolution

AFK 自决（授权同 T81，可推翻）：

1. **归属**：core 新 `budget` 包 `TokenBudgetHook`（挂 BuzhouHook 链，order 1100 在 runaway(1000) 之后）——与 RunawayHook 同范式而非并入（runaway 语义是「行为失控步数」，token/cost 是「资源消耗预算」，分开演进）。
2. **累计存放**：SessionStateStore（`ctx.state()`，per-session 持久化跨崩溃，与 runaway 会话累计同先例），值以字符串存 long（prompt-tokens / completion-tokens / cost-micro-usd 三键）；per-session 锁防工具扇出并行 RMW 竞争。
3. **计量来源**：`afterModel` 从 `ctx.response().chatResponse().metadata().usage()` 提取（与 ObservabilityAdvisor 同源同口径）；null/零 usage 跳过（替身模型无 usage 不误记）。每模型调用发 `budget.tokens-accumulated` 事件（含会话累计快照 + 可选成本）。
4. **成本价目**：`buzhou.token-budget.pricing.<model>.{input-per-million,output-per-million}`（BigDecimal USD）；模型名取 chatOptions.model 回退 `buzhou.model-name`。无价目=零成本（只计 token）。成本以 micro-USD long 累计（token × 每百万价 = 恰好 microUsd/token，无浮点误差）。
5. **预算闸**（safe-by-default，null=不限）：`max-session-prompt-tokens` / `max-session-total-tokens` / `max-session-cost-usd`（设 cost 上限而未配价目 → 启动 fail-fast）。`beforeModel` 检查：超限 `budget.token-hard-stop` / `budget.cost-hard-stop` 事件（携带 partialResultRef，同 runaway 硬顶口径）+ `HookResult.block` 终止（已消耗预算不可逆，下一模型调用前拦截）。
6. **配置**：`buzhou.token-budget.*`（`BuzhouTokenBudgetProperties`，enabled 默认 true、阈值 null=不限——无阈值空转零开销）；装配对齐 runawayHook bean 模式（@ConditionalOnMissingBean(name)+observabilityStore 双写）。
7. **可见性**：事件三件（accumulated/token-hard-stop/cost-hard-stop）双写 SessionEvent+EventRecord（dashboard 可查）；指标 `buzhou.budget.prompt-tokens/completion-tokens` 计数器 + `buzhou.budget.hard-stops`（core 单口径家族）。
