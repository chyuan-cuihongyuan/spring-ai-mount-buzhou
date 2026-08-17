# 04 — core · 工具参数 schema 校验 + per-turn 重试预算

**What to build:** 工具参数不过 schema 时不执行工具，把校验错误回喂模型自愈重试，重试受每 Turn 独立预算硬上限约束、耗尽优雅收尾。

**Blocked by:** None — can start immediately.

**Status:** done（2026-08-14：ToolArgsValidator 自实现最小 schema 子集 + ToolValidationFeedback 两档词汇 + manager 执行前拦截计数 + TurnLoopPolicy.retryBudget + advisor REASK_FAILED 收尾/事件；ToolArgsValidatorTest 6 例 + ArgsValidationRetryEndToEndTest 2 例；spec 05 新节）

- [ ] 工具执行前对 arguments 做 JSON Schema 校验（复用 spring-ai 工具 schema 生成）
- [ ] 校验失败合成 `ToolValidationFeedback`（与执行期 `ToolErrorFeedback` 两档词汇分明，格式对齐错误回喂通道）
- [ ] `TurnLoopPolicy` 增 `retryBudget`（默认 1–2，与 Turn 上界独立扣减），耗尽转 REASK_FAILED 停止条件优雅收尾
- [ ] 校验重试与执行失败分别可观测（事件区分）
- [ ] 端到端：模型误用参数→收到校验反馈→自愈成功；预算耗尽→优雅终止
- [ ] spec 05（并行工具）同步

> spec 12 §core-2；[T30](../tickets/T30-core-tool-args-validation-retry.md)。源：pydantic-ai 19,271★（ModelRetry vs ToolFailed、默认 retries=1）+ instructor 13,726★。
