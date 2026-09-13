# 778 — 内嵌策略引擎判定分布读面

**What to build:** EmbeddedPolicyEngine 四桶判定计数 + 嵌套 PolicyDecisionStats + stats() + 判定分布测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 四桶计数（allow/deny/escalate/escalateApproved）
- [x] PolicyDecisionStats 嵌套 record + stats()
- [x] EmbeddedPolicyEngineStatsTest（四桶/守恒/谓词跳过/默认拒）
- [x] spec 1025 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-guard test -Dtest='EmbeddedPolicyEngineStatsTest'` 全绿 + 既有策略引擎回归绿。commit 见本轮 `feat(guard)` 提交。
