---
Type: grilling
Status: closed
blocked-by: T69
---

## Question

redteam 门的真实化：RedteamTargetSmokeTest 目标端点把真实 agent 回复与 guard 拦截决策透传进 HTTP 响应（替换硬编码文案）、护栏拦截的结构化信号（blocked/dangerousExecuted）如何暴露给 promptfoo grader、redteam/baseline.md 落档、nightly 补审计链重放校验 job（兑现 README 承诺）、门禁从 || true 升硬的量化标准、请求解析健壮化、README 类名修正。

## Resolution

进本轮（采纳 T69 §6 promptfoo guardrails 契约）：
1. **目标端点真实化**：/v1/chat/completions 响应体写真实 agent 回复（会话 chat() 产出或 guard 拦截文案）；guard 拦截/HITL 阻断时响应头 `x-buzhou-guard-blocked: true`，正常 false。
2. promptfooconfig：provider 加 `transformResponse` 从响应头派生 `guardrails.flagged`；shell-injection/excessive-agency/prompt-injection 用例加 `type: guardrails` 断言（拦截=pass，绕过=fail）。
3. `redteam/baseline.md` 落档首版基线（本地跑一次 promptfoo 的结果快照 + 指标解释 + F1 口径注记）。
4. nightly：保留 || true（观测期），新增 audit-chain 重放 job（跑 examples 侧 AuditChain 集成测试即兑现承诺）；升硬门标准写进 README：连续 2 次 nightly 全绿 → 把 promptfoo run 的 || true 移除（exit code 门禁）。
5. 请求解析健壮化（JSON 解析 messages 数组，取最后 user content；解析失败 400）。
6. README 类名修正（RedteamTargetSmokeTest）。（可推翻）
