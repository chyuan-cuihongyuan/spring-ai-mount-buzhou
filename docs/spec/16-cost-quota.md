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

- （待 T84 决议后补）
