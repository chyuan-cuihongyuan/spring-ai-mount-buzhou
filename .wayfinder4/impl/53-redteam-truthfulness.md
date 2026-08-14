# 53 — redteam 门真实化

**What to build:** 红队评分对象=真实 agent 回复与真实护栏决策（x-buzhou-guard-blocked 头 + transformResponse 派生 guardrails.flagged + type:guardrails 断言）；基线落档；nightly 跑审计链重放；升硬门标准成文。

**Blocked by:** 48-dashboard-security

**Status:** ready-for-agent

- [ ] RedteamTarget：响应体=真实回复；拦截时 x-buzhou-guard-blocked: true；请求解析 JSON 化（失败 400）
- [ ] promptfooconfig：transformResponse + 注入类用例 guardrails 断言
- [ ] redteam/baseline.md 首版基线（快照+口径注记）
- [ ] nightly audit-chain 重放 job；README 类名修正 + 升硬门标准
