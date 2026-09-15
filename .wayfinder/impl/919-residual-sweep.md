# 919 — Advisor 残余分支清扫（R17）

**What to build:** ObservabilityAdvisorStreamTest + ObservabilityAdvisorCallTest 各追加残余分支用例。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 流式残余 4 用例（metadata null·result null 防御/blank finishReason 不记/空串思维链无 THINKING 事件）
- [x] 非流式残余 2 用例（metadata=null 归一口径/assistant=null 仅 usage·close）
- [x] spec 1216 + README 行
- [x] 验证：141 用例全绿；ObservabilityAdvisor 77%（累计口径）

## Done

验证：observability 141 用例全绿；残余分支收敛。commit 见本轮 `test(observability)` 提交。
