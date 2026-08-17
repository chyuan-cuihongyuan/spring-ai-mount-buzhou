# 53 — redteam 门真实化

**What to build:** 红队评分对象=真实 agent 回复与真实护栏决策（x-buzhou-guard-blocked 头 + transformResponse 派生 guardrails.flagged + type:guardrails 断言）；基线落档；nightly 跑审计链重放；升硬门标准成文。

**Blocked by:** 48-dashboard-security

**Status:** done

- [ ] RedteamTarget：响应体=真实回复；拦截时 x-buzhou-guard-blocked: true；请求解析 JSON 化（失败 400）
- [ ] promptfooconfig：transformResponse + 注入类用例 guardrails 断言
- [ ] redteam/baseline.md 首版基线（快照+口径注记）
- [ ] nightly audit-chain 重放 job；README 类名修正 + 升硬门标准


## Done

commit: 见 git log（impl/53）。验证：RedteamTargetSmokeTest 1/1 绿（新增拦截信号头断言）。
落地：target 响应体=真实 chat() 产出（JSON 转义）+ x-buzhou-guard-blocked/x-buzhou-dangerous-executed 响应头（hook.blocked 事件派生）+ promptfooconfig transformResponse 派生 {output, guardrails:{flagged}} + defaultTest type:guardrails 断言（拦截=pass）+ redteam/baseline.md 首版基线（确定性基线表 + F1 口径注记 + 升硬门标准）+ nightly 新增 audit-chain-replay（ResilienceMatrixEndToEndTest）与 guard-signal-smoke（硬门）两个 job + README 类名修正与升硬门文档。
