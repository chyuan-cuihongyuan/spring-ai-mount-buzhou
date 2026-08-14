# Spec 16 — Token/成本预算与配额（mechanism）

> effort #5（T83 / impl-58 起落地）。借鉴：OpenAI Agents SDK `/cost`、LangSmith cost-per-trace、
> Claude Code 会话成本显示——语义借鉴，不引依赖。

## Token/成本计量（core/budget，impl-58）

- **TokenBudgetHook**（挂 BuzhouHook 链，order 1100，runaway(1000) 之后——runaway 管「行为失控步数」，
  budget 管「资源消耗预算」，同范式分立演进）。
- **计量来源**：`afterModel` 从 `ctx.response().chatResponse().metadata().usage()` 提取
  prompt/completion tokens（与 ObservabilityAdvisor 同源同口径）；null/零 usage 跳过（替身模型不误记）。
- **会话累计**：SessionStateStore（`ctx.state()`，跨崩溃持久化，与 runaway 会话累计同先例）三键：
  `buzhou.budget.prompt-tokens` / `completion-tokens` / `cost-micro-usd`（long 字符串值）；
  per-session 锁防并行 RMW 竞争。
- **成本价目**：`buzhou.token-budget.pricing.<model>.{input-per-million, output-per-million}`
  （BigDecimal USD/百万 token）。模型名 = `chatOptions.model` 回退 `buzhou.model-name`。
  **无价目 = 零成本**（只计 token）。累计单位 micro-USD long：`token × 每百万价` 恰为 microUsd/token，
  整数运算无浮点漂移。
- **事件**：`budget.tokens-accumulated`（每次有 usage 的模型调用：本次 + 会话累计快照 + 可选成本），
  双写 SessionEvent + EventRecord（dashboard 可查）；指标 `buzhou.budget.prompt-tokens` /
  `completion-tokens` 计数器（core MeterBinder 单口径家族）。

## 预算闸（safe-by-default：null = 不限，无阈值空转零开销）

- **硬顶**（`beforeModel` 检查，超限拦截**下一次**模型调用——已消耗预算不可逆）：
  `max-session-prompt-tokens` / `max-session-total-tokens` / `max-session-cost-usd`。
- 超限：`budget.token-hard-stop` / `budget.cost-hard-stop` 事件（reason/limit/value/partialResultRef，
  与 runaway 硬顶同口径）+ `HookResult.block(reason)` 终止本轮（reason 为受控终态文本，
  本轮已完成工具调用结果随轮次保留）。
- **fail-fast**：设 `max-session-cost-usd` 而未配任何价目 → 启动失败（成本闸无从计算）；
  各上限必须为正。
- **配置**：`buzhou.token-budget.*`（`BuzhouTokenBudgetProperties`；`enabled` 默认 true）；
  装配对齐 runawayHook bean 模式（@ConditionalOnMissingBean(name) + observabilityStore 双写注入）。

## per-session 配额（impl-59，T84）

- **限流器进程级修正**：`ModelRateLimiter` 自 `ResilienceModule.configure()` 创建（进程级、全部会话共享）——
  RPM/TPM 是进程级容量，此前 customize() 内建导致 N 会话 = N 倍限额。
- **SessionQuotaHook**（resilience 模块贡献的 BuzhouHook，order 1150）：三维度 / UTC 自然日固定窗口——
  `buzhou.resilience.session-quota.{turns-per-day, tool-calls-per-day, tokens-per-day}`（null = 不限）。
- **计数存放**：SessionStateStore 单键单维度 `"<epochDay>:<count>"`（键 `buzhou.quota.turns/tool-calls/
  tokens`），读时日不符即重置——免过期键清理、跨崩溃持久化。tokens 在 afterModel 自行按日键累计
  （与 TokenBudgetHook 同源提取 usage，键不交叉：生命周期累计 ≠ 日窗配额）。
- **拦截点**：beforeTurn（turns）/ beforeTool（tool-calls，reason 回注为工具结果文本）/ beforeModel
  （tokens，读当日已累计）。超配额 `HookResult.block`（受控终态文本）+ 事件 `quota.exceeded`
  （dimension/limit/value/day）。
- **观测**：`ResilienceStats.quotaRejections` + 指标 `buzhou.resilience.quota-rejected`（dimension tag）。
- **诚实边界**：单进程语义——多实例部署 = 每实例独立配额（分布式配额 out-of-scope，见 spec 23）。
