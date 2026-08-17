# 58 — Token/成本计量与预算闸（T83 决策落地）

**What to build:** core 新 `budget` 包：`TokenBudgetHook`（afterModel 累计 usage 进 SessionStateStore + beforeModel 预算闸）+ `BuzhouTokenBudgetProperties`（pricing 价目表 + 三硬顶 + fail-fast）+ AutoConfiguration 装配 + 事件/指标。

**Blocked by:** None.

**Status:** done

- [ ] `BuzhouTokenBudgetProperties`：enabled/max-session-prompt-tokens/max-session-total-tokens/max-session-cost-usd/pricing map；cost 上限无价目 fail-fast、负值 fail-fast
- [ ] `TokenBudgetHook`：afterModel 提取 usage（null/零跳过）→ per-session 锁累计三键 → `budget.tokens-accumulated` 事件双写 + 指标计数；beforeModel 三硬顶检查 → hard-stop 事件 + HookResult.block
- [ ] 模型名解析：chatOptions.model 回退构造传入的 buzhou.model-name；价目未含该模型 = 零成本
- [ ] `BuzhouCoreAutoConfiguration` 注册 tokenBudgetHook bean（镜像 runawayHook：@ConditionalOnMissingBean(name) + stores ObjectProvider 惰性取 observabilityStore）
- [ ] 测试：usage 携带模型替身 e2e（累计正确、事件 payload、跨 turn 持续累计）；total-tokens 硬顶拦截下一调用并 block；cost 硬顶（价目换算 microUsd 口径）；无 usage 替身零记账；fail-fast 两例

## Done

验证：`./mvnw -pl buzhou-core clean test` 253/253 绿（新增 TokenBudgetHookEndToEndTest 5 用例：跨轮累计/total 硬顶拦截/成本价目 microUsd 换算/无 usage 零记账/fail-fast×2）；starter 5/5、examples 62/62 绿。
落地：`core/budget/TokenBudgetHook`（order 1100，afterModel 累计 + beforeModel 三硬顶 + block + partialResultRef 事件双写）+ `BuzhouTokenBudgetProperties`（pricing 价目 + fail-fast：cost 上限无价目/负值）+ `BuzhouCoreAutoConfiguration` tokenBudgetHook bean（镜像 runawayHook，buzhou.model-name 回退键）；SessionStateStore 三键 long 字符串累计（per-session 锁）；microUsd 整数口径（token × 每百万价）；指标 buzhou.budget.prompt-tokens/completion-tokens/hard-stops；事件 budget.tokens-accumulated / token-hard-stop / cost-hard-stop。
